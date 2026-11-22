package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import coil.compose.AsyncImage
import com.example.data.SampleData
import com.example.model.ChatMediaType
import com.example.model.ChatMessage
import com.example.model.DirectMessage
import com.example.model.MessageDeliveryStatus
import com.example.model.User
import com.example.ui.theme.IgBlue
import com.example.ui.theme.IgRed

private val QUICK_REACTION_EMOJIS = listOf("❤️", "😂", "😮", "😢", "🙏", "🔥")

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    thread: DirectMessage,
    currentUser: User,
    showBackButton: Boolean = true,
    openMediaSheetInitially: Boolean = false,
    onMediaSheetOpened: () -> Unit = {},
    onBack: () -> Unit,
    onOpenUserProfile: (User) -> Unit,
    onSendTextMessage: (String) -> Unit,
    onSendMediaMessage: (mediaUri: String, mediaType: ChatMediaType, thumbnailUrl: String?, caption: String) -> Unit,
    onToggleReaction: (messageId: String, emoji: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (showBackButton) {
        BackHandler { onBack() }
    }

    val context = LocalContext.current
    val listState = rememberLazyListState()
    var messageInput by remember(thread.id) { mutableStateOf("") }
    var activeReactionTargetId by remember(thread.id) { mutableStateOf<String?>(null) }
    var showMediaPickerSheet by remember(thread.id) { mutableStateOf(false) }
    var previewMediaMessage by remember { mutableStateOf<ChatMessage?>(null) }

    LaunchedEffect(openMediaSheetInitially) {
        if (openMediaSheetInitially) {
            showMediaPickerSheet = true
            onMediaSheetOpened()
        }
    }

    // Zero-permission Android Photo Picker for both Images & Videos
    val visualMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            val mediaType = if (mimeType.startsWith("video/")) {
                ChatMediaType.VIDEO
            } else {
                ChatMediaType.IMAGE
            }
            onSendMediaMessage(uri.toString(), mediaType, uri.toString(), "")
            showMediaPickerSheet = false
        }
    }

    // Smoothly scroll to the newest message or typing indicator
    LaunchedEffect(thread.id, thread.messages.size, thread.isTyping) {
        val totalItems = thread.messages.size + if (thread.isTyping) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    val lastOutgoingMessageId = remember(thread.messages) {
        thread.messages.lastOrNull { it.isFromMe }?.id
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("chat_detail_screen")
    ) {
        // Chat Top Bar
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBackButton) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to messages",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Recipient Header Info (Avatar + Online Dot + Name + Status)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenUserProfile(thread.user) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("chat_recipient_header"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(
                            model = thread.user.avatarUrl,
                            contentDescription = thread.user.fullName,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        if (thread.isOnline) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = thread.user.fullName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (thread.user.isVerified) {
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
                            text = if (thread.isTyping) "Typing..." else thread.lastSeenText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = if (thread.isTyping) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (thread.isTyping) {
                                IgBlue
                            } else if (thread.isOnline) {
                                Color(0xFF2E7D32)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("chat_online_status")
                        )
                    }
                }

                IconButton(
                    onClick = { onOpenUserProfile(thread.user) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("chat_profile_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "View profile",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            thickness = 0.5.dp
        )

        // Message Thread List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (thread.messages.isEmpty() && !thread.isTyping) {
                EmptyChatConversationPrompt(
                    user = thread.user,
                    onSendQuickGreeting = { greeting -> onSendTextMessage(greeting) },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("chat_messages_list"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(thread.messages, key = { it.id }) { msg ->
                        ChatMessageBubbleRow(
                            message = msg,
                            recipientAvatarUrl = thread.user.avatarUrl,
                            currentUserId = currentUser.id,
                            isLatestOutgoing = msg.id == lastOutgoingMessageId,
                            showReactionPicker = activeReactionTargetId == msg.id,
                            onOpenReactionPicker = {
                                activeReactionTargetId =
                                    if (activeReactionTargetId == msg.id) null else msg.id
                            },
                            onDismissReactionPicker = { activeReactionTargetId = null },
                            onSelectReaction = { emoji ->
                                onToggleReaction(msg.id, emoji)
                                activeReactionTargetId = null
                            },
                            onMediaClick = { previewMediaMessage = msg }
                        )
                    }

                    if (thread.isTyping) {
                        item(key = "typing_indicator") {
                            TypingIndicatorRow(avatarUrl = thread.user.avatarUrl)
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            thickness = 0.5.dp
        )

        // Bottom Message Composer Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Direct Photo/Video Picker button (Zero-permission Android Photo Picker)
            IconButton(
                onClick = {
                    visualMediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(IgBlue)
                    .testTag("chat_photo_picker_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Share photo or video",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Text Input
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = {
                    Text(
                        text = "Message...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("chat_message_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IgBlue.copy(alpha = 0.6f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ),
                maxLines = 4
            )

            // Quick Gallery & Sample Media Sheet Button
            IconButton(
                onClick = { showMediaPickerSheet = true },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("chat_media_gallery_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = "Open media tray",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            if (messageInput.isNotBlank()) {
                IconButton(
                    onClick = {
                        onSendTextMessage(messageInput)
                        messageInput = ""
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(IgBlue)
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = { onSendTextMessage("❤️") },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("chat_quick_heart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Send heart",
                        tint = IgRed
                    )
                }
            }
        }
    }

    // Bottom Sheet: Share Photo or Video (Device Photo Picker + Curated Media Tray)
    if (showMediaPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMediaPickerSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Share Photo or Video",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pick from your device gallery or share a quick sample photo/video.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        visualMediaPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("chat_device_photo_picker_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IgBlue)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose from Device Gallery", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Quick Media Library",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(SampleData.sampleChatMedia, key = { it.id }) { item ->
                        Column(
                            modifier = Modifier
                                .width(116.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSendMediaMessage(
                                        item.mediaUri,
                                        item.mediaType,
                                        item.thumbnailUrl,
                                        ""
                                    )
                                    showMediaPickerSheet = false
                                }
                                .testTag("quick_media_${item.id}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(116.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = item.thumbnailUrl,
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (item.mediaType == ChatMediaType.VIDEO) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Videocam,
                                                contentDescription = "Video",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "VIDEO",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // Full-screen Media Viewer Dialog (Image or Media3 ExoPlayer Video)
    previewMediaMessage?.let { mediaMsg ->
        ChatMediaViewerDialog(
            message = mediaMsg,
            onDismiss = { previewMediaMessage = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageBubbleRow(
    message: ChatMessage,
    recipientAvatarUrl: String,
    currentUserId: String,
    isLatestOutgoing: Boolean,
    showReactionPicker: Boolean,
    onOpenReactionPicker: () -> Unit,
    onDismissReactionPicker: () -> Unit,
    onSelectReaction: (String) -> Unit,
    onMediaClick: () -> Unit
) {
    val isMe = message.isFromMe
    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }
    val bubbleColor = if (isMe) IgBlue else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_message_${message.id}"),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // Emoji Reaction Picker Tray
        AnimatedVisibility(
            visible = showReactionPicker,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val myCurrentReaction = message.reactions[currentUserId]
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .testTag("reaction_picker_tray")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QUICK_REACTION_EMOJIS.forEach { emoji ->
                        val isSelected = myCurrentReaction == emoji
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) IgBlue.copy(alpha = 0.18f) else Color.Transparent
                                )
                                .clickable { onSelectReaction(emoji) }
                                .testTag("react_emoji_$emoji"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    }
                    IconButton(
                        onClick = onDismissReactionPicker,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close reactions",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isMe) {
                AsyncImage(
                    model = recipientAvatarUrl,
                    contentDescription = "Sender avatar",
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                IconButton(
                    onClick = onOpenReactionPicker,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("react_trigger_${message.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddReaction,
                        contentDescription = "React to message",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Column(
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = 276.dp)
            ) {
                Surface(
                    shape = bubbleShape,
                    color = bubbleColor,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            if (message.mediaUri != null) onMediaClick() else onOpenReactionPicker()
                        },
                        onLongClick = onOpenReactionPicker
                    )
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        if (!message.mediaUri.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 180.dp, max = 250.dp)
                                    .height(175.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Black.copy(alpha = 0.15f))
                                    .clickable { onMediaClick() }
                                    .testTag("chat_media_preview_${message.id}")
                            ) {
                                AsyncImage(
                                    model = message.thumbnailUrl ?: message.mediaUri,
                                    contentDescription = if (message.mediaType == ChatMediaType.VIDEO) "Shared video" else "Shared photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (message.mediaType == ChatMediaType.VIDEO) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.65f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play video",
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Video",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        if (message.text.isNotBlank()) {
                            Text(
                                text = message.text,
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.5.sp,
                                    lineHeight = 19.sp
                                ),
                                modifier = Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = if (message.mediaUri != null) 7.dp else 7.dp
                                )
                            )
                        }
                    }
                }

                // Reaction Badges
                if (message.reactions.isNotEmpty()) {
                    val grouped = message.reactions.values.groupingBy { it }.eachCount()
                    Row(
                        modifier = Modifier
                            .offset(y = (-6).dp)
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        grouped.forEach { (emoji, count) ->
                            val reactedByMe = message.reactions[currentUserId] == emoji
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .border(
                                        width = if (reactedByMe) 1.dp else 0.5.dp,
                                        color = if (reactedByMe) IgBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectReaction(emoji) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = emoji, fontSize = 12.sp)
                                    if (count > 1) {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "$count",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isMe) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onOpenReactionPicker,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("react_trigger_${message.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddReaction,
                        contentDescription = "React to message",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Timestamp + Read Receipt Indicator
        Row(
            modifier = Modifier.padding(
                start = if (isMe) 0.dp else 36.dp,
                end = if (isMe) 4.dp else 0.dp,
                top = if (message.reactions.isNotEmpty()) 0.dp else 3.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = message.timestamp,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            if (isMe) {
                val (receiptIcon, receiptTint, receiptLabel) = when (message.status) {
                    MessageDeliveryStatus.SENT -> Triple(
                        Icons.Default.Done,
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        "Sent"
                    )
                    MessageDeliveryStatus.DELIVERED -> Triple(
                        Icons.Default.DoneAll,
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        "Delivered"
                    )
                    MessageDeliveryStatus.SEEN -> Triple(
                        Icons.Default.DoneAll,
                        IgBlue,
                        "Seen"
                    )
                }
                Icon(
                    imageVector = receiptIcon,
                    contentDescription = receiptLabel,
                    tint = receiptTint,
                    modifier = Modifier.size(14.dp)
                )
                if (isLatestOutgoing) {
                    Text(
                        text = receiptLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = if (message.status == MessageDeliveryStatus.SEEN) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = receiptTint,
                        modifier = Modifier.testTag("latest_read_receipt")
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingIndicatorRow(avatarUrl: String) {
    val transition = rememberInfiniteTransition(label = "typing")
    val dot1Alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "d1"
    )
    val dot2Alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500, delayMillis = 150), RepeatMode.Reverse),
        label = "d2"
    )
    val dot3Alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500, delayMillis = 300), RepeatMode.Reverse),
        label = "d3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.testTag("chat_typing_indicator")
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "Typing avatar",
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(dot1Alpha, dot2Alpha, dot3Alpha).forEach { alpha ->
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .alpha(alpha)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChatConversationPrompt(
    user: User,
    onSendQuickGreeting: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = user.fullName,
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = user.fullName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "@${user.username} • InstaSnap",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Hey 👋", "Love your work! 🔥").forEach { greeting ->
                SuggestionChip(
                    onClick = { onSendQuickGreeting(greeting) },
                    label = { Text(greeting) }
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ChatMediaViewerDialog(
    message: ChatMessage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val uriString = message.mediaUri ?: return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            if (message.mediaType == ChatMediaType.VIDEO && !isInPreview) {
                var player by remember(uriString) { mutableStateOf<ExoPlayer?>(null) }
                DisposableEffect(uriString) {
                    val exo = runCatching {
                        ExoPlayer.Builder(context).build().apply {
                            setMediaItem(MediaItem.fromUri(Uri.parse(uriString)))
                            repeatMode = Player.REPEAT_MODE_ONE
                            playWhenReady = true
                            prepare()
                        }
                    }.getOrNull()
                    player = exo
                    onDispose {
                        player = null
                        exo?.release()
                    }
                }

                if (player != null) {
                    ContentFrame(
                        player = player,
                        surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center)
                    )
                } else {
                    AsyncImage(
                        model = message.thumbnailUrl ?: uriString,
                        contentDescription = "Shared video preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                AsyncImage(
                    model = uriString,
                    contentDescription = "Shared image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close media viewer",
                    tint = Color.White
                )
            }
        }
    }
}
