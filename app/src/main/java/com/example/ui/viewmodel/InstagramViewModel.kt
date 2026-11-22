package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SampleData
import com.example.firebase.AiAssistantDataSource
import com.example.firebase.AiCaptionResult
import com.example.firebase.AiStreamChunk
import com.example.firebase.FirestorePostState
import com.example.firebase.FirestoreUserComment
import com.example.firebase.FirestoreUserPost
import com.example.firebase.FirestoreUserProfile
import com.example.firebase.SocialDataSource
import com.example.model.*
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "InstagramVM"
private const val STOP_TIMEOUT_MILLIS = 5000L

sealed interface AiCaptionUiState {
    data object Idle : AiCaptionUiState
    data object Loading : AiCaptionUiState
    data class Success(val result: AiCaptionResult) : AiCaptionUiState
    data class Error(val message: String) : AiCaptionUiState
}

sealed interface AiCompanionUiState {
    data object Idle : AiCompanionUiState
    data object Loading : AiCompanionUiState
    data class Streaming(val text: String) : AiCompanionUiState
    data class Success(val text: String) : AiCompanionUiState
    data class Error(val message: String) : AiCompanionUiState
}

sealed interface FirestoreSyncState {
    data object Loading : FirestoreSyncState
    data object Ready : FirestoreSyncState
    data class Error(val message: String) : FirestoreSyncState
}

class InstagramViewModel(
    private val repository: SocialDataSource,
    private val aiService: AiAssistantDataSource,
    private val currentUserId: String,
    private val initialDisplayName: String? = null,
    private val initialPhotoUrl: String? = null
) : ViewModel() {

    private val _recycledFeedPosts = MutableStateFlow<List<Post>>(emptyList())
    private val _followedUsersOverride = MutableStateFlow(
        SampleData.sampleUsers.associate { it.id to it.isFollowing }
    )
    val followedUsers: StateFlow<Map<String, Boolean>> = _followedUsersOverride.asStateFlow()
    private val _commentLikesOverride = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    private val _isRefreshingFeed = MutableStateFlow(false)
    val isRefreshingFeed: StateFlow<Boolean> = _isRefreshingFeed.asStateFlow()
    private val _manualRefreshedUserPosts = MutableStateFlow<List<FirestoreUserPost>?>(null)

    private val _syncState = MutableStateFlow<FirestoreSyncState>(FirestoreSyncState.Loading)
    val syncState: StateFlow<FirestoreSyncState> = _syncState.asStateFlow()

    val currentUser: StateFlow<User> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing user profile", rootError)
            _syncState.value = FirestoreSyncState.Error(rootError.message ?: "Failed to load profile")
            emit(null)
        }
        .combine(MutableStateFlow(Unit)) { profile, _ ->
            mapProfileToUser(profile)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SampleData.currentUser.copy(
                id = currentUserId,
                fullName = initialDisplayName?.ifBlank { null } ?: SampleData.currentUser.fullName,
                avatarUrl = initialPhotoUrl?.ifBlank { null } ?: SampleData.currentUser.avatarUrl
            )
        )

    private val firestoreUserPosts: StateFlow<List<FirestoreUserPost>> = repository.observeUserPosts(currentUserId)
        .onEach { _manualRefreshedUserPosts.value = null }
        .catch { error ->
            handleStreamException("user posts", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    private val effectiveUserPosts: Flow<List<FirestoreUserPost>> = combine(
        firestoreUserPosts,
        _manualRefreshedUserPosts
    ) { livePosts, refreshedPosts ->
        refreshedPosts ?: livePosts
    }

    private val firestorePostStates: StateFlow<List<FirestorePostState>> = repository.observePostStates(currentUserId)
        .catch { error ->
            handleStreamException("post states", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    private val firestoreUserComments: StateFlow<List<FirestoreUserComment>> = repository.observeUserComments(currentUserId)
        .catch { error ->
            handleStreamException("user comments", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val posts: StateFlow<List<Post>> = combine(
        currentUser,
        effectiveUserPosts,
        firestorePostStates,
        firestoreUserComments,
        _recycledFeedPosts
    ) { user, userPosts, postStates, userComments, recycledPosts ->
        val stateByPostId = postStates.associateBy { it.postId }
        val commentsByPostId = userComments.groupBy { it.postId }

        val mappedUserPosts = userPosts.map { fp ->
            val extraComments = commentsByPostId[fp.id].orEmpty().map { fc ->
                Comment(
                    id = fc.id,
                    username = fc.authorUsername,
                    userAvatar = fc.authorAvatarUrl,
                    text = fc.text,
                    timestamp = "Saved",
                    likesCount = fc.likesCount.toInt(),
                    isLiked = fc.liked
                )
            }
            Post(
                id = fp.id,
                user = user.copy(
                    username = fp.authorUsername.ifBlank { user.username },
                    avatarUrl = fp.authorAvatarUrl.ifBlank { user.avatarUrl }
                ),
                location = fp.location,
                musicTrack = null,
                mediaUrls = listOf(fp.imageUrl),
                isVideo = false,
                likesCount = fp.likesCount.toInt(),
                isLiked = fp.liked,
                isSaved = fp.saved,
                caption = fp.caption,
                hashtags = fp.hashtags,
                commentsCount = maxOf(fp.commentsCount.toInt(), extraComments.size),
                comments = extraComments,
                timestamp = "Cloud Post",
                aspectRatio = 1.0f
            )
        }

        val curatedFeed = (SampleData.samplePosts + recycledPosts).map { post ->
            val interaction = stateByPostId[post.id]
            val extraComments = commentsByPostId[post.id].orEmpty().map { fc ->
                Comment(
                    id = fc.id,
                    username = fc.authorUsername,
                    userAvatar = fc.authorAvatarUrl,
                    text = fc.text,
                    timestamp = "Just now",
                    likesCount = fc.likesCount.toInt(),
                    isLiked = fc.liked
                )
            }
            val mergedLiked = interaction?.liked ?: post.isLiked
            val deltaLikes = when {
                interaction == null -> 0
                interaction.liked && !post.isLiked -> 1
                !interaction.liked && post.isLiked -> -1
                else -> 0
            }
            post.copy(
                isLiked = mergedLiked,
                likesCount = maxOf(0, post.likesCount + deltaLikes),
                isSaved = interaction?.saved ?: post.isSaved,
                comments = extraComments + post.comments,
                commentsCount = post.commentsCount + extraComments.size
            )
        }

        mappedUserPosts + curatedFeed
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SampleData.samplePosts
    )

    private val _stories = MutableStateFlow<List<Story>>(SampleData.sampleStories)
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    private val _reels = MutableStateFlow<List<Reel>>(SampleData.sampleReels)
    val reels: StateFlow<List<Reel>> = _reels.asStateFlow()

    private val _isReelMuted = MutableStateFlow(false)
    val isReelMuted: StateFlow<Boolean> = _isReelMuted.asStateFlow()

    private val _currentTab = MutableStateFlow(NavigationTab.HOME)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    private val _viewedProfileUser = MutableStateFlow<User?>(null)
    val viewedProfileUser: StateFlow<User?> = _viewedProfileUser.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _activeStory = MutableStateFlow<Story?>(null)
    val activeStory: StateFlow<Story?> = _activeStory.asStateFlow()

    private val _activeCommentsPostId = MutableStateFlow<String?>(null)
    val activeCommentsPost: StateFlow<Post?> = combine(posts, _activeCommentsPostId) { list, id ->
        id?.let { targetId -> list.firstOrNull { it.id == targetId } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private val _activeSharePost = MutableStateFlow<Post?>(null)
    val activeSharePost: StateFlow<Post?> = _activeSharePost.asStateFlow()

    private val _activeOptionsPost = MutableStateFlow<Post?>(null)
    val activeOptionsPost: StateFlow<Post?> = _activeOptionsPost.asStateFlow()

    private val _activeAiCompanionPost = MutableStateFlow<Post?>(null)
    val activeAiCompanionPost: StateFlow<Post?> = _activeAiCompanionPost.asStateFlow()

    private val _aiCaptionState = MutableStateFlow<AiCaptionUiState>(AiCaptionUiState.Idle)
    val aiCaptionState: StateFlow<AiCaptionUiState> = _aiCaptionState.asStateFlow()

    private val _aiCompanionState = MutableStateFlow<AiCompanionUiState>(AiCompanionUiState.Idle)
    val aiCompanionState: StateFlow<AiCompanionUiState> = _aiCompanionState.asStateFlow()

    private var aiCompanionJob: Job? = null

    private val _showDirectMessages = MutableStateFlow(false)
    val showDirectMessages: StateFlow<Boolean> = _showDirectMessages.asStateFlow()

    private val _showNotifications = MutableStateFlow(false)
    val showNotifications: StateFlow<Boolean> = _showNotifications.asStateFlow()

    private val _showEditProfileDialog = MutableStateFlow(false)
    val showEditProfileDialog: StateFlow<Boolean> = _showEditProfileDialog.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(SampleData.sampleNotifications)
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    val unreadNotificationsCount: StateFlow<Int> = _notifications
        .combine(MutableStateFlow(Unit)) { list, _ -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SampleData.sampleNotifications.count { !it.isRead })

    private val _activeActivityAlert = MutableStateFlow<NotificationItem?>(null)
    val activeActivityAlert: StateFlow<NotificationItem?> = _activeActivityAlert.asStateFlow()

    private var activitySimulationCounter = 0

    private val _directMessages = MutableStateFlow<List<DirectMessage>>(SampleData.sampleDirectMessages)
    val directMessages: StateFlow<List<DirectMessage>> = _directMessages.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    val activeChatThread: StateFlow<DirectMessage?> = combine(_directMessages, _activeChatId) { list, id ->
        id?.let { targetId -> list.firstOrNull { it.id == targetId } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private val chatSimulationJobs = mutableMapOf<String, Job>()

    private val _isLoadingMorePosts = MutableStateFlow(false)
    val isLoadingMorePosts: StateFlow<Boolean> = _isLoadingMorePosts.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            val result = repository.ensureUserProfile(
                userId = currentUserId,
                displayName = initialDisplayName,
                photoUrl = initialPhotoUrl
            )
            _syncState.value = if (result.isSuccess) {
                FirestoreSyncState.Ready
            } else {
                FirestoreSyncState.Error(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to sync user profile"
                )
            }
        }
    }

    private fun handleStreamException(label: String, error: Throwable) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $label", rootError)
        _syncState.value = FirestoreSyncState.Error(rootError.message ?: "Failed to observe $label")
    }

    private fun mapProfileToUser(profile: FirestoreUserProfile?): User {
        if (profile == null) {
            return SampleData.currentUser.copy(
                id = currentUserId,
                fullName = initialDisplayName?.ifBlank { null } ?: SampleData.currentUser.fullName,
                avatarUrl = initialPhotoUrl?.ifBlank { null } ?: SampleData.currentUser.avatarUrl
            )
        }
        return User(
            id = profile.userId.ifBlank { currentUserId },
            username = profile.username.ifBlank { SampleData.currentUser.username },
            fullName = profile.fullName.ifBlank { SampleData.currentUser.fullName },
            avatarUrl = profile.avatarUrl.ifBlank { SampleData.currentUser.avatarUrl },
            isVerified = profile.verified,
            bio = profile.bio,
            link = profile.link,
            postsCount = profile.postsCount.toInt(),
            followersCount = profile.followersCount.ifBlank { "18.4K" },
            followingCount = profile.followingCount.ifBlank { "482" },
            mutualFollowersSummary = SampleData.currentUser.mutualFollowersSummary
        )
    }

    private fun isUserAuthoredPost(postId: String): Boolean {
        return firestoreUserPosts.value.any { it.id == postId }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun toggleDarkMode() {
        _isDarkMode.update { !it }
    }

    fun selectTab(tab: NavigationTab) {
        if (tab == NavigationTab.PROFILE) {
            _viewedProfileUser.value = null
        }
        _currentTab.value = tab
    }

    fun openUserProfile(user: User) {
        _viewedProfileUser.value = if (user.id == currentUser.value.id || user.username == currentUser.value.username) {
            null
        } else {
            user
        }
        _currentTab.value = NavigationTab.PROFILE
    }

    fun closeViewedUserProfile() {
        _viewedProfileUser.value = null
    }

    fun messageUser(user: User) {
        startNewConversation(user)
    }

    fun startNewConversation(user: User) {
        var resolvedId = "dm_${user.id}"
        _directMessages.update { list ->
            val existing = list.indexOfFirst { it.user.id == user.id || it.user.username == user.username }
            if (existing >= 0) {
                val thread = list[existing].copy(
                    unreadCount = 0,
                    messages = list[existing].messages.map { msg ->
                        if (!msg.isFromMe) msg.copy(status = MessageDeliveryStatus.SEEN) else msg
                    }
                )
                resolvedId = thread.id
                listOf(thread) + list.filterIndexed { i, _ -> i != existing }
            } else {
                listOf(
                    DirectMessage(
                        id = resolvedId,
                        user = user,
                        lastMessage = "Say hi to @${user.username} 👋",
                        timestamp = "Now",
                        unreadCount = 0,
                        isOnline = true,
                        lastSeenText = "Active now",
                        messages = emptyList()
                    )
                ) + list
            }
        }
        _activeChatId.value = resolvedId
        _showDirectMessages.value = true
    }

    fun openChat(conversationId: String) {
        _activeChatId.value = conversationId
        _directMessages.update { list ->
            list.map { dm ->
                if (dm.id == conversationId) {
                    dm.copy(
                        unreadCount = 0,
                        messages = dm.messages.map { msg ->
                            if (!msg.isFromMe) msg.copy(status = MessageDeliveryStatus.SEEN) else msg
                        }
                    )
                } else dm
            }
        }
    }

    fun closeChat() {
        _activeChatId.value = null
    }

    fun shareProfile(user: User) {
        _snackbarMessage.value = "Profile link copied for @${user.username}"
    }

    fun setShowDirectMessages(show: Boolean) {
        _showDirectMessages.value = show
        if (!show) {
            _activeChatId.value = null
        }
    }

    fun setShowNotifications(show: Boolean) {
        _showNotifications.value = show
        if (show) {
            markNotificationsAsRead()
            _activeActivityAlert.value = null
        }
    }

    fun markNotificationsAsRead() {
        _notifications.update { list ->
            list.map { if (it.isRead) it else it.copy(isRead = true) }
        }
    }

    fun dismissActivityAlert() {
        _activeActivityAlert.value = null
    }

    fun recordPostLikeActivity(
        actor: User,
        postId: String? = null,
        postThumbnailUrl: String? = null
    ) {
        val newItem = NotificationItem(
            id = "notif_like_${UUID.randomUUID()}",
            user = actor,
            type = NotificationType.LIKE,
            text = "liked your post.",
            postThumbnailUrl = postThumbnailUrl,
            postId = postId,
            timestamp = "Just now",
            isFollowingBack = _followedUsersOverride.value[actor.id] ?: actor.isFollowing,
            isRead = false
        )
        _notifications.update { list -> listOf(newItem) + list }
        _activeActivityAlert.value = newItem
    }

    fun recordFollowActivity(actor: User) {
        val newItem = NotificationItem(
            id = "notif_follow_${UUID.randomUUID()}",
            user = actor,
            type = NotificationType.FOLLOW,
            text = "started following you.",
            timestamp = "Just now",
            isFollowingBack = _followedUsersOverride.value[actor.id] ?: actor.isFollowing,
            isRead = false
        )
        _notifications.update { list -> listOf(newItem) + list }
        _activeActivityAlert.value = newItem
    }

    fun simulateLocalActivity() {
        val creators = SampleData.sampleUsers
        if (creators.isEmpty()) return
        val actor = creators[activitySimulationCounter % creators.size]
        val isLikeEvent = activitySimulationCounter % 2 == 0
        activitySimulationCounter++
        if (isLikeEvent) {
            val targetPost = posts.value.firstOrNull()
            recordPostLikeActivity(
                actor = actor,
                postId = targetPost?.id,
                postThumbnailUrl = targetPost?.mediaUrls?.firstOrNull()
            )
        } else {
            recordFollowActivity(actor = actor)
        }
    }

    fun setShowEditProfileDialog(show: Boolean) {
        _showEditProfileDialog.value = show
    }

    fun updateProfile(username: String, fullName: String, bio: String, link: String) {
        viewModelScope.launch {
            val result = repository.updateProfile(
                userId = currentUserId,
                username = username,
                fullName = fullName,
                bio = bio,
                link = link
            )
            if (result.isSuccess) {
                _showEditProfileDialog.value = false
                _snackbarMessage.value = "Profile updated in Firestore"
            } else {
                _snackbarMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to update profile"
            }
        }
    }

    fun openStory(story: Story) {
        val resolvedStory = if (story.isYourStory) {
            story.copy(userAvatar = currentUser.value.avatarUrl)
        } else story
        _activeStory.value = resolvedStory.copy(hasUnseenStory = false)
        _stories.update { list ->
            list.map {
                if (it.id == story.id) resolvedStory.copy(hasUnseenStory = false) else it
            }
        }
    }

    fun closeStory() {
        _activeStory.value = null
    }

    fun advanceStory() {
        val current = _activeStory.value ?: return
        val list = _stories.value
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex < list.size - 1) {
            openStory(list[currentIndex + 1])
        } else {
            closeStory()
        }
    }

    fun previousStory() {
        val current = _activeStory.value ?: return
        val list = _stories.value
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            openStory(list[currentIndex - 1])
        }
    }

    fun toggleLikeStory(storyId: String) {
        _stories.update { list ->
            list.map { story ->
                if (story.id == storyId) story.copy(isLiked = !story.isLiked) else story
            }
        }
        _activeStory.update { current ->
            if (current?.id == storyId) current.copy(isLiked = !current.isLiked) else current
        }
    }

    fun reactToStory(story: Story, emoji: String) {
        _stories.update { list ->
            list.map { item ->
                if (item.id == story.id) item.copy(lastReaction = emoji) else item
            }
        }
        _activeStory.update { current ->
            if (current?.id == story.id) current.copy(lastReaction = emoji) else current
        }
        appendStoryDirectMessage(story, "Reacted $emoji to your story")
    }

    fun replyToStory(story: Story, message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        appendStoryDirectMessage(story, "Replied to story: $trimmed")
        _snackbarMessage.value = "Reply sent to @${story.username}"
    }

    fun shareStory(story: Story) {
        _snackbarMessage.value = "Shared @${story.username}'s story"
    }

    private fun appendStoryDirectMessage(story: Story, messageText: String) {
        if (story.isYourStory) return
        val targetUser = SampleData.sampleUsers.firstOrNull { it.id == story.userId }
            ?: User(
                id = story.userId,
                username = story.username,
                fullName = story.username,
                avatarUrl = story.userAvatar
            )
        val chatMessage = ChatMessage(
            id = "msg_story_${UUID.randomUUID()}",
            senderId = currentUser.value.id,
            text = messageText,
            mediaUri = story.mediaUrls.firstOrNull(),
            mediaType = if (story.mediaUrls.isNotEmpty()) ChatMediaType.IMAGE else null,
            thumbnailUrl = story.mediaUrls.firstOrNull(),
            timestamp = "Just now",
            status = MessageDeliveryStatus.DELIVERED,
            isFromMe = true
        )
        _directMessages.update { list ->
            val existingIndex = list.indexOfFirst { it.user.id == story.userId || it.user.username == story.username }
            if (existingIndex >= 0) {
                val existing = list[existingIndex]
                val updated = existing.copy(
                    lastMessage = messageText,
                    timestamp = "Just now",
                    unreadCount = 0,
                    messages = existing.messages + chatMessage
                )
                listOf(updated) + list.filterIndexed { index, _ -> index != existingIndex }
            } else {
                listOf(
                    DirectMessage(
                        id = "dm_story_${story.userId}",
                        user = targetUser,
                        lastMessage = messageText,
                        timestamp = "Just now",
                        unreadCount = 0,
                        isOnline = true,
                        lastSeenText = "Active now",
                        messages = listOf(chatMessage)
                    )
                ) + list
            }
        }
    }

    fun sendChatMessage(
        conversationId: String,
        text: String = "",
        mediaUri: String? = null,
        mediaType: ChatMediaType? = null,
        thumbnailUrl: String? = null
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() && mediaUri.isNullOrBlank()) return

        val messageId = "msg_${UUID.randomUUID()}"
        val timestampLabel = formatCurrentChatTime()
        val newMessage = ChatMessage(
            id = messageId,
            senderId = currentUser.value.id,
            text = trimmed,
            mediaUri = mediaUri,
            mediaType = mediaType,
            thumbnailUrl = thumbnailUrl ?: mediaUri,
            timestamp = timestampLabel,
            status = MessageDeliveryStatus.SENT,
            isFromMe = true
        )

        val previewText = when {
            trimmed.isNotEmpty() -> "You: $trimmed"
            mediaType == ChatMediaType.VIDEO -> "You sent a video"
            mediaType == ChatMediaType.IMAGE -> "You sent a photo"
            else -> "You sent a message"
        }

        var targetThread: DirectMessage? = null
        _directMessages.update { list ->
            val index = list.indexOfFirst { it.id == conversationId }
            if (index < 0) return@update list
            val existing = list[index]
            val updated = existing.copy(
                lastMessage = previewText,
                timestamp = "Just now",
                unreadCount = 0,
                messages = existing.messages + newMessage
            )
            targetThread = updated
            listOf(updated) + list.filterIndexed { i, _ -> i != index }
        }

        val thread = targetThread ?: return
        chatSimulationJobs[conversationId]?.cancel()
        chatSimulationJobs[conversationId] = viewModelScope.launch {
            kotlinx.coroutines.delay(350)
            updateMessageDeliveryStatus(conversationId, messageId, MessageDeliveryStatus.DELIVERED)

            if (thread.isOnline) {
                kotlinx.coroutines.delay(450)
                updateMessageDeliveryStatus(conversationId, messageId, MessageDeliveryStatus.SEEN)

                if (trimmed.isNotEmpty() || mediaUri != null) {
                    setConversationTyping(conversationId, true)
                    kotlinx.coroutines.delay(950)
                    setConversationTyping(conversationId, false)

                    val replyText = buildContextualReply(thread.user, trimmed, mediaType)
                    val isCurrentlyViewing = _showDirectMessages.value && _activeChatId.value == conversationId
                    val replyMessage = ChatMessage(
                        id = "msg_reply_${UUID.randomUUID()}",
                        senderId = thread.user.id,
                        text = replyText,
                        timestamp = formatCurrentChatTime(),
                        status = if (isCurrentlyViewing) MessageDeliveryStatus.SEEN else MessageDeliveryStatus.DELIVERED,
                        isFromMe = false
                    )
                    _directMessages.update { list ->
                        val idx = list.indexOfFirst { it.id == conversationId }
                        if (idx < 0) return@update list
                        val dm = list[idx]
                        val updated = dm.copy(
                            lastMessage = replyText,
                            timestamp = "Just now",
                            isTyping = false,
                            unreadCount = if (isCurrentlyViewing) 0 else dm.unreadCount + 1,
                            messages = dm.messages + replyMessage
                        )
                        listOf(updated) + list.filterIndexed { i, _ -> i != idx }
                    }
                }
            }
        }
    }

    fun toggleMessageReaction(conversationId: String, messageId: String, emoji: String) {
        val myId = currentUser.value.id
        _directMessages.update { list ->
            list.map { dm ->
                if (dm.id != conversationId) dm
                else {
                    dm.copy(
                        messages = dm.messages.map { msg ->
                            if (msg.id != messageId) msg
                            else {
                                val updatedReactions = if (msg.reactions[myId] == emoji) {
                                    msg.reactions - myId
                                } else {
                                    msg.reactions + (myId to emoji)
                                }
                                msg.copy(reactions = updatedReactions)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun updateMessageDeliveryStatus(
        conversationId: String,
        messageId: String,
        status: MessageDeliveryStatus
    ) {
        _directMessages.update { list ->
            list.map { dm ->
                if (dm.id != conversationId) dm
                else dm.copy(
                    messages = dm.messages.map { msg ->
                        if (msg.id == messageId) msg.copy(status = status) else msg
                    }
                )
            }
        }
    }

    private fun setConversationTyping(conversationId: String, isTyping: Boolean) {
        _directMessages.update { list ->
            list.map { dm ->
                if (dm.id == conversationId) dm.copy(isTyping = isTyping) else dm
            }
        }
    }

    private fun buildContextualReply(
        sender: User,
        outgoingText: String,
        mediaType: ChatMediaType?
    ): String {
        val lower = outgoingText.lowercase()
        return when {
            mediaType == ChatMediaType.VIDEO -> "Love that clip! The motion and framing look super crisp 🎬✨"
            mediaType == ChatMediaType.IMAGE -> "Such a clean shot! The lighting really pops 📸🔥"
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Hey! Great to hear from you — working on anything fun today?"
            lower.contains("photowalk") || lower.contains("meet") || lower.contains("tomorrow") ->
                "Yes! Let's meet near the waterfront right around golden hour 🌇"
            lower.contains("?") ->
                "Definitely! Give me two minutes and I'll send over the full details."
            else ->
                "Totally agree with you 🙌 Let's collaborate on a shoot soon!"
        }
    }

    private fun formatCurrentChatTime(): String {
        val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        return formatter.format(java.util.Date())
    }

    fun toggleLikePost(postId: String) {
        val post = posts.value.firstOrNull { it.id == postId } ?: return
        val newLiked = !post.isLiked
        val newCount = maxOf(0, if (newLiked) post.likesCount + 1 else post.likesCount - 1)
        val isUserPost = isUserAuthoredPost(postId)

        viewModelScope.launch {
            val res = repository.savePostInteraction(
                userId = currentUserId,
                postId = postId,
                isUserPost = isUserPost,
                liked = newLiked,
                likesCount = newCount,
                saved = post.isSaved
            )
            if (res.isFailure) {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update like"
            }
        }
    }

    fun doubleTapLikePost(postId: String) {
        val post = posts.value.firstOrNull { it.id == postId } ?: return
        if (post.isLiked) return
        val newCount = post.likesCount + 1
        val isUserPost = isUserAuthoredPost(postId)

        viewModelScope.launch {
            val res = repository.savePostInteraction(
                userId = currentUserId,
                postId = postId,
                isUserPost = isUserPost,
                liked = true,
                likesCount = newCount,
                saved = post.isSaved
            )
            if (res.isFailure) {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to like post"
            }
        }
    }

    fun toggleSavePost(postId: String) {
        val post = posts.value.firstOrNull { it.id == postId } ?: return
        val newSaved = !post.isSaved
        val isUserPost = isUserAuthoredPost(postId)

        viewModelScope.launch {
            val res = repository.savePostInteraction(
                userId = currentUserId,
                postId = postId,
                isUserPost = isUserPost,
                liked = post.isLiked,
                likesCount = post.likesCount,
                saved = newSaved
            )
            if (res.isSuccess) {
                _snackbarMessage.value = if (newSaved) "Saved to cloud collection" else "Removed from saved"
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save post"
            }
        }
    }

    fun openComments(post: Post) {
        _activeCommentsPostId.value = post.id
    }

    fun closeComments() {
        _activeCommentsPostId.value = null
    }

    fun addComment(postId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val user = currentUser.value
        val isUserPost = isUserAuthoredPost(postId)

        viewModelScope.launch {
            val res = repository.addUserComment(
                userId = currentUserId,
                postId = postId,
                isUserPost = isUserPost,
                username = user.username,
                avatarUrl = user.avatarUrl,
                text = trimmed
            )
            if (res.isFailure) {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to post comment"
            }
        }
    }

    fun toggleLikeComment(postId: String, commentId: String) {
        _commentLikesOverride.update { current ->
            current + (commentId to !(current[commentId] ?: false))
        }
    }

    fun openShare(post: Post) {
        _activeSharePost.value = post
    }

    fun closeShare() {
        _activeSharePost.value = null
    }

    fun openOptions(post: Post) {
        _activeOptionsPost.value = post
    }

    fun closeOptions() {
        _activeOptionsPost.value = null
    }

    fun openAiCompanion(post: Post) {
        _activeAiCompanionPost.value = post
        streamAiCompanionForPost(post)
    }

    fun closeAiCompanion() {
        aiCompanionJob?.cancel()
        _activeAiCompanionPost.value = null
        _aiCompanionState.value = AiCompanionUiState.Idle
    }

    fun retryAiCompanion() {
        _activeAiCompanionPost.value?.let { streamAiCompanionForPost(it) }
    }

    private fun streamAiCompanionForPost(post: Post) {
        aiCompanionJob?.cancel()
        aiCompanionJob = viewModelScope.launch {
            _aiCompanionState.value = AiCompanionUiState.Loading
            try {
                aiService.streamPostCompanion(
                    authorUsername = post.user.username,
                    caption = post.caption,
                    location = post.location,
                    hashtags = post.hashtags
                ).collect { chunk ->
                    when (chunk) {
                        is AiStreamChunk.Streaming -> {
                            _aiCompanionState.value = AiCompanionUiState.Streaming(chunk.text)
                        }
                        is AiStreamChunk.Completed -> {
                            _aiCompanionState.value = AiCompanionUiState.Success(chunk.text)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Companion failed", e)
                _aiCompanionState.value = AiCompanionUiState.Error(
                    e.localizedMessage ?: "Failed to generate Gemini insights."
                )
            }
        }
    }

    fun generateAiCaptions(topicOrVibe: String, location: String?, filterName: String) {
        viewModelScope.launch {
            _aiCaptionState.value = AiCaptionUiState.Loading
            val res = aiService.generatePostCaptions(
                topicOrVibe = topicOrVibe,
                location = location,
                filterName = filterName
            )
            _aiCaptionState.value = res.fold(
                onSuccess = { AiCaptionUiState.Success(it) },
                onFailure = { error ->
                    AiCaptionUiState.Error(error.localizedMessage ?: "Failed to generate captions.")
                }
            )
        }
    }

    fun resetAiCaptionState() {
        _aiCaptionState.value = AiCaptionUiState.Idle
    }

    fun toggleLikeReel(reelId: String) {
        _reels.update { list ->
            list.map { reel ->
                if (reel.id == reelId) {
                    val newLiked = !reel.isLiked
                    val newCount = if (newLiked) reel.likesCount + 1 else reel.likesCount - 1
                    reel.copy(isLiked = newLiked, likesCount = maxOf(0, newCount))
                } else reel
            }
        }
    }

    fun doubleTapLikeReel(reelId: String) {
        _reels.update { list ->
            list.map { reel ->
                if (reel.id == reelId && !reel.isLiked) {
                    reel.copy(isLiked = true, likesCount = reel.likesCount + 1)
                } else reel
            }
        }
    }

    fun toggleSaveReel(reelId: String) {
        _reels.update { list ->
            list.map { reel ->
                if (reel.id == reelId) {
                    val newSaved = !reel.isSaved
                    _snackbarMessage.value = if (newSaved) "Reel saved" else "Reel unsaved"
                    reel.copy(isSaved = newSaved)
                } else reel
            }
        }
    }

    fun toggleReelMute() {
        _isReelMuted.update { !it }
    }

    fun addReelComment(reelId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val user = currentUser.value
        val newComment = Comment(
            id = "rc_${UUID.randomUUID()}",
            username = user.username,
            userAvatar = user.avatarUrl,
            text = trimmed,
            timestamp = "Just now",
            likesCount = 0
        )
        _reels.update { list ->
            list.map { reel ->
                if (reel.id == reelId) {
                    reel.copy(
                        comments = listOf(newComment) + reel.comments,
                        commentsCount = reel.commentsCount + 1
                    )
                } else reel
            }
        }
    }

    fun shareReel(reel: Reel, recipient: User? = null) {
        _reels.update { list ->
            list.map { item ->
                if (item.id == reel.id) item.copy(sharesCount = item.sharesCount + 1) else item
            }
        }
        if (recipient != null) {
            val preview = "Shared a reel by @${reel.user.username}"
            val reelChatMessage = ChatMessage(
                id = "msg_reel_${UUID.randomUUID()}",
                senderId = currentUser.value.id,
                text = preview,
                mediaUri = reel.videoUrl.ifBlank { reel.mediaUrl },
                mediaType = if (reel.videoUrl.isNotBlank()) ChatMediaType.VIDEO else ChatMediaType.IMAGE,
                thumbnailUrl = reel.mediaUrl,
                timestamp = "Just now",
                status = MessageDeliveryStatus.DELIVERED,
                isFromMe = true
            )
            _directMessages.update { list ->
                val idx = list.indexOfFirst { it.user.id == recipient.id || it.user.username == recipient.username }
                if (idx >= 0) {
                    val existing = list[idx]
                    val updated = existing.copy(
                        lastMessage = preview,
                        timestamp = "Just now",
                        messages = existing.messages + reelChatMessage
                    )
                    listOf(updated) + list.filterIndexed { i, _ -> i != idx }
                } else {
                    listOf(
                        DirectMessage(
                            id = "dm_reel_${recipient.id}",
                            user = recipient,
                            lastMessage = preview,
                            timestamp = "Just now",
                            isOnline = true,
                            lastSeenText = "Active now",
                            messages = listOf(reelChatMessage)
                        )
                    ) + list
                }
            }
            _snackbarMessage.value = "Reel sent to @${recipient.username}"
        } else {
            _snackbarMessage.value = "Reel link copied to clipboard"
        }
    }

    fun toggleFollowUser(userId: String) {
        _followedUsersOverride.update { current ->
            current + (userId to !(current[userId] ?: true))
        }
    }

    fun createPost(caption: String, imageUrl: String, location: String? = null) {
        val user = currentUser.value
        val hashtags = extractHashtags(caption)
        viewModelScope.launch {
            val res = repository.createPost(
                userId = currentUserId,
                username = user.username,
                avatarUrl = user.avatarUrl,
                caption = caption,
                imageUrl = imageUrl,
                location = location,
                hashtags = hashtags
            )
            if (res.isSuccess) {
                val createdPostId = res.getOrNull()
                _aiCaptionState.value = AiCaptionUiState.Idle
                _snackbarMessage.value = "Post published to Firestore! 🎉"
                _currentTab.value = NavigationTab.HOME
                val reactor = SampleData.sampleUsers.firstOrNull()
                if (reactor != null) {
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(2000)
                        recordPostLikeActivity(
                            actor = reactor,
                            postId = createdPostId,
                            postThumbnailUrl = imageUrl
                        )
                    }
                }
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to publish post"
            }
        }
    }

    private fun extractHashtags(text: String): List<String> {
        val regex = Regex("#(\\w+)")
        return regex.findAll(text).map { it.value }.toList()
    }

    fun refreshFeed() {
        if (_isRefreshingFeed.value) return
        viewModelScope.launch {
            _isRefreshingFeed.value = true
            try {
                val res = repository.fetchLatestUserPosts(currentUserId)
                res.fold(
                    onSuccess = { latestPosts ->
                        _manualRefreshedUserPosts.value = latestPosts
                    },
                    onFailure = { err ->
                        _snackbarMessage.value = err.localizedMessage ?: "Failed to refresh feed"
                    }
                )
            } finally {
                _isRefreshingFeed.value = false
            }
        }
    }

    fun loadMorePosts() {
        if (_isLoadingMorePosts.value) return
        viewModelScope.launch {
            _isLoadingMorePosts.value = true
            kotlinx.coroutines.delay(900)
            val recycled = SampleData.samplePosts.map {
                it.copy(
                    id = "p_more_${UUID.randomUUID()}",
                    timestamp = "1d ago"
                )
            }
            _recycledFeedPosts.update { it + recycled }
            _isLoadingMorePosts.value = false
        }
    }
}
