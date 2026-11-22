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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
    private val _followedUsersOverride = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val _commentLikesOverride = MutableStateFlow<Map<String, Boolean>>(emptyMap())

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
        .catch { error ->
            handleStreamException("user posts", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

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
        firestoreUserPosts,
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

    private val _currentTab = MutableStateFlow(NavigationTab.HOME)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

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

    private val _directMessages = MutableStateFlow<List<DirectMessage>>(SampleData.sampleDirectMessages)
    val directMessages: StateFlow<List<DirectMessage>> = _directMessages.asStateFlow()

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
        _currentTab.value = tab
    }

    fun setShowDirectMessages(show: Boolean) {
        _showDirectMessages.value = show
    }

    fun setShowNotifications(show: Boolean) {
        _showNotifications.value = show
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
        _activeStory.value = story
        _stories.update { list ->
            list.map {
                if (it.id == story.id) it.copy(hasUnseenStory = false) else it
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
                _aiCaptionState.value = AiCaptionUiState.Idle
                _snackbarMessage.value = "Post published to Firestore! 🎉"
                _currentTab.value = NavigationTab.HOME
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to publish post"
            }
        }
    }

    private fun extractHashtags(text: String): List<String> {
        val regex = Regex("#(\\w+)")
        return regex.findAll(text).map { it.value }.toList()
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
