package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IgBlue
import com.example.update.AppUpdateChecker
import com.example.update.GitHubReleaseInfo
import com.example.update.UpdateCheckResult

@Composable
fun UpdateAvailableDialog(
    currentVersion: String,
    release: GitHubReleaseInfo,
    onUpdateNow: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.NewReleases,
                contentDescription = null,
                tint = IgBlue,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Update Available (${release.tagName})",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "A newer build of InstaSnap is available on GitHub Releases (current: v$currentVersion).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (release.releaseNotes.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = release.releaseNotes.take(600),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                Text(
                    text = "Tapping Update Now opens the release APK in your browser so you can install the latest version.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdateNow,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IgBlue,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("update_now_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Update Now", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("update_later_button")
            ) {
                Text("Later")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdatesSettingsSheet(
    currentVersion: String,
    configuredRepo: String,
    isChecking: Boolean,
    lastCheckResult: UpdateCheckResult?,
    onSaveAndCheck: (String) -> Unit,
    onOpenRelease: (GitHubReleaseInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var repoInput by remember(configuredRepo) { mutableStateOf(configuredRepo) }
    var inputError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("app_updates_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "GitHub APK Auto-Updater",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "v$currentVersion",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "When configured, InstaSnap checks GitHub Releases automatically each time you open the app and prompts you to update.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = repoInput,
                onValueChange = {
                    repoInput = it
                    inputError = null
                },
                label = { Text("GitHub Repository (owner/repo)") },
                placeholder = { Text("e.g. username/instasnap") },
                singleLine = true,
                isError = inputError != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("github_repo_input"),
                shape = RoundedCornerShape(10.dp)
            )

            inputError?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            when (lastCheckResult) {
                is UpdateCheckResult.UpToDate -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "You're up to date (v${lastCheckResult.currentVersion}).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                is UpdateCheckResult.UpdateAvailable -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = IgBlue.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "New version ${lastCheckResult.release.tagName} ready",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = IgBlue
                                )
                                Text(
                                    text = lastCheckResult.release.title,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(onClick = { onOpenRelease(lastCheckResult.release) }) {
                                Text("Update", fontWeight = FontWeight.Bold, color = IgBlue)
                            }
                        }
                    }
                }
                is UpdateCheckResult.Error -> {
                    Text(
                        text = lastCheckResult.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> Unit
            }

            Button(
                onClick = {
                    val sanitized = AppUpdateChecker.sanitizeRepoSlug(repoInput)
                    if (sanitized == null) {
                        inputError = "Enter a valid GitHub repo like owner/repo."
                    } else {
                        onSaveAndCheck(sanitized)
                    }
                },
                enabled = !isChecking,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IgBlue,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("check_updates_button")
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save & Check for Updates", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
