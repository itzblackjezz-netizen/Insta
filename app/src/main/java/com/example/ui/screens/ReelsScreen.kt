package com.example.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import coil.compose.AsyncImage
import com.example.model.Reel
import com.example.model.User
import com.example.ui.components.DoubleTapHeartAnimation
import com.example.ui.components.ReelCommentsBottomSheet
import com.example.ui.components.ReelOptionsBottomSheet
import com.example.ui.components.ReelShareBottomSheet
import com.example.ui.theme.IgBlue
import com.example.ui.theme.IgRed
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ReelsScreen(
    reels: List<Reel>,
    currentUser: User,
    followedUsers: Map<String, Boolean>,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onLikeReel: (String) -> Unit,
    onDoubleTapLikeReel: (String) -> Unit,
    onSaveReel: (String) -> Unit,
    onToggleFollowUser: (String) -> Unit,
    onAddReelComment: (String, String) -> Unit,
    onShareReel: (Reel, User?) -> Unit,
    onCreateReelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { reels.size })
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppResumed by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isAppResumed = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isAppResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var activeCommentsReelId by remember { mutableStateOf<String?>(null) }
    var activeShareReelId by remember { mutableStateOf<String?>(null) }
    var activeOptionsReelId by remember { mutableStateOf<String?>(null) }

    val activeCommentsReel = remember(activeCommentsReelId, reels) {
        activeCommentsReelId?.let { id -> reels.firstOrNull { it.id == id } }
    }
    val activeShareReel = remember(activeShareReelId, reels) {
        activeShareReelId?.let { id -> reels.firstOrNull { it.id == id } }
    }
    val activeOptionsReel = remember(activeOptionsReelId, reels) {
        activeOptionsReelId?.let { id -> reels.firstOrNull { it.id == id } }
    }

    val isSheetOpen = activeCommentsReel != null || activeShareReel != null || activeOptionsReel != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("reels_screen")
    ) {
        if (reels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No reels available yet",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("reels_vertical_pager")
            ) { pageIndex ->
                val reel = reels[pageIndex]
                val isVisiblePage = pagerState.currentPage == pageIndex
                val isFollowingCreator = followedUsers[reel.user.id] ?: reel.user.isFollowing

                ReelItem(
                    reel = reel,
                    isVisiblePage = isVisiblePage,
                    shouldAutoPlay = isVisiblePage && isAppResumed && !isSheetOpen,
                    isMuted = isMuted,
                    isFollowingCreator = isFollowingCreator,
                    onLike = { onLikeReel(reel.id) },
                    onDoubleTapLike = { onDoubleTapLikeReel(reel.id) },
                    onComment = { activeCommentsReelId = reel.id },
                    onShare = { activeShareReelId = reel.id },
                    onSave = { onSaveReel(reel.id) },
                    onMoreOptions = { activeOptionsReelId = reel.id },
                    onToggleFollow = { onToggleFollowUser(reel.user.id) }
                )
            }
        }

        // Top Reels Header (Title + Mute toggle + Camera)
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("reels_mute_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute reel audio" else "Mute reel audio",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onCreateReelClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("reels_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Create Reel",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    activeCommentsReel?.let { reel ->
        ReelCommentsBottomSheet(
            reel = reel,
            currentUser = currentUser,
            onDismiss = { activeCommentsReelId = null },
            onAddComment = { text -> onAddReelComment(reel.id, text) }
        )
    }

    activeShareReel?.let { reel ->
        ReelShareBottomSheet(
            reel = reel,
            onDismiss = { activeShareReelId = null },
            onShareToUser = { friend -> onShareReel(reel, friend) },
            onCopyLink = { onShareReel(reel, null) }
        )
    }

    activeOptionsReel?.let { reel ->
        ReelOptionsBottomSheet(
            reel = reel,
            isFollowingCreator = followedUsers[reel.user.id] ?: reel.user.isFollowing,
            isMuted = isMuted,
            onDismiss = { activeOptionsReelId = null },
            onToggleSave = { onSaveReel(reel.id) },
            onToggleMute = onToggleMute,
            onToggleFollow = { onToggleFollowUser(reel.user.id) }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ReelItem(
    reel: Reel,
    isVisiblePage: Boolean,
    shouldAutoPlay: Boolean,
    isMuted: Boolean,
    isFollowingCreator: Boolean,
    onLike: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onMoreOptions: () -> Unit,
    onToggleFollow: () -> Unit
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    var isUserPaused by remember(reel.id) { mutableStateOf(false) }
    var isCaptionExpanded by remember(reel.id) { mutableStateOf(false) }
    var showDoubleTapHeart by remember { mutableStateOf(false) }
    var showPlayPausePulse by remember { mutableStateOf(false) }

    // Reset pause state whenever the user swipes away from this page
    LaunchedEffect(isVisiblePage) {
        if (!isVisiblePage) {
            isUserPaused = false
            showPlayPausePulse = false
        }
    }

    // Auto-hide transient play/pause feedback pill
    LaunchedEffect(showPlayPausePulse, isUserPaused) {
        if (showPlayPausePulse) {
            delay(650)
            showPlayPausePulse = false
        }
    }

    val isEffectivelyPlaying = shouldAutoPlay && !isUserPaused

    // Manage ExoPlayer lifecycle only for the visible video page so hardware decoders stay free
    var activePlayer by remember(reel.id) { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(reel.id, reel.videoUrl, isVisiblePage, isInPreview) {
        if (!isVisiblePage || reel.videoUrl.isBlank() || isInPreview) {
            onDispose { }
        } else {
            val player = runCatching {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(Uri.parse(reel.videoUrl)))
                    repeatMode = Player.REPEAT_MODE_ONE
                    volume = if (isMuted) 0f else 1f
                    playWhenReady = isEffectivelyPlaying
                    prepare()
                }
            }.getOrNull()

            activePlayer = player

            onDispose {
                activePlayer = null
                player?.release()
            }
        }
    }

    // Sync play/pause & mute state cleanly to active ExoPlayer
    LaunchedEffect(activePlayer, isEffectivelyPlaying, isMuted) {
        activePlayer?.let { player ->
            player.volume = if (isMuted) 0f else 1f
            player.playWhenReady = isEffectivelyPlaying
            if (isEffectivelyPlaying && !player.isPlaying) {
                player.play()
            } else if (!isEffectivelyPlaying && player.isPlaying) {
                player.pause()
            }
        }
    }

    // Vinyl disc rotation driven smoothly while playing
    var discAngle by remember(reel.id) { mutableFloatStateOf(0f) }
    LaunchedEffect(isEffectivelyPlaying) {
        if (isEffectivelyPlaying) {
            var lastTimeNanos = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val deltaSec = (now - lastTimeNanos) / 1_000_000_000f
                lastTimeNanos = now
                discAngle = (discAngle + deltaSec * 90f) % 360f
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(reel.id) {
                detectTapGestures(
                    onTap = {
                        isUserPaused = !isUserPaused
                        showPlayPausePulse = true
                    },
                    onDoubleTap = {
                        showDoubleTapHeart = true
                        onDoubleTapLike()
                    }
                )
            }
            .testTag("reel_item_${reel.id}")
    ) {
        // Video Surface via Media3 Compose ContentFrame with poster shutter
        if (activePlayer != null) {
            ContentFrame(
                player = activePlayer,
                modifier = Modifier.fillMaxSize(),
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                contentScale = ContentScale.Crop,
                shutter = {
                    AsyncImage(
                        model = reel.mediaUrl,
                        contentDescription = reel.caption,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            )
        } else {
            AsyncImage(
                model = reel.mediaUrl,
                contentDescription = reel.caption,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Top & Bottom Gradient Scrims for legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                    )
                )
        )

        // Play / Pause Indicator Overlay
        AnimatedVisibility(
            visible = isUserPaused || showPlayPausePulse,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.52f))
                    .testTag("reel_play_pause_indicator_${reel.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUserPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isUserPaused) "Paused, tap to play" else "Playing",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        // Double-Tap Heart Burst Animation
        DoubleTapHeartAnimation(
            trigger = showDoubleTapHeart,
            onAnimationEnd = { showDoubleTapHeart = false }
        )

        // Right Action Rail (Like, Comment, Share, Save, More, Vinyl Disc)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ActionRailItem(
                icon = if (reel.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                count = formatReelCount(reel.likesCount),
                tint = if (reel.isLiked) IgRed else Color.White,
                contentDescription = if (reel.isLiked) "Unlike reel" else "Like reel",
                testTag = "reel_like_button_${reel.id}",
                onClick = onLike
            )

            ActionRailItem(
                icon = Icons.Outlined.ChatBubbleOutline,
                count = formatReelCount(reel.commentsCount),
                tint = Color.White,
                contentDescription = "Comments",
                testTag = "reel_comment_button_${reel.id}",
                onClick = onComment
            )

            ActionRailItem(
                icon = Icons.Outlined.Send,
                count = formatReelCount(reel.sharesCount),
                tint = Color.White,
                contentDescription = "Share reel",
                testTag = "reel_share_button_${reel.id}",
                onClick = onShare
            )

            ActionRailItem(
                icon = if (reel.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                count = null,
                tint = Color.White,
                contentDescription = if (reel.isSaved) "Remove from saved" else "Save reel",
                testTag = "reel_save_button_${reel.id}",
                onClick = onSave
            )

            IconButton(
                onClick = onMoreOptions,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("reel_more_button_${reel.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Spinning Audio Vinyl Disc
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                    .rotate(discAngle),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = reel.user.avatarUrl,
                    contentDescription = "Audio cover art",
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Bottom-Left Creator, Follow, Caption, and Audio Info
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AsyncImage(
                    model = reel.user.avatarUrl,
                    contentDescription = reel.user.username,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                    contentScale = ContentScale.Crop
                )

                Text(
                    text = reel.user.username,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (reel.user.isVerified) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified creator",
                        tint = IgBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isFollowingCreator) Color.White.copy(alpha = 0.18f)
                            else Color.Transparent
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = if (isFollowingCreator) 0.4f else 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable(onClick = onToggleFollow)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .testTag("reel_follow_button_${reel.id}")
                ) {
                    Text(
                        text = if (isFollowingCreator) "Following" else "Follow",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expandable Caption
            Text(
                text = reel.caption,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                maxLines = if (isCaptionExpanded) 6 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { isCaptionExpanded = !isCaptionExpanded }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Audio / Music Track Pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    Color.White.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Audio track",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = reel.audioTitle,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: String?,
    tint: Color,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
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
