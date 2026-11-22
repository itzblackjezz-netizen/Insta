package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SampleData
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class InstagramViewModel : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(SampleData.samplePosts)
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _stories = MutableStateFlow<List<Story>>(SampleData.sampleStories)
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    private val _reels = MutableStateFlow<List<Reel>>(SampleData.sampleReels)
    val reels: StateFlow<List<Reel>> = _reels.asStateFlow()

    private val _currentUser = MutableStateFlow<User>(SampleData.currentUser)
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    private val _currentTab = MutableStateFlow(NavigationTab.HOME)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _activeStory = MutableStateFlow<Story?>(null)
    val activeStory: StateFlow<Story?> = _activeStory.asStateFlow()

    private val _activeCommentsPost = MutableStateFlow<Post?>(null)
    val activeCommentsPost: StateFlow<Post?> = _activeCommentsPost.asStateFlow()

    private val _activeSharePost = MutableStateFlow<Post?>(null)
    val activeSharePost: StateFlow<Post?> = _activeSharePost.asStateFlow()

    private val _activeOptionsPost = MutableStateFlow<Post?>(null)
    val activeOptionsPost: StateFlow<Post?> = _activeOptionsPost.asStateFlow()

    private val _showDirectMessages = MutableStateFlow(false)
    val showDirectMessages: StateFlow<Boolean> = _showDirectMessages.asStateFlow()

    private val _showNotifications = MutableStateFlow(false)
    val showNotifications: StateFlow<Boolean> = _showNotifications.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(SampleData.sampleNotifications)
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _directMessages = MutableStateFlow<List<DirectMessage>>(SampleData.sampleDirectMessages)
    val directMessages: StateFlow<List<DirectMessage>> = _directMessages.asStateFlow()

    private val _isLoadingMorePosts = MutableStateFlow(false)
    val isLoadingMorePosts: StateFlow<Boolean> = _isLoadingMorePosts.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

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

    fun openStory(story: Story) {
        _activeStory.value = story
        // Mark as seen
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
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    val newLiked = !post.isLiked
                    val newCount = if (newLiked) post.likesCount + 1 else post.likesCount - 1
                    post.copy(isLiked = newLiked, likesCount = maxOf(0, newCount))
                } else post
            }
        }
    }

    fun doubleTapLikePost(postId: String) {
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    if (!post.isLiked) {
                        post.copy(isLiked = true, likesCount = post.likesCount + 1)
                    } else post
                } else post
            }
        }
    }

    fun toggleSavePost(postId: String) {
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    val newSaved = !post.isSaved
                    _snackbarMessage.value = if (newSaved) "Saved to collection" else "Removed from saved"
                    post.copy(isSaved = newSaved)
                } else post
            }
        }
    }

    fun openComments(post: Post) {
        _activeCommentsPost.value = post
    }

    fun closeComments() {
        _activeCommentsPost.value = null
    }

    fun addComment(postId: String, text: String) {
        if (text.isBlank()) return
        val newComment = Comment(
            id = UUID.randomUUID().toString(),
            username = _currentUser.value.username,
            userAvatar = _currentUser.value.avatarUrl,
            text = text.trim(),
            timestamp = "Just now",
            likesCount = 0,
            isLiked = false
        )
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    val updatedComments = listOf(newComment) + post.comments
                    post.copy(
                        comments = updatedComments,
                        commentsCount = post.commentsCount + 1
                    )
                } else post
            }
        }
        // Update active comments post reference
        _activeCommentsPost.value?.let { currentActive ->
            if (currentActive.id == postId) {
                _activeCommentsPost.value = _posts.value.firstOrNull { it.id == postId }
            }
        }
    }

    fun toggleLikeComment(postId: String, commentId: String) {
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    val updatedComments = post.comments.map { comment ->
                        if (comment.id == commentId) {
                            val newLiked = !comment.isLiked
                            val count = if (newLiked) comment.likesCount + 1 else comment.likesCount - 1
                            comment.copy(isLiked = newLiked, likesCount = maxOf(0, count))
                        } else comment
                    }
                    post.copy(comments = updatedComments)
                } else post
            }
        }
        _activeCommentsPost.value?.let { currentActive ->
            if (currentActive.id == postId) {
                _activeCommentsPost.value = _posts.value.firstOrNull { it.id == postId }
            }
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
        // Toggle follow on sample users
        _posts.update { list ->
            list.map { post ->
                if (post.user.id == userId) {
                    post.copy(user = post.user.copy(isFollowing = !post.user.isFollowing))
                } else post
            }
        }
    }

    fun createPost(caption: String, imageUrl: String, location: String? = null) {
        val user = _currentUser.value
        val newPost = Post(
            id = "user_post_${UUID.randomUUID()}",
            user = user,
            location = location?.ifBlank { null } ?: "San Francisco, CA",
            musicTrack = null,
            mediaUrls = listOf(imageUrl),
            isVideo = false,
            likesCount = 1,
            isLiked = true,
            isSaved = false,
            caption = caption,
            hashtags = extractHashtags(caption),
            commentsCount = 0,
            comments = emptyList(),
            timestamp = "Just now",
            aspectRatio = 1.0f
        )
        _posts.update { listOf(newPost) + it }
        _currentUser.update { it.copy(postsCount = it.postsCount + 1) }
        _snackbarMessage.value = "Post shared to feed! 🎉"
        _currentTab.value = NavigationTab.HOME
    }

    private fun extractHashtags(text: String): List<String> {
        val regex = Regex("#(\\w+)")
        return regex.findAll(text).map { it.value }.toList()
    }

    fun loadMorePosts() {
        if (_isLoadingMorePosts.value) return
        viewModelScope.launch {
            _isLoadingMorePosts.value = true
            kotlinx.coroutines.delay(1200)
            val currentList = _posts.value
            val recycled = SampleData.samplePosts.map {
                it.copy(
                    id = "p_more_${UUID.randomUUID()}",
                    timestamp = "1d ago"
                )
            }
            _posts.value = currentList + recycled
            _isLoadingMorePosts.value = false
        }
    }
}
