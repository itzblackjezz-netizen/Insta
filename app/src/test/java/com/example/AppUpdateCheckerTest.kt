package com.example

import com.example.update.AppUpdateChecker
import com.example.update.GitHubReleaseInfo
import com.example.update.GitHubUpdateClient
import com.example.update.UpdateCheckResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun testSanitizeRepoSlug() {
        assertEquals("octocat/instasnap", AppUpdateChecker.sanitizeRepoSlug("octocat/instasnap"))
        assertEquals("octocat/instasnap", AppUpdateChecker.sanitizeRepoSlug("https://github.com/octocat/instasnap.git"))
        assertNull(AppUpdateChecker.sanitizeRepoSlug(""))
        assertNull(AppUpdateChecker.sanitizeRepoSlug("invalid-slug"))
    }

    @Test
    fun testIsNewerVersion() {
        assertTrue(AppUpdateChecker.isNewerVersion("v1.0.1", "1.0.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("v1.2.0", "1.1.9"))
        assertTrue(AppUpdateChecker.isNewerVersion("v2.0.0", "1.9.9"))
        assertFalse(AppUpdateChecker.isNewerVersion("v1.0.0", "1.0.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("v0.9.5", "1.0.0"))
    }

    @Test
    fun testCheckForUpdatesReturnsUpdateAvailableWhenNewerReleaseExists() = runTest {
        val fakeClient = object : GitHubUpdateClient {
            override suspend fun fetchLatestRelease(repoSlug: String): Result<GitHubReleaseInfo> {
                return Result.success(
                    GitHubReleaseInfo(
                        tagName = "v1.1.0",
                        title = "InstaSnap v1.1.0",
                        releaseNotes = "Interactive Stories & Performance Improvements",
                        htmlUrl = "https://github.com/$repoSlug/releases/tag/v1.1.0",
                        apkDownloadUrl = "https://github.com/$repoSlug/releases/download/v1.1.0/InstaSnap-v1.1.0.apk"
                    )
                )
            }
        }

        val result = AppUpdateChecker.checkForUpdates(
            repoSlug = "octocat/instasnap",
            currentVersion = "1.0.0",
            client = fakeClient
        )

        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        val available = result as UpdateCheckResult.UpdateAvailable
        assertEquals("v1.1.0", available.release.tagName)
        assertTrue(available.release.apkDownloadUrl!!.endsWith(".apk"))
    }

    @Test
    fun testCheckForUpdatesReturnsUpToDateWhenVersionsMatch() = runTest {
        val fakeClient = object : GitHubUpdateClient {
            override suspend fun fetchLatestRelease(repoSlug: String): Result<GitHubReleaseInfo> {
                return Result.success(
                    GitHubReleaseInfo(
                        tagName = "v1.0.0",
                        title = "InstaSnap v1.0.0",
                        releaseNotes = "",
                        htmlUrl = "https://github.com/$repoSlug/releases/tag/v1.0.0",
                        apkDownloadUrl = null
                    )
                )
            }
        }

        val result = AppUpdateChecker.checkForUpdates(
            repoSlug = "octocat/instasnap",
            currentVersion = "1.0.0",
            client = fakeClient
        )

        assertTrue(result is UpdateCheckResult.UpToDate)
    }
}
