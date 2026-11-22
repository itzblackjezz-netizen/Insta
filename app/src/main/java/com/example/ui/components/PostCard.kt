package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Post
import com.example.ui.theme.IgBlue
import com.example.ui.theme.IgLightHashtag
import com.example.ui.theme.IgRed
import com.example.ui.theme.InstagramGradient
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PostCard(
    post: Post,
    onLikeClick: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onOptionsClick: () -> Unit,
    onUserClick: () -> Unit,
    onAddQuickComment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showHeartAnimation by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    var quickCommentText by remember { mutableStateOf("") }
    var isCaptionExpanded by remember { mutableStateOf(false) }

    val formattedLikes = remember(post.likesCount) {
        NumberFormat.getNumberInstance(Locale.US).format(post.likesCount)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .testTag("post_card_${post.id}")
    ) {
        // Post Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onUserClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar with Story Ring
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.5.dp, InstagramGradient, CircleShape)
                        .padding(2.dp)
                ) {
                    AsyncImage(
                        model = post.user.avatarUrl,
                        contentDescription = "${post.user.username}'s avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.user.username,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (post.user.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = IgBlue,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    if (!post.location.isNullOrBlank()) {
                        Text(
                            text = post.location,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (!post.musicTrack.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = post.musicTrack,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Options Button
            IconButton(
                onClick = onOptionsClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("post_options_button_${post.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Post Options",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Media Carousel / Image Container
        val pagerState = rememberPagerState(pageCount = { post.mediaUrls.size })

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(post.aspectRatio)
                .background(Color.Black.copy(alpha = 0.05f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            showHeartAnimation = true
                            onDoubleTapLike()
                        }
                    )
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                AsyncImage(
                    model = post.mediaUrls[pageIndex],
                    contentDescription = "Post media ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Video indicator / audio toggle
            if (post.isVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { isMuted = !isMuted },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Carousel Counter Badge (e.g. 1/2) in top-right
            if (post.mediaUrls.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1}/${post.mediaUrls.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                    )
                }
            }

            // Double tap bursting heart animation
            DoubleTapHeartAnimation(
                trigger = showHeartAnimation,
                onAnimationEnd = { showHeartAnimation = false }
            )
        }

        // Action Buttons Row (Like, Comment, Share, Indicators, Save)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Like Button with Bounce
                val likeScale by animateFloatAsState(
                    targetValue = if (post.isLiked) 1.15f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "likeScale"
                )
                val likeColor by animateColorAsState(
                    targetValue = if (post.isLiked) IgRed else MaterialTheme.colorScheme.onBackground,
                    label = "likeColor"
                )

                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier
                        .size(34.dp)
                        .scale(likeScale)
                        .testTag("like_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (post.isLiked) "Unlike" else "Like",
                        tint = likeColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Comment Button
                IconButton(
                    onClick = onCommentClick,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("comment_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Share Button
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("share_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Send,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }

            // Pager Dots Indicator in center
            if (post.mediaUrls.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(post.mediaUrls.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 6.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) IgBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
            }

            // Save / Bookmark Button
            IconButton(
                onClick = onSaveClick,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("save_button_${post.id}")
            ) {
                Icon(
                    imageVector = if (post.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (post.isSaved) "Unsave" else "Save",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(25.dp)
                )
            }
        }

        // Likes Count
        Text(
            text = "$formattedLikes likes",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .clickable(onClick = onCommentClick)
                .testTag("likes_count_${post.id}")
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Caption with Hashtags
        val captionString = remember(post.caption, post.user.username, post.hashtags) {
            buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Unspecified
                    )
                ) {
                    append(post.user.username)
                    append(" ")
                }
                append(post.caption)
                if (post.hashtags.isNotEmpty()) {
                    append(" ")
                    post.hashtags.forEach { tag ->
                        withStyle(
                            SpanStyle(
                                color = IgLightHashtag,
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append("$tag ")
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            Text(
                text = captionString,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = if (isCaptionExpanded) Int.MAX_VALUE else 2
            )
            if (!isCaptionExpanded && post.caption.length > 80) {
                Text(
                    text = "... more",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier
                        .clickable { isCaptionExpanded = true }
                        .padding(top = 2.dp)
                )
            }
        }

        // Comments Count / Preview
        if (post.commentsCount > 0) {
            Text(
                text = "View all ${post.commentsCount} comments",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.5.sp
                ),
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .clickable(onClick = onCommentClick)
                    .testTag("view_comments_button_${post.id}")
            )

            // Top Comment Preview
            post.comments.firstOrNull()?.let { topComment ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp
                                )
                            ) {
                                append(topComment.username)
                                append(" ")
                            }
                            append(topComment.text)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.5.sp,
                            lineHeight = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                }
            }
        }

        // Inline Quick Comment Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text input
            TextField(
                value = quickCommentText,
                onValueChange = { quickCommentText = it },
                placeholder = {
                    Text(
                        text = "Add a comment...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 12.5.sp
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("quick_comment_input_${post.id}"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            if (quickCommentText.isNotBlank()) {
                Text(
                    text = "Post",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = IgBlue
                    ),
                    modifier = Modifier
                        .clickable {
                            onAddQuickComment(quickCommentText)
                            quickCommentText = ""
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("quick_comment_post_button_${post.id}")
                )
            }
        }

        // Time Posted
        Text(
            text = post.timestamp.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            ),
            modifier = Modifier.padding(start = 14.dp, bottom = 12.dp)
        )

        // Subtle divider separating posts
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            thickness = 0.5.dp
        )
    }
}
