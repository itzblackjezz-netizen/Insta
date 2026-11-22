package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.Story
import com.example.ui.theme.IgBlue
import com.example.ui.theme.IgRed
import com.example.ui.theme.InstagramGradient
import kotlinx.coroutines.delay

@Composable
fun StoryViewerDialog(
    story: Story,
    hasPreviousStory: Boolean,
    hasNextStory: Boolean,
    onDismiss: () -> Unit,
    onAdvanceStory: () -> Unit,
    onPreviousStory: () -> Unit,
    onToggleLikeStory: () -> Unit,
    onSendReply: (String) -> Unit,
    onReactEmoji: (String) -> Unit,
    onShareStory: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    val focusManager = LocalFocusManager.current
    var currentSlideIndex by remember(story.id) { mutableIntStateOf(0) }
    var isManuallyPaused by remember(story.id) { mutableStateOf(false) }
    var isHoldPaused by remember { mutableStateOf(false) }
    var isReplyFocused by remember { mutableStateOf(false) }
    var replyText by remember(story.id) { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var burstEmoji by remember { mutableStateOf<String?>(null) }

    val totalSlides = maxOf(1, story.mediaUrls.size)
    val progress = remember(story.id, currentSlideIndex) { Animatable(0f) }
    val effectivePaused = isManuallyPaused || isHoldPaused || isReplyFocused || replyText.isNotEmpty()

    fun goPrevious() {
        isManuallyPaused = false
        if (currentSlideIndex > 0) {
            currentSlideIndex--
        } else {
            onPreviousStory()
        }
    }

    fun goNext() {
        isManuallyPaused = false
        if (currentSlideIndex < totalSlides - 1) {
            currentSlideIndex++
        } else {
            onAdvanceStory()
        }
    }

    fun submitReply() {
        val trimmed = replyText.trim()
        if (trimmed.isNotEmpty()) {
            onSendReply(trimmed)
            replyText = ""
            isReplyFocused = false
            isManuallyPaused = false
            focusManager.clearFocus()
            feedbackMessage = "Reply sent to @${story.username}"
        }
    }

    // Auto-advance progress timer
    LaunchedEffect(story.id, currentSlideIndex, effectivePaused) {
        if (!effectivePaused) {
            val remainingMs = maxOf(1, ((1f - progress.value) * 4500f).toInt())
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = remainingMs,
                    easing = LinearEasing
                )
            )
            if (currentSlideIndex < totalSlides - 1) {
                currentSlideIndex++
            } else {
                onAdvanceStory()
            }
        }
    }

    // Feedback overlay auto-clear
    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            delay(1400)
            feedbackMessage = null
        }
    }

    // Floating emoji burst auto-clear
    LaunchedEffect(burstEmoji) {
        if (burstEmoji != null) {
            delay(950)
            burstEmoji = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("story_viewer_dialog"),
            contentAlignment = Alignment.Center
        ) {
            val isWideScreen = maxWidth >= 600.dp

            // Desktop/Tablet Previous Story Chevron
            if (isWideScreen && (hasPreviousStory || currentSlideIndex > 0)) {
                IconButton(
                    onClick = ::goPrevious,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 24.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .testTag("prev_story_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Story",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Story Stage Container
            Box(
                modifier = Modifier
                    .then(
                        if (isWideScreen) {
                            Modifier
                                .widthIn(max = 460.dp)
                                .fillMaxHeight(0.92f)
                                .clip(RoundedCornerShape(20.dp))
                        } else {
                            Modifier.fillMaxSize()
                        }
                    )
                    .background(Color(0xFF0D0D0D))
            ) {
                // Media Background with Tap Navigation, Tap-to-Pause/Continue, Hold-to-Pause, and Swipe
                val currentMedia = story.mediaUrls.getOrNull(currentSlideIndex) ?: story.userAvatar
                var horizontalDragAccum by remember { mutableFloatStateOf(0f) }

                AsyncImage(
                    model = currentMedia,
                    contentDescription = "${story.username} story slide ${currentSlideIndex + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(story.id, currentSlideIndex, totalSlides) {
                            detectTapGestures(
                                onPress = {
                                    isHoldPaused = true
                                    tryAwaitRelease()
                                    isHoldPaused = false
                                },
                                onTap = { offset ->
                                    if (isReplyFocused) {
                                        focusManager.clearFocus()
                                        isReplyFocused = false
                                        return@detectTapGestures
                                    }
                                    val screenWidth = size.width
                                    when {
                                        offset.x < screenWidth * 0.30f -> goPrevious()
                                        offset.x > screenWidth * 0.70f -> goNext()
                                        else -> isManuallyPaused = !isManuallyPaused
                                    }
                                }
                            )
                        }
                        .pointerInput(story.id, currentSlideIndex) {
                            detectHorizontalDragGestures(
                                onDragStart = { horizontalDragAccum = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    horizontalDragAccum += dragAmount
                                },
                                onDragEnd = {
                                    if (horizontalDragAccum < -90f) {
                                        onAdvanceStory()
                                    } else if (horizontalDragAccum > 90f) {
                                        onPreviousStory()
                                    }
                                    horizontalDragAccum = 0f
                                }
                            )
                        },
                    contentScale = ContentScale.Crop
                )

                // Top Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.82f), Color.Transparent)
                            )
                        )
                )

                // Top Bar: Multi-segment Progress Bars + Author Header + Pause & Close Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("story_progress_bars"),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(totalSlides) { index ->
                            val segmentProgress = when {
                                index < currentSlideIndex -> 1f
                                index == currentSlideIndex -> progress.value
                                else -> 0f
                            }
                            LinearProgressIndicator(
                                progress = { segmentProgress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.34f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Gradient-ringed author circular avatar
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .border(2.dp, InstagramGradient, CircleShape)
                                    .padding(3.dp)
                                    .clip(CircleShape)
                            ) {
                                AsyncImage(
                                    model = story.userAvatar,
                                    contentDescription = "${story.username} avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (story.isYourStory) "Your story" else story.username,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = story.timestamp,
                                        color = Color.White.copy(alpha = 0.72f),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                                    )
                                }
                                AnimatedVisibility(visible = effectivePaused) {
                                    Text(
                                        text = "Paused",
                                        color = Color.White.copy(alpha = 0.85f),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Pause / Continue Toggle Button
                            IconButton(
                                onClick = { isManuallyPaused = !isManuallyPaused },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("story_pause_play_button")
                            ) {
                                Icon(
                                    imageVector = if (effectivePaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (effectivePaused) "Continue Story" else "Pause Story",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Close Story Button
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("close_story_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Story",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Story Caption Pill (above bottom controls or centered cleanly)
                if (story.caption.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(start = 20.dp, end = 20.dp, bottom = 146.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.52f))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = story.caption,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.5.sp
                            )
                        )
                    }
                }

                // Center Tap-to-Continue Badge when Manually Paused
                AnimatedVisibility(
                    visible = isManuallyPaused && !isReplyFocused && burstEmoji == null && feedbackMessage == null,
                    enter = fadeIn() + scaleIn(initialScale = 0.85f),
                    exit = fadeOut() + scaleOut(targetScale = 0.85f),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Surface(
                        onClick = { isManuallyPaused = false },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.testTag("story_paused_overlay")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume story",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Paused • Tap to continue",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                // Animated Floating Emoji Burst
                AnimatedVisibility(
                    visible = burstEmoji != null,
                    enter = fadeIn() + scaleIn(
                        initialScale = 0.4f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ),
                    exit = fadeOut() + scaleOut(targetScale = 1.4f),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    val scale by animateFloatAsState(
                        targetValue = if (burstEmoji != null) 1.15f else 0.8f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "emojiBurstScale"
                    )
                    Text(
                        text = burstEmoji.orEmpty(),
                        fontSize = 68.sp,
                        modifier = Modifier.scale(scale)
                    )
                }

                // Reply / Reaction Confirmation Toast
                AnimatedVisibility(
                    visible = feedbackMessage != null,
                    enter = fadeIn() + scaleIn(initialScale = 0.9f),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 96.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.82f))
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = feedbackMessage.orEmpty(),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                // Bottom Gradient Scrim
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                            )
                        )
                )

                // Bottom Story Emoji Reactions + Reply Input Bar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    // Quick Emoji Reactions Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val emojis = listOf("🔥", "❤️", "😂", "😮", "👏", "😍")
                        emojis.forEach { emoji ->
                            val isSelected = story.lastReaction == emoji
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.24f) else Color.Transparent
                                    )
                                    .clickable {
                                        burstEmoji = emoji
                                        feedbackMessage = "Reacted $emoji to @${story.username}"
                                        onReactEmoji(emoji)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("story_emoji_$emoji"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 24.sp
                                )
                            }
                        }
                    }

                    // Reply Input & Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = {
                                Text(
                                    text = if (story.isYourStory) "Add a note..." else "Reply to ${story.username}...",
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 13.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .onFocusChanged { isReplyFocused = it.isFocused }
                                .testTag("story_reply_input"),
                            shape = CircleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White.copy(alpha = 0.85f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.45f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White,
                                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submitReply() }),
                            singleLine = true
                        )

                        if (replyText.isNotBlank()) {
                            IconButton(
                                onClick = ::submitReply,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(IgBlue)
                                    .testTag("send_story_reply_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Reply",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = onToggleLikeStory,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("story_like_button")
                            ) {
                                Icon(
                                    imageVector = if (story.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (story.isLiked) "Unlike Story" else "Like Story",
                                    tint = if (story.isLiked) IgRed else Color.White,
                                    modifier = Modifier.size(27.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    feedbackMessage = "Shared @${story.username}'s story"
                                    onShareStory()
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("story_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Send,
                                    contentDescription = "Share Story",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Desktop/Tablet Next Story Chevron
            if (isWideScreen && (hasNextStory || currentSlideIndex < totalSlides - 1)) {
                IconButton(
                    onClick = ::goNext,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .testTag("next_story_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Story",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
