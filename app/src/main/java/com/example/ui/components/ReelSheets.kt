package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil.compose.AsyncImage
import com.example.data.SampleData
import com.example.model.Reel
import com.example.model.User
import com.example.ui.theme.IgBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelCommentsBottomSheet(
    reel: Reel,
    currentUser: User,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("reel_comments_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Comments",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                thickness = 0.5.dp
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (reel.comments.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No comments yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Be the first to comment on @${reel.user.username}'s reel.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(reel.comments, key = { it.id }) { comment ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            AsyncImage(
                                model = comment.userAvatar,
                                contentDescription = "${comment.username} avatar",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = comment.username,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = comment.timestamp,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("❤️", "🔥", "🙌", "👏", "😂", "😍", "✨").forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .clickable { commentInput += emoji }
                            .padding(4.dp)
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = currentUser.avatarUrl,
                    contentDescription = "Your avatar",
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(10.dp))

                TextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    placeholder = {
                        Text(
                            text = "Add a comment...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("reel_comment_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                if (commentInput.isNotBlank()) {
                    TextButton(
                        onClick = {
                            onAddComment(commentInput)
                            commentInput = ""
                        },
                        modifier = Modifier.testTag("reel_comment_post_button")
                    ) {
                        Text(
                            text = "Post",
                            color = IgBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelShareBottomSheet(
    reel: Reel,
    onDismiss: () -> Unit,
    onShareToUser: (User) -> Unit,
    onCopyLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val sentUserIds = remember { mutableStateListOf<String>() }
    val friends = remember { SampleData.sampleUsers }
    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) friends
        else friends.filter {
            it.username.contains(searchQuery, ignoreCase = true) ||
                it.fullName.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("reel_share_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Share Reel",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search friends...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { friend ->
                    val isSent = sentUserIds.contains(friend.id)
                    val buttonBg by animateColorAsState(
                        targetValue = if (isSent) MaterialTheme.colorScheme.surfaceVariant else IgBlue,
                        label = "shareBtnBg"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!isSent) {
                                    sentUserIds.add(friend.id)
                                    onShareToUser(friend)
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(
                                model = friend.avatarUrl,
                                contentDescription = friend.username,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            if (isSent) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(IgBlue)
                                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Sent",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = friend.username,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(buttonBg)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isSent) "Sent" else "Send",
                                color = if (isSent) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)

            TextButton(
                onClick = {
                    onCopyLink()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = IgBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Reel Link", color = IgBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelOptionsBottomSheet(
    reel: Reel,
    isFollowingCreator: Boolean,
    isMuted: Boolean,
    onDismiss: () -> Unit,
    onToggleSave: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("reel_options_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ListItem(
                headlineContent = {
                    Text(if (reel.isSaved) "Remove from Saved" else "Save Reel")
                },
                leadingContent = {
                    Icon(
                        imageVector = if (reel.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable {
                    onToggleSave()
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = {
                    Text(if (isMuted) "Unmute Audio" else "Mute Audio")
                },
                leadingContent = {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable {
                    onToggleMute()
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = {
                    Text(if (isFollowingCreator) "Unfollow @${reel.user.username}" else "Follow @${reel.user.username}")
                },
                leadingContent = {
                    Icon(
                        imageVector = if (isFollowingCreator) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable {
                    onToggleFollow()
                    onDismiss()
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
