package com.example.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.model.Reel
import com.example.model.User
import com.example.ui.components.DoubleTapHeartAnimation
import com.example.ui.components.ReelCommentsBottomSheet
import com.example.ui.components.ReelOptionsBottomSheet
import com.example.ui.components.ReelShareBottomSheet
import com.example.ui.theme.IgBlue
import com.example.ui.theme.IgRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val PLAYBACK_SPEED_OPTIONS = listOf(1.0f, 1.5f, 2.0f)

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    reels: List<Reel>,
    currentUser: User,
    followedUsers: Map<String, Boolean>,
    isMuted: Boolean,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onToggleMute: () -> Unit,
    onLikeReel: (String) -> Unit,
    onDoubleTapLikeReel: (String) -> Unit,
    onSaveReel: (String) -> Unit,
    onToggleFollowUser: (String) -> Unit,
    onAddReelComment: (String, String) -> Unit,
    onShareReel: (Reel, User?) -> Unit,
    onCreateReelClick: () -> Unit,
    onUserClick: (User) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { reels.size })
    val pullToRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppResumed by remember { mutableStateOf(true) }
    var autoAdvanceEnabled by remember { mutableStateOf(false) }
    var playbackSpeedIndex by remember { mutableIntStateOf(0) }
    val selectedSpeed = PLAYBACK_SPEED_OPTIONS[playbackSpeedIndex]

    var wasRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing, reels.size) {
        if (wasRefreshing && !isRefreshing && reels.isNotEmpty() && pagerState.currentPage != 0) {
            pagerState.animateScrollToPage(0)
        }
        wasRefreshing = isRefreshing
    }

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

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                containerColor = Color.Black.copy(alpha = 0.85f),
                color = IgBlue,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp)
                    .testTag("reels_refresh_indicator")
            )
        },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("reels_screen")
    ) {
        if (reels.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp)
                    .testTag("reels_empty_state"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No reels available yet",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pull down to check for trending videos or tap Create Reel.",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCreateReelClick,
                    colors = ButtonDefaults.buttonColors(containerColor = IgBlue),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Create Reel")
                }
            }
        } else {
            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapPositionalThreshold = 0.25f
                ),
                key = { idx -> reels[idx].id },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("reels_vertical_pager")
            ) { pageIndex ->
                val reel = reels[pageIndex]
                val isVisiblePage = pagerState.currentPage == pageIndex
                // Keep at most 2 ExoPlayers alive (active page + adjacent next page pre-buffered)
                val shouldPreparePlayer = pageIndex == pagerState.currentPage ||
                    pageIndex == pagerState.currentPage + 1
                val isFollowingCreator = followedUsers[reel.user.id] ?: reel.user.isFollowing

                ReelItem(
                    reel = reel,
                    pageIndex = pageIndex,
                    totalReels = reels.size,
                    isVisiblePage = isVisiblePage,
                    shouldPreparePlayer = shouldPreparePlayer,
                    shouldAutoPlay = isVisiblePage && isAppResumed && !isSheetOpen,
                    isMuted = isMuted,
                    playbackSpeed = selectedSpeed,
                    autoAdvanceEnabled = autoAdvanceEnabled,
                    isFollowingCreator = isFollowingCreator,
                    onVideoEnded = {
                        if (autoAdvanceEnabled && pageIndex < reels.lastIndex) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pageIndex + 1)
                            }
                        }
                    },
                    onLike = { onLikeReel(reel.id) },
                    onDoubleTapLike = { onDoubleTapLikeReel(reel.id) },
                    onComment = { activeCommentsReelId = reel.id },
                    onShare = { activeShareReelId = reel.id },
                    onSave = { onSaveReel(reel.id) },
                    onMoreOptions = { activeOptionsReelId = reel.id },
                    onToggleFollow = { onToggleFollowUser(reel.user.id) },
                    onCreatorClick = { onUserClick(reel.user) }
                )
            }
        }

        // Top Reels Header (Title + Speed + Auto-Advance + Mute + Camera)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Reels",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = Color.White
                )
                if (reels.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1}/${reels.size}",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Playback Speed Toggle (1x -> 1.5x -> 2x)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.45f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Cycle playback speed"
                        ) {
                            playbackSpeedIndex = (playbackSpeedIndex + 1) % PLAYBACK_SPEED_OPTIONS.size
                        }
                        .testTag("reels_speed_button")
                ) {
                    Text(
                        text = if (selectedSpeed == 1.0f) "1x" else "${selectedSpeed}x",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Loop vs Auto-Next Toggle
                IconButton(
                    onClick = { autoAdvanceEnabled = !autoAdvanceEnabled },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("reels_auto_advance_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (autoAdvanceEnabled) IgBlue.copy(alpha = 0.8f)
                                else Color.Black.copy(alpha = 0.45f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (autoAdvanceEnabled) Icons.Default.SkipNext else Icons.Default.RepeatOne,
                            contentDescription = if (autoAdvanceEnabled) {
                                "Auto-advance enabled, tap to loop"
                            } else {
                                "Loop enabled, tap for auto-advance"
                            },
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

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
    pageIndex: Int,
    totalReels: Int,
    isVisiblePage: Boolean,
    shouldPreparePlayer: Boolean,
    shouldAutoPlay: Boolean,
    isMuted: Boolean,
    playbackSpeed: Float,
    autoAdvanceEnabled: Boolean,
    isFollowingCreator: Boolean,
    onVideoEnded: () -> Unit,
    onLike: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onMoreOptions: () -> Unit,
    onToggleFollow: () -> Unit,
    onCreatorClick: () -> Unit
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    var isUserPaused by remember(reel.id) { mutableStateOf(false) }
    var isHoldingFastForward by remember(reel.id) { mutableStateOf(false) }
    var isCaptionExpanded by remember(reel.id) { mutableStateOf(false) }
    var showDoubleTapHeart by remember { mutableStateOf(false) }
    var showPlayPausePulse by remember { mutableStateOf(false) }

    var isBuffering by remember(reel.id) { mutableStateOf(false) }
    var playbackError by remember(reel.id) { mutableStateOf<String?>(null) }
    var currentPositionMs by remember(reel.id) { mutableLongStateOf(0L) }
    var durationMs by remember(reel.id) { mutableLongStateOf(0L) }
    var isScrubbing by remember(reel.id) { mutableStateOf(false) }
    var scrubProgress by remember(reel.id) { mutableFloatStateOf(0f) }

    // Reset transient page state whenever the user swipes away from this page
    LaunchedEffect(isVisiblePage) {
        if (!isVisiblePage) {
            isUserPaused = false
            isHoldingFastForward = false
            showPlayPausePulse = false
            isScrubbing = false
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
    val effectiveSpeed = if (isHoldingFastForward && isEffectivelyPlaying) 2.0f else playbackSpeed

    // Manage ExoPlayer for the active page + adjacent pre-buffered next page
    var activePlayer by remember(reel.id) { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(reel.id, reel.videoUrl, shouldPreparePlayer, isInPreview) {
        if (!shouldPreparePlayer || reel.videoUrl.isBlank() || isInPreview) {
            onDispose { }
        } else {
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 2_500,
                    /* maxBufferMs = */ 12_000,
                    /* bufferForPlaybackMs = */ 400,
                    /* bufferForPlaybackAfterRebufferMs = */ 1_000
                )
                .build()

            val player = runCatching {
                ExoPlayer.Builder(context)
                    .setLoadControl(loadControl)
                    .build()
                    .apply {
                        setMediaItem(MediaItem.fromUri(Uri.parse(reel.videoUrl)))
                        repeatMode = if (autoAdvanceEnabled) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
                        volume = if (isMuted || !isVisiblePage) 0f else 1f
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

    // Observe ExoPlayer playback state, buffering, completion, and errors
    val currentOnVideoEnded by rememberUpdatedState(onVideoEnded)
    DisposableEffect(activePlayer) {
        val player = activePlayer ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    playbackError = null
                    val dur = player.duration
                    if (dur != C.TIME_UNSET && dur > 0L) {
                        durationMs = dur
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    currentOnVideoEnded()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playbackError = error.localizedMessage ?: "Unable to stream this video"
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    // Sync play/pause, mute, repeat mode, and playback speed to active ExoPlayer
    LaunchedEffect(activePlayer, isVisiblePage, isEffectivelyPlaying, isMuted, effectiveSpeed, autoAdvanceEnabled) {
        activePlayer?.let { player ->
            player.volume = if (isMuted || !isVisiblePage) 0f else 1f
            player.repeatMode = if (autoAdvanceEnabled) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
            player.playbackParameters = PlaybackParameters(effectiveSpeed)
            player.playWhenReady = isEffectivelyPlaying
            if (isEffectivelyPlaying && !player.isPlaying) {
                player.play()
            } else if (!isEffectivelyPlaying && player.isPlaying) {
                player.pause()
            }
        }
    }

    // Poll ExoPlayer playback position for smooth progress scrubber
    LaunchedEffect(activePlayer, isVisiblePage, isEffectivelyPlaying, isScrubbing) {
        val player = activePlayer ?: return@LaunchedEffect
        while (isVisiblePage) {
            if (!isScrubbing) {
                val dur = player.duration
                if (dur != C.TIME_UNSET && dur > 0L) {
                    durationMs = dur
                }
                currentPositionMs = player.currentPosition.coerceAtLeast(0L)
            }
            delay(if (isEffectivelyPlaying) 100L else 250L)
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

    val posterRequest = remember(context, reel.mediaUrl) {
        ImageRequest.Builder(context)
            .data(reel.mediaUrl)
            .crossfade(150)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    val liveProgress = if (isScrubbing) {
        scrubProgress
    } else if (durationMs > 0L) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .semantics {
                contentDescription = "Reel ${pageIndex + 1} of $totalReels by @${reel.user.username}: ${reel.caption}"
            }
            .pointerInput(reel.id) {
                detectTapGestures(
                    onPress = {
                        tryAwaitRelease()
                        isHoldingFastForward = false
                    },
                    onLongPress = {
                        if (!isUserPaused && playbackError == null) {
                            isHoldingFastForward = true
                        }
                    },
                    onTap = {
                        if (playbackError == null) {
                            isUserPaused = !isUserPaused
                            showPlayPausePulse = true
                        }
                    },
                    onDoubleTap = {
                        showDoubleTapHeart = true
                        onDoubleTapLike()
                    }
                )
            }
            .testTag("reel_item_${reel.id}")
    ) {
        // Video Surface via Media3 Compose ContentFrame with cached poster shutter
        if (activePlayer != null) {
            ContentFrame(
                player = activePlayer,
                modifier = Modifier.fillMaxSize(),
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                contentScale = ContentScale.Crop,
                shutter = {
                    AsyncImage(
                        model = posterRequest,
                        contentDescription = reel.caption,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            )
        } else {
            AsyncImage(
                model = posterRequest,
                contentDescription = reel.caption,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Top & Bottom Gradient Scrims for overlay contrast
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.58f), Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.90f))
                    )
                )
        )

        // Buffering Indicator
        if (isVisiblePage && isBuffering && playbackError == null && !isUserPaused) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .testTag("reel_buffering_indicator_${reel.id}"),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp,
                    color = Color.White
                )
            }
        }

        // Playback Error Overlay with Retry
        if (playbackError != null) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
                    .testTag("reel_error_overlay_${reel.id}")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = "Playback error",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Couldn't load video",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = playbackError ?: "Check your network connection and try again.",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Button(
                        onClick = {
                            playbackError = null
                            isBuffering = true
                            activePlayer?.apply {
                                setMediaItem(MediaItem.fromUri(Uri.parse(reel.videoUrl)))
                                prepare()
                                playWhenReady = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IgBlue),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reel_retry_button_${reel.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2x Hold-to-Fast-Forward Pill
        AnimatedVisibility(
            visible = isHoldingFastForward,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 60.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.72f),
                border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.3f)),
                modifier = Modifier.testTag("reel_fast_forward_pill_${reel.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "2x speed active",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "2x Speed",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // Play / Pause Indicator Overlay
        AnimatedVisibility(
            visible = (isUserPaused || showPlayPausePulse) && playbackError == null && !isHoldingFastForward,
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
                .padding(end = 10.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                icon = Icons.AutoMirrored.Outlined.Send,
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
                .padding(start = 16.dp, bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .minimumInteractiveComponentSize()
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "View @${reel.user.username}'s profile",
                            onClick = onCreatorClick
                        )
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
                        .clickable(
                            role = Role.Button,
                            onClickLabel = if (isFollowingCreator) "Unfollow" else "Follow",
                            onClick = onToggleFollow
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("reel_follow_button_${reel.id}")
                ) {
                    Text(
                        text = if (isFollowingCreator) "Following" else "Follow",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (isCaptionExpanded) "Collapse caption" else "Expand caption"
                    ) { isCaptionExpanded = !isCaptionExpanded }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Audio / Music Track Pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.45f),
                border = BorderStroke(
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

        // Bottom Playback Progress & Interactive Scrubber Bar
        ReelScrubberBar(
            reelId = reel.id,
            progress = liveProgress,
            durationMs = durationMs,
            isScrubbing = isScrubbing,
            onScrubStart = { fraction ->
                isScrubbing = true
                scrubProgress = fraction
            },
            onScrubChange = { fraction ->
                scrubProgress = fraction
            },
            onScrubEnd = {
                if (durationMs > 0L) {
                    activePlayer?.seekTo((scrubProgress * durationMs).toLong())
                }
                isScrubbing = false
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ReelScrubberBar(
    reelId: String,
    progress: Float,
    durationMs: Long,
    isScrubbing: Boolean,
    onScrubStart: (Float) -> Unit,
    onScrubChange: (Float) -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var barWidthPx by remember { mutableIntStateOf(1) }
    val trackHeight by animateDpAsState(
        targetValue = if (isScrubbing) 6.dp else 2.5.dp,
        label = "scrubber_height"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reel_scrubber_$reelId")
    ) {
        if (isScrubbing && durationMs > 0L) {
            val previewPositionMs = (progress * durationMs).toLong()
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 10.dp)
            ) {
                Text(
                    text = "${formatDurationMs(previewPositionMs)} / ${formatDurationMs(durationMs)}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .onSizeChanged { size ->
                    if (size.width > 0) barWidthPx = size.width
                }
                .pointerInput(reelId, durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val frac = (offset.x / barWidthPx.toFloat()).coerceIn(0f, 1f)
                            onScrubStart(frac)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val frac = (change.position.x / barWidthPx.toFloat()).coerceIn(0f, 1f)
                            onScrubChange(frac)
                        },
                        onDragEnd = onScrubEnd,
                        onDragCancel = onScrubEnd
                    )
                },
            contentAlignment = Alignment.BottomStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .background(Color.White.copy(alpha = 0.28f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(Color.White)
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

private fun formatDurationMs(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
