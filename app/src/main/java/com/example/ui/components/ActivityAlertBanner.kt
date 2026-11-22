package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.theme.IgBlue
import com.example.ui.theme.IgPink
import com.example.ui.theme.IgRed
import kotlinx.coroutines.delay

@Composable
fun ActivityAlertBanner(
    notification: NotificationItem?,
    onOpenActivityFeed: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(notification?.id) {
        if (notification != null) {
            delay(4200)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = notification != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        notification?.let { item ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .clickable {
                        onOpenActivityFeed()
                    }
                    .testTag("activity_alert_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(
                            model = item.user.avatarUrl,
                            contentDescription = item.user.username,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        val isLike = item.type == NotificationType.LIKE
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isLike) IgRed else IgBlue)
                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isLike) Icons.Default.Favorite else Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "New Activity",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = IgPink,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(item.user.username)
                                    append(" ")
                                }
                                append(item.text)
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (item.postThumbnailUrl != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AsyncImage(
                            model = item.postThumbnailUrl,
                            contentDescription = "Post thumbnail",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("dismiss_activity_alert_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss notification",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
