package com.example.update

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GitHubReleaseInfo(
    val tagName: String,
    val title: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val release: GitHubReleaseInfo) : UpdateCheckResult
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data object NotConfigured : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

interface GitHubUpdateClient {
    suspend fun fetchLatestRelease(repoSlug: String): Result<GitHubReleaseInfo>
}

class HttpGitHubUpdateClient : GitHubUpdateClient {
    override suspend fun fetchLatestRelease(repoSlug: String): Result<GitHubReleaseInfo> =
        withContext(Dispatchers.IO) {
            val cleanSlug = AppUpdateChecker.sanitizeRepoSlug(repoSlug)
            if (cleanSlug == null) {
                return@withContext Result.failure(
                    IllegalArgumentException("Enter repository as owner/repo (for example, octocat/instasnap).")
                )
            }

            var connection: HttpURLConnection? = null
            try {
                val url = URL("https://api.github.com/repos/$cleanSlug/releases/latest")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "InstaSnap-Android-Updater")
                }

                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                    return@withContext Result.failure(
                        IllegalStateException("No published GitHub Releases found for $cleanSlug.")
                    )
                }
                if (code !in 200..299) {
                    return@withContext Result.failure(
                        IllegalStateException("GitHub API returned HTTP $code.")
                    )
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "").trim()
                val htmlUrl = json.optString("html_url", "").trim()
                if (tagName.isEmpty() || htmlUrl.isEmpty()) {
                    return@withContext Result.failure(
                        IllegalStateException("Invalid release metadata from GitHub.")
                    )
                }

                val title = json.optString("name", tagName).ifBlank { tagName }
                val releaseNotes = json.optString("body", "").trim()
                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val assetName = asset.optString("name", "")
                        val downloadUrl = asset.optString("browser_download_url", "")
                        if (assetName.endsWith(".apk", ignoreCase = true) && downloadUrl.startsWith("https://")) {
                            apkUrl = downloadUrl
                            break
                        }
                    }
                }

                Result.success(
                    GitHubReleaseInfo(
                        tagName = tagName,
                        title = title,
                        releaseNotes = releaseNotes,
                        htmlUrl = htmlUrl,
                        apkDownloadUrl = apkUrl
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                connection?.disconnect()
            }
        }
}

object AppUpdateChecker {
    private val repoSlugRegex = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")

    fun sanitizeRepoSlug(raw: String): String? {
        val trimmed = raw.trim()
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removeSuffix(".git")
            .trim('/')
        return if (repoSlugRegex.matches(trimmed)) trimmed else null
    }

    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val remoteParts = parseVersionParts(remoteTag)
        val currentParts = parseVersionParts(currentVersion)
        if (remoteParts.isEmpty()) return false
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val remoteVal = remoteParts.getOrElse(i) { 0 }
            val currentVal = currentParts.getOrElse(i) { 0 }
            if (remoteVal > currentVal) return true
            if (remoteVal < currentVal) return false
        }
        return false
    }

    private fun parseVersionParts(version: String): List<Int> {
        val cleaned = version.trim().removePrefix("v").removePrefix("V")
        val numericPrefix = cleaned.substringBefore("-")
        return numericPrefix.split(".")
            .mapNotNull { segment -> segment.filter { it.isDigit() }.toIntOrNull() }
    }

    suspend fun checkForUpdates(
        repoSlug: String,
        currentVersion: String,
        client: GitHubUpdateClient = HttpGitHubUpdateClient()
    ): UpdateCheckResult {
        val cleanSlug = sanitizeRepoSlug(repoSlug) ?: return UpdateCheckResult.NotConfigured
        return client.fetchLatestRelease(cleanSlug).fold(
            onSuccess = { release ->
                if (isNewerVersion(release.tagName, currentVersion)) {
                    UpdateCheckResult.UpdateAvailable(release)
                } else {
                    UpdateCheckResult.UpToDate(currentVersion)
                }
            },
            onFailure = { error ->
                UpdateCheckResult.Error(error.localizedMessage ?: "Failed to check GitHub releases.")
            }
        )
    }
}

class AppUpdatePreferences(context: Context, defaultRepoFromResources: String) {
    private val prefs = context.applicationContext.getSharedPreferences("instasnap_updates", Context.MODE_PRIVATE)
    private val defaultRepo = defaultRepoFromResources.trim()

    fun getConfiguredRepo(): String {
        val saved = prefs.getString(KEY_GITHUB_REPO, null)
        return saved?.takeIf { it.isNotBlank() } ?: defaultRepo
    }

    fun setConfiguredRepo(repoSlug: String) {
        prefs.edit().putString(KEY_GITHUB_REPO, repoSlug.trim()).apply()
    }

    private companion object {
        const val KEY_GITHUB_REPO = "github_repo"
    }
}
