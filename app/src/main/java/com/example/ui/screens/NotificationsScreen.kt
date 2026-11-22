package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.NotificationItem
import com.example.model.NotificationType
import com.example.ui.theme.IgBlue
import com.example.ui.theme.IgPink
import com.example.ui.theme.IgRed

private enum class ActivityFilter(val label: String) {
    ALL("All"),
    LIKES("Likes"),
    FOLLOWS("Follows"),
    COMMENTS("Comments")
}

@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
    followedUsers: Map<String, Boolean>,
    onToggleFollowUser: (String) -> Unit,
    onPostNotificationClick: (NotificationItem) -> Unit,
    onSimulateActivity: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var selectedFilter by remember { mutableStateOf(ActivityFilter.ALL) }

    val filteredNotifications = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            ActivityFilter.ALL -> notifications
            ActivityFilter.LIKES -> notifications.filter { it.type == NotificationType.LIKE }
            ActivityFilter.FOLLOWS -> notifications.filter { it.type == NotificationType.FOLLOW }
            ActivityFilter.COMMENTS -> notifications.filter {
                it.type == NotificationType.COMMENT || it.type == NotificationType.MENTION
            }
        }
    }

    val recentItems = remember(filteredNotifications) {
        filteredNotifications.filter { it.timestamp == "Just now" || it.timestamp.endsWith("m") }
    }
    val earlierItems = remember(filteredNotifications) {
        filteredNotifications.filterNot { it.timestamp == "Just now" || it.timestamp.endsWith("m") }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .testTag("notifications_screen")
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
                        text = "Activity",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${notifications.size} recent updates",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Refresh / Check for new local activity alerts
            TextButton(
                onClick = onSimulateActivity,
                modifier = Modifier.testTag("activity_check_updates_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = IgBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Check",
                    color = IgBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        // Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ActivityFilter.entries) { filter ->
                val isSelected = selectedFilter == filter
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clickable { selectedFilter = filter }
                        .testTag("activity_filter_${filter.name.lowercase()}")
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            thickness = 0.5.dp
        )

        // Activity Feed List
        if (filteredNotifications.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
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
                    text = "No activity in this category",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "When someone likes your posts or follows your account, updates appear right here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("activity_feed_list"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (recentItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "New",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    items(recentItems, key = { it.id }) { item ->
                        ActivityNotificationRow(
                            item = item,
                            isFollowing = followedUsers[item.user.id] ?: item.isFollowingBack,
                            onToggleFollow = { onToggleFollowUser(item.user.id) },
                            onClick = { onPostNotificationClick(item) }
                        )
                    }
                }

                if (earlierItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "This week",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = if (recentItems.isNotEmpty()) 8.dp else 0.dp)
                        )
                    }
                    items(earlierItems, key = { it.id }) { item ->
                        ActivityNotificationRow(
                            item = item,
                            isFollowing = followedUsers[item.user.id] ?: item.isFollowingBack,
                            onToggleFollow = { onToggleFollowUser(item.user.id) },
                            onClick = { onPostNotificationClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityNotificationRow(
    item: NotificationItem,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
    onClick: () -> Unit
) {
    val badgeColor = when (item.type) {
        NotificationType.LIKE -> IgRed
        NotificationType.FOLLOW -> IgBlue
        NotificationType.COMMENT, NotificationType.MENTION -> IgPink
    }
    val badgeIcon = when (item.type) {
        NotificationType.LIKE -> Icons.Default.Favorite
        NotificationType.FOLLOW -> Icons.Default.PersonAdd
        NotificationType.COMMENT, NotificationType.MENTION -> Icons.Default.ChatBubble
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
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = item.user.avatarUrl,
                contentDescription = item.user.username,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(item.user.username)
                    append(" ")
                }
                append(item.text)
                append(" ")
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                ) {
                    append(item.timestamp)
                }
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(10.dp))

        if (item.type == NotificationType.FOLLOW) {
            Button(
                onClick = onToggleFollow,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else IgBlue,
                    contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("activity_follow_button_${item.id}")
            ) {
                Text(
                    text = if (isFollowing) "Following" else "Follow back",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        } else if (item.postThumbnailUrl != null) {
            AsyncImage(
                model = item.postThumbnailUrl,
                contentDescription = "Post thumbnail",
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}
