package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
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
import com.example.model.ChatMediaType
import com.example.model.DirectMessage
import com.example.model.MessageDeliveryStatus
import com.example.model.User
import com.example.ui.theme.IgBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectMessagesScreen(
    currentUser: User,
    messages: List<DirectMessage>,
    activeThread: DirectMessage?,
    onOpenChat: (String) -> Unit,
    onCloseChat: () -> Unit,
    onStartNewChat: (User) -> Unit,
    onSendTextMessage: (conversationId: String, text: String) -> Unit,
    onSendMediaMessage: (
        conversationId: String,
        mediaUri: String,
        mediaType: ChatMediaType,
        thumbnailUrl: String?,
        caption: String
    ) -> Unit,
    onToggleReaction: (conversationId: String, messageId: String, emoji: String) -> Unit,
    onOpenUserProfile: (User) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showNewMessageSheet by remember { mutableStateOf(false) }
    var openMediaForChatId by remember { mutableStateOf<String?>(null) }

    BackHandler {
        if (activeThread != null) {
            onCloseChat()
        } else {
            onBack()
        }
    }

    val filteredConversations = remember(messages, searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            messages
        } else {
            messages.filter { dm ->
                dm.user.fullName.lowercase().contains(query) ||
                    dm.user.username.lowercase().contains(query) ||
                    dm.lastMessage.lowercase().contains(query) ||
                    dm.messages.any { it.text.lowercase().contains(query) }
            }
        }
    }

    val activeUsers = remember(messages) {
        val onlineFromThreads = messages.filter { it.isOnline }.map { it.user }
        (onlineFromThreads + SampleData.sampleUsers).distinctBy { it.id }.take(5)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("direct_messages_screen")
    ) {
        val isWideScreen = maxWidth >= 720.dp

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                ConversationListPane(
                    currentUser = currentUser,
                    conversations = filteredConversations,
                    totalConversationsCount = messages.size,
                    activeUsers = activeUsers,
                    selectedChatId = activeThread?.id,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onBack = onBack,
                    onNewMessageClick = { showNewMessageSheet = true },
                    onActiveUserClick = onStartNewChat,
                    onConversationClick = { dm -> onOpenChat(dm.id) },
                    onQuickCameraClick = { dm ->
                        openMediaForChatId = dm.id
                        onOpenChat(dm.id)
                    },
                    modifier = Modifier
                        .width(350.dp)
                        .fillMaxHeight()
                )

                VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (activeThread != null) {
                        ChatDetailScreen(
                            thread = activeThread,
                            currentUser = currentUser,
                            showBackButton = false,
                            openMediaSheetInitially = openMediaForChatId == activeThread.id,
                            onMediaSheetOpened = { openMediaForChatId = null },
                            onBack = onCloseChat,
                            onOpenUserProfile = onOpenUserProfile,
                            onSendTextMessage = { text -> onSendTextMessage(activeThread.id, text) },
                            onSendMediaMessage = { uri, type, thumb, caption ->
                                onSendMediaMessage(activeThread.id, uri, type, thumb, caption)
                            },
                            onToggleReaction = { msgId, emoji ->
                                onToggleReaction(activeThread.id, msgId, emoji)
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, MaterialTheme.colorScheme.onBackground, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(34.dp),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Your Messages",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Select a conversation or start a new chat with a creator.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showNewMessageSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = IgBlue)
                            ) {
                                Text("Send Message")
                            }
                        }
                    }
                }
            }
        } else {
            AnimatedContent(
                targetState = activeThread,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 3 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "dm_screen_transition"
            ) { thread ->
                if (thread != null) {
                    ChatDetailScreen(
                        thread = thread,
                        currentUser = currentUser,
                        showBackButton = true,
                        openMediaSheetInitially = openMediaForChatId == thread.id,
                        onMediaSheetOpened = { openMediaForChatId = null },
                        onBack = onCloseChat,
                        onOpenUserProfile = onOpenUserProfile,
                        onSendTextMessage = { text -> onSendTextMessage(thread.id, text) },
                        onSendMediaMessage = { uri, type, thumb, caption ->
                            onSendMediaMessage(thread.id, uri, type, thumb, caption)
                        },
                        onToggleReaction = { msgId, emoji ->
                            onToggleReaction(thread.id, msgId, emoji)
                        }
                    )
                } else {
                    ConversationListPane(
                        currentUser = currentUser,
                        conversations = filteredConversations,
                        totalConversationsCount = messages.size,
                        activeUsers = activeUsers,
                        selectedChatId = null,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onBack = onBack,
                        onNewMessageClick = { showNewMessageSheet = true },
                        onActiveUserClick = onStartNewChat,
                        onConversationClick = { dm -> onOpenChat(dm.id) },
                        onQuickCameraClick = { dm ->
                            openMediaForChatId = dm.id
                            onOpenChat(dm.id)
                        }
                    )
                }
            }
        }
    }

    if (showNewMessageSheet) {
        NewMessagePickerSheet(
            onDismiss = { showNewMessageSheet = false },
            onSelectUser = { user ->
                showNewMessageSheet = false
                onStartNewChat(user)
            }
        )
    }
}

@Composable
private fun ConversationListPane(
    currentUser: User,
    conversations: List<DirectMessage>,
    totalConversationsCount: Int,
    activeUsers: List<User>,
    selectedChatId: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onNewMessageClick: () -> Unit,
    onActiveUserClick: (User) -> Unit,
    onConversationClick: (DirectMessage) -> Unit,
    onQuickCameraClick: (DirectMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("dm_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = currentUser.username,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
            }

            IconButton(
                onClick = onNewMessageClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("dm_new_message_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "New Message",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Search Conversations Bar
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search conversations...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("dm_search_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IgBlue,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                singleLine = true
            )
        }

        // Active Now Horizontal Row
        if (searchQuery.isBlank() && activeUsers.isNotEmpty()) {
            Text(
                text = "Active Now",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(activeUsers, key = { it.id }) { user ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onActiveUserClick(user) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("active_user_${user.username}")
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = user.username,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                                    .border(2.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = user.username,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Messages",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (totalConversationsCount > 0) {
                Text(
                    text = "${conversations.size} chats",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            thickness = 0.5.dp
        )

        if (conversations.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "No matching conversations" else "No messages yet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (searchQuery.isNotBlank()) {
                        "Try another name or start a new conversation."
                    } else {
                        "Tap the compose icon to start chatting."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(onClick = onNewMessageClick) {
                    Text("Start New Chat")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("dm_conversations_list"),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                items(conversations, key = { it.id }) { dm ->
                    val isSelected = dm.id == selectedChatId
                    val lastMsg = dm.messages.lastOrNull()
                    val isOutgoingLast = lastMsg?.isFromMe == true

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                else Color.Transparent
                            )
                            .clickable { onConversationClick(dm) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("dm_row_${dm.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(
                                model = dm.user.avatarUrl,
                                contentDescription = dm.user.username,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            if (dm.isOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dm.user.fullName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (dm.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (dm.user.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = IgBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isOutgoingLast && !dm.isTyping) {
                                    val (receiptIcon, receiptTint) = when (lastMsg?.status) {
                                        MessageDeliveryStatus.SENT -> Icons.Default.Done to MaterialTheme.colorScheme.onSurfaceVariant
                                        MessageDeliveryStatus.DELIVERED -> Icons.Default.DoneAll to MaterialTheme.colorScheme.onSurfaceVariant
                                        MessageDeliveryStatus.SEEN -> Icons.Default.DoneAll to IgBlue
                                        null -> Icons.Default.Done to MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Icon(
                                        imageVector = receiptIcon,
                                        contentDescription = "Delivery status",
                                        tint = receiptTint,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                Text(
                                    text = if (dm.isTyping) "Typing..." else dm.lastMessage,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (dm.unreadCount > 0 || dm.isTyping) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            dm.isTyping -> IgBlue
                                            dm.unreadCount > 0 -> MaterialTheme.colorScheme.onBackground
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Text(
                                    text = " · ${dm.timestamp}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (dm.unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                                    .clip(CircleShape)
                                    .background(IgBlue)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${dm.unreadCount}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { onQuickCameraClick(dm) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dm_quick_camera_${dm.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Quick photo or video to ${dm.user.username}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewMessagePickerSheet(
    onDismiss: () -> Unit,
    onSelectUser: (User) -> Unit
) {
    var userSearch by remember { mutableStateOf("") }
    val users = remember(userSearch) {
        val q = userSearch.trim().lowercase()
        if (q.isEmpty()) SampleData.sampleUsers
        else SampleData.sampleUsers.filter {
            it.fullName.lowercase().contains(q) || it.username.lowercase().contains(q)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("new_message_sheet")
        ) {
            Text(
                text = "New Message",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = userSearch,
                onValueChange = { userSearch = it },
                placeholder = { Text("Search people...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search creators"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("new_message_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(users, key = { it.id }) { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectUser(user) }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                            .testTag("new_chat_user_${user.username}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = user.username,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.fullName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (user.isVerified) {
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
                                text = "@${user.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Chat",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = IgBlue
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
