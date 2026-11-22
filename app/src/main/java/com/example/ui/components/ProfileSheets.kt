package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.SampleData
import com.example.model.User
import com.example.ui.theme.IgBlue
import com.example.ui.theme.IgPink
import com.example.ui.theme.InstagramGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsBottomSheet(
    user: User,
    isDarkMode: Boolean,
    unreadNotificationsCount: Int,
    onToggleDarkMode: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenSavedTab: () -> Unit,
    onOpenUpdatesSettings: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("profile_settings_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Settings & Activity",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            )

            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)

            ListItem(
                headlineContent = { Text("Edit profile") },
                supportingContent = { Text("Update name, bio, and website link") },
                leadingContent = {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                },
                modifier = Modifier
                    .clickable {
                        onDismiss()
                        onEditProfile()
                    }
                    .testTag("settings_edit_profile")
            )

            ListItem(
                headlineContent = { Text("Dark mode") },
                supportingContent = { Text(if (isDarkMode) "Dark theme enabled" else "Light theme enabled") },
                leadingContent = {
                    Icon(imageVector = Icons.Default.DarkMode, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onToggleDarkMode() },
                        modifier = Modifier.testTag("settings_dark_mode_switch")
                    )
                },
                modifier = Modifier.clickable { onToggleDarkMode() }
            )

            ListItem(
                headlineContent = { Text("Activity notifications") },
                supportingContent = { Text("Likes, comments, and new followers") },
                leadingContent = {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = null)
                },
                trailingContent = {
                    if (unreadNotificationsCount > 0) {
                        Badge(containerColor = IgPink, contentColor = Color.White) {
                            Text("$unreadNotificationsCount")
                        }
                    }
                },
                modifier = Modifier
                    .clickable {
                        onDismiss()
                        onOpenActivity()
                    }
                    .testTag("settings_open_activity")
            )

            ListItem(
                headlineContent = { Text("Saved posts") },
                supportingContent = { Text("View your bookmarked collection") },
                leadingContent = {
                    Icon(imageVector = Icons.Default.Bookmark, contentDescription = null)
                },
                modifier = Modifier
                    .clickable {
                        onDismiss()
                        onOpenSavedTab()
                    }
                    .testTag("settings_open_saved")
            )

            ListItem(
                headlineContent = { Text("App updates & GitHub releases") },
                supportingContent = { Text("Check for newer APK releases") },
                leadingContent = {
                    Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null)
                },
                modifier = Modifier
                    .clickable {
                        onDismiss()
                        onOpenUpdatesSettings()
                    }
                    .testTag("settings_app_updates")
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)

            ListItem(
                headlineContent = {
                    Text(
                        text = "Sign out",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier
                    .clickable {
                        onDismiss()
                        onSignOut()
                    }
                    .testTag("settings_sign_out")
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileConnectionsSheet(
    title: String,
    followedUsers: Map<String, Boolean>,
    onToggleFollow: (String) -> Unit,
    onSelectUser: (User) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val users = SampleData.sampleUsers
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("profile_connections_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(users, key = { it.id }) { creator ->
                    val isFollowing = followedUsers[creator.id] ?: creator.isFollowing
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onDismiss()
                                onSelectUser(creator)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = creator.avatarUrl,
                            contentDescription = creator.username,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = creator.username,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (creator.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = IgBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = creator.fullName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onToggleFollow(creator.id) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else IgBlue,
                                contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = if (isFollowing) "Following" else "Follow",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileAvatarPreviewDialog(
    user: User,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(40.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close avatar preview")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .border(3.dp, InstagramGradient, CircleShape)
                        .padding(5.dp)
                ) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = user.fullName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    fontSize = 15.sp
                )
            }
        }
    }
}
