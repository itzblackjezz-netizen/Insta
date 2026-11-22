package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.NotificationItem
import com.example.model.NotificationType
import com.example.model.User
import com.example.ui.theme.IgBlue
import com.example.ui.theme.IgOrange
import com.example.ui.theme.IgPink
import com.example.ui.theme.IgPurple
import com.example.ui.theme.IgRed
import com.example.ui.theme.InstagramGradient

private enum class ActivityFilter(val label: String) {
    ALL("All"),
    LIKES("Likes"),
    COMMENTS("Comments"),
    FOLLOWERS("Followers"),
    MENTIONS("Mentions"),
    STORIES("Stories"),
    REQUESTS("Requests")
}

@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
    followedUsers: Map<String, Boolean>,
    onToggleFollowUser: (String) -> Unit,
    onConfirmFollowRequest: (String) -> Unit,
    onDeleteFollowRequest: (String) -> Unit,
    onPostNotificationClick: (NotificationItem) -> Unit,
    onUserClick: (User) -> Unit,
    onSimulateActivity: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var selectedFilter by remember { mutableStateOf(ActivityFilter.ALL) }
    var expandFollowRequests by remember { mutableStateOf(true) }

    val followRequests = remember(notifications) {
        notifications.filter { it.type == NotificationType.FOLLOW_REQUEST }
    }

    val filteredNotifications = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            ActivityFilter.ALL -> notifications.filterNot { it.type == NotificationType.FOLLOW_REQUEST }
            ActivityFilter.LIKES -> notifications.filter { it.type == NotificationType.LIKE }
            ActivityFilter.COMMENTS -> notifications.filter { it.type == NotificationType.COMMENT }
            ActivityFilter.FOLLOWERS -> notifications.filter { it.type == NotificationType.FOLLOW }
            ActivityFilter.MENTIONS -> notifications.filter { it.type == NotificationType.MENTION }
            ActivityFilter.STORIES -> notifications.filter { it.type == NotificationType.STORY_INTERACTION }
            ActivityFilter.REQUESTS -> notifications.filter { it.type == NotificationType.FOLLOW_REQUEST }
        }
    }

    val todayItems = remember(filteredNotifications) {
        filteredNotifications.filter {
            it.timestamp == "Just now" || it.timestamp.endsWith("m") || it.timestamp.endsWith("h")
        }
    }
    val thisWeekItems = remember(filteredNotifications) {
        filteredNotifications.filter {
            it.timestamp == "Yesterday" || it.timestamp.endsWith("d")
        }
    }
    val earlierItems = remember(filteredNotifications) {
        filteredNotifications.filterNot { item ->
            item.timestamp == "Just now" ||
                item.timestamp.endsWith("m") ||
                item.timestamp.endsWith("h") ||
                item.timestamp == "Yesterday" ||
                item.timestamp.endsWith("d")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("notifications_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("activity_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${notifications.size} updates · ${followRequests.size} requests",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = onSimulateActivity,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("activity_check_updates_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = IgBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Refresh",
                        color = IgBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ActivityFilter.entries) { filter ->
                    val isSelected = selectedFilter == filter
                    val countBadge = if (filter == ActivityFilter.REQUESTS && followRequests.isNotEmpty()) {
                        " (${followRequests.size})"
                    } else ""
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedFilter = filter }
                            .testTag("activity_filter_${filter.name.lowercase()}")
                    ) {
                        Text(
                            text = "${filter.label}$countBadge",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.background
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                thickness = 0.5.dp
            )

            val showPinnedRequests = selectedFilter == ActivityFilter.ALL && followRequests.isNotEmpty()

            if (filteredNotifications.isEmpty() && !showPinnedRequests) {
                EmptyNotificationsState(
                    filterLabel = selectedFilter.label,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("activity_feed_list"),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Pinned Follow Requests Header + Inline Request Cards in "All" View
                    if (showPinnedRequests) {
                        item(key = "follow_requests_header") {
                            FollowRequestsBanner(
                                requests = followRequests,
                                isExpanded = expandFollowRequests,
                                onToggleExpand = { expandFollowRequests = !expandFollowRequests }
                            )
                        }
                        if (expandFollowRequests) {
                            items(followRequests, key = { "pinned_${it.id}" }) { req ->
                                ActivityNotificationRow(
                                    item = req,
                                    isFollowing = followedUsers[req.user.id] ?: req.isFollowingBack,
                                    onToggleFollow = { onToggleFollowUser(req.user.id) },
                                    onConfirmRequest = { onConfirmFollowRequest(req.id) },
                                    onDeleteRequest = { onDeleteFollowRequest(req.id) },
                                    onUserClick = { onUserClick(req.user) },
                                    onClick = { onUserClick(req.user) }
                                )
                            }
                            item(key = "follow_requests_divider") {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (todayItems.isNotEmpty()) {
                        item(key = "header_today") {
                            SectionTimestampHeader(title = "Today")
                        }
                        items(todayItems, key = { it.id }) { item ->
                            ActivityNotificationRow(
                                item = item,
                                isFollowing = followedUsers[item.user.id] ?: item.isFollowingBack,
                                onToggleFollow = { onToggleFollowUser(item.user.id) },
                                onConfirmRequest = { onConfirmFollowRequest(item.id) },
                                onDeleteRequest = { onDeleteFollowRequest(item.id) },
                                onUserClick = { onUserClick(item.user) },
                                onClick = { onPostNotificationClick(item) }
                            )
                        }
                    }

                    if (thisWeekItems.isNotEmpty()) {
                        item(key = "header_week") {
                            SectionTimestampHeader(
                                title = "This week",
                                modifier = Modifier.padding(top = if (todayItems.isNotEmpty()) 6.dp else 0.dp)
                            )
                        }
                        items(thisWeekItems, key = { it.id }) { item ->
                            ActivityNotificationRow(
                                item = item,
                                isFollowing = followedUsers[item.user.id] ?: item.isFollowingBack,
                                onToggleFollow = { onToggleFollowUser(item.user.id) },
                                onConfirmRequest = { onConfirmFollowRequest(item.id) },
                                onDeleteRequest = { onDeleteFollowRequest(item.id) },
                                onUserClick = { onUserClick(item.user) },
                                onClick = { onPostNotificationClick(item) }
                            )
                        }
                    }

                    if (earlierItems.isNotEmpty()) {
                        item(key = "header_earlier") {
                            SectionTimestampHeader(
                                title = "Earlier",
                                modifier = Modifier.padding(
                                    top = if (todayItems.isNotEmpty() || thisWeekItems.isNotEmpty()) 6.dp else 0.dp
                                )
                            )
                        }
                        items(earlierItems, key = { it.id }) { item ->
                            ActivityNotificationRow(
                                item = item,
                                isFollowing = followedUsers[item.user.id] ?: item.isFollowingBack,
                                onToggleFollow = { onToggleFollowUser(item.user.id) },
                                onConfirmRequest = { onConfirmFollowRequest(item.id) },
                                onDeleteRequest = { onDeleteFollowRequest(item.id) },
                                onUserClick = { onUserClick(item.user) },
                                onClick = { onPostNotificationClick(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTimestampHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.5.sp
        ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun FollowRequestsBanner(
    requests: List<NotificationItem>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val latest = requests.firstOrNull() ?: return
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggleExpand)
            .testTag("follow_requests_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                AsyncImage(
                    model = latest.user.avatarUrl,
                    contentDescription = latest.user.username,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                        .clip(CircleShape)
                        .background(IgBlue)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${requests.size}",
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Follow requests",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (requests.size == 1) {
                        "${latest.user.username} wants to follow you"
                    } else {
                        "${latest.user.username} + ${requests.size - 1} others"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (isExpanded) "Collapse follow requests" else "Expand follow requests",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActivityNotificationRow(
    item: NotificationItem,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
    onConfirmRequest: () -> Unit,
    onDeleteRequest: () -> Unit,
    onUserClick: () -> Unit,
    onClick: () -> Unit
) {
    val (badgeColor, badgeIcon, typeTag) = when (item.type) {
        NotificationType.LIKE -> Triple(IgRed, Icons.Default.Favorite, "Like")
        NotificationType.COMMENT -> Triple(IgPurple, Icons.Default.ChatBubble, "Comment")
        NotificationType.FOLLOW -> Triple(IgBlue, Icons.Default.PersonAdd, "Follower")
        NotificationType.MENTION -> Triple(IgPink, Icons.Default.AlternateEmail, "Mention")
        NotificationType.STORY_INTERACTION -> Triple(IgOrange, Icons.Default.AutoAwesome, "Story")
        NotificationType.FOLLOW_REQUEST -> Triple(IgBlue, Icons.Default.PersonAddAlt1, "Request")
    }

    val isStory = item.type == NotificationType.STORY_INTERACTION
    val avatarRingModifier = if (isStory) {
        Modifier
            .border(2.dp, InstagramGradient, CircleShape)
            .padding(3.dp)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag("activity_item_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onUserClick)
        ) {
            AsyncImage(
                model = item.user.avatarUrl,
                contentDescription = item.user.username,
                modifier = Modifier
                    .size(48.dp)
                    .then(avatarRingModifier)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .size(19.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = typeTag,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(item.user.username)
                            append(" ")
                        }
                        append(item.text)
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formatReadableActivityTimestamp(item.timestamp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("activity_timestamp_${item.id}")
                )
                Text(
                    text = "· $typeTag",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = badgeColor
                )
                if (item.user.isVerified) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = IgBlue,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        when (item.type) {
            NotificationType.FOLLOW_REQUEST -> {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onConfirmRequest,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IgBlue,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("confirm_request_${item.id}")
                    ) {
                        Text(
                            text = "Confirm",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    OutlinedButton(
                        onClick = onDeleteRequest,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("delete_request_${item.id}")
                    ) {
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            NotificationType.FOLLOW -> {
                Button(
                    onClick = onToggleFollow,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else IgBlue,
                        contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("activity_follow_button_${item.id}")
                ) {
                    Text(
                        text = if (isFollowing) "Following" else "Follow back",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            else -> {
                if (item.postThumbnailUrl != null) {
                    AsyncImage(
                        model = item.postThumbnailUrl,
                        contentDescription = if (isStory) "Story thumbnail" else "Post thumbnail",
                        modifier = Modifier
                            .width(if (isStory) 36.dp else 46.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isStory) 1.dp else 0.dp,
                                color = if (isStory) IgOrange.copy(alpha = 0.6f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNotificationsState(
    filterLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No $filterLabel notifications yet",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Likes, comments, mentions, story interactions, and follow activity appear here with timestamps.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatReadableActivityTimestamp(raw: String): String {
    return when {
        raw.equals("Just now", ignoreCase = true) -> "Just now"
        raw.equals("Yesterday", ignoreCase = true) -> "Yesterday"
        raw.endsWith("m") || raw.endsWith("h") || raw.endsWith("d") || raw.endsWith("w") -> "$raw ago"
        else -> raw
    }
}
