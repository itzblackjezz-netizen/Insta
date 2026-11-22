package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Reel
import com.example.ui.theme.IgRed
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ReelsScreen(
    reels: List<Reel>,
    onLikeReel: (String) -> Unit,
    onSaveReel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { reels.size })

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("reels_screen")
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val reel = reels[pageIndex]
            ReelItem(
                reel = reel,
                onLike = { onLikeReel(reel.id) },
                onSave = { onSaveReel(reel.id) }
            )
        }

        // Top Reels Header (Camera icon + Title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Reels",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = Color.White
            )

            IconButton(
                onClick = { /* Open Reels Camera */ },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ReelItem(
    reel: Reel,
    onLike: () -> Unit,
    onSave: () -> Unit
) {
    var isFollowing by remember { mutableStateOf(reel.user.isFollowing) }

    // Infinite spinning animation for the audio record disc
    val infiniteTransition = rememberInfiniteTransition(label = "discRotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Reel Media (Video / Photo background)
        AsyncImage(
            model = reel.mediaUrl,
            contentDescription = reel.caption,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient Scrim for readable text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Right Action Rail (Like, Comment, Share, Save, Music Disc)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Like
            ActionRailItem(
                icon = if (reel.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                count = formatReelCount(reel.likesCount),
                tint = if (reel.isLiked) IgRed else Color.White,
                onClick = onLike
            )

            // Comment
            ActionRailItem(
                icon = Icons.Outlined.ChatBubbleOutline,
                count = formatReelCount(reel.commentsCount),
                tint = Color.White,
                onClick = { /* Open reel comments */ }
            )

            // Share
            ActionRailItem(
                icon = Icons.Outlined.Send,
                count = formatReelCount(reel.sharesCount),
                tint = Color.White,
                onClick = { /* Share reel */ }
            )

            // Save
            ActionRailItem(
                icon = if (reel.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                count = null,
                tint = Color.White,
                onClick = onSave
            )

            // Three Dots
            IconButton(
                onClick = { /* Options */ },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Spinning Vinyl Disc
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                    .rotate(rotation),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = reel.user.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Creator and Caption Overlay on Bottom Left
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.8f)
                .padding(start = 14.dp, bottom = 24.dp)
        ) {
            // Author + Follow Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = reel.user.avatarUrl,
                    contentDescription = reel.user.username,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = reel.user.username,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .clickable { isFollowing = !isFollowing }
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isFollowing) "Following" else "Follow",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Caption
            Text(
                text = reel.caption,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Audio Track Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = reel.audioTitle,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp)
                )
            }
        }
    }
}

@Composable
private fun ActionRailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: String?,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        if (count != null) {
            Text(
                text = count,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp
                )
            )
        }
    }
}

private fun formatReelCount(count: Int): String {
    return if (count >= 1000) {
        String.format(Locale.US, "%.1fK", count / 1000.0)
    } else {
        NumberFormat.getNumberInstance(Locale.US).format(count)
    }
}
