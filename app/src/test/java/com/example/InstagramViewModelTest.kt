package com.example

import com.example.firebase.AiAssistantDataSource
import com.example.firebase.AiCaptionResult
import com.example.firebase.AiStreamChunk
import com.example.firebase.FirestorePostState
import com.example.firebase.FirestoreUserComment
import com.example.firebase.FirestoreUserPost
import com.example.firebase.FirestoreUserProfile
import com.example.firebase.SocialDataSource
import com.example.model.NavigationTab
import com.example.ui.viewmodel.AiCaptionUiState
import com.example.ui.viewmodel.AiCompanionUiState
import com.example.ui.viewmodel.InstagramViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    private val dispatcher = UnconfinedTestDispatcher()
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeSocialDataSource : SocialDataSource {
    val profileFlow = MutableStateFlow<FirestoreUserProfile?>(null)
    val userPostsFlow = MutableStateFlow<List<FirestoreUserPost>>(emptyList())
    val postStatesFlow = MutableStateFlow<List<FirestorePostState>>(emptyList())
    val userCommentsFlow = MutableStateFlow<List<FirestoreUserComment>>(emptyList())

    var shouldFailFetchLatestPosts = false

    override fun observeUserProfile(userId: String): Flow<FirestoreUserProfile?> = profileFlow
    override fun observeUserPosts(userId: String): Flow<List<FirestoreUserPost>> = userPostsFlow
    override suspend fun fetchLatestUserPosts(userId: String): Result<List<FirestoreUserPost>> {
        if (shouldFailFetchLatestPosts) {
            return Result.failure(IllegalStateException("Firestore temporarily unavailable"))
        }
        return Result.success(userPostsFlow.value)
    }
    override fun observePostStates(userId: String): Flow<List<FirestorePostState>> = postStatesFlow
    override fun observeUserComments(userId: String): Flow<List<FirestoreUserComment>> = userCommentsFlow

    override suspend fun ensureUserProfile(
        userId: String,
        displayName: String?,
        photoUrl: String?
    ): Result<Unit> {
        if (profileFlow.value == null) {
            profileFlow.value = FirestoreUserProfile(
                userId = userId,
                username = "alex.creates",
                fullName = displayName ?: "Alex Mercer",
                avatarUrl = photoUrl ?: ""
            )
        }
        return Result.success(Unit)
    }

    override suspend fun updateProfile(
        userId: String,
        username: String,
        fullName: String,
        bio: String,
        link: String
    ): Result<Unit> {
        profileFlow.update { current ->
            (current ?: FirestoreUserProfile(userId = userId)).copy(
                username = username,
                fullName = fullName,
                bio = bio,
                link = link
            )
        }
        return Result.success(Unit)
    }

    var shouldFailMediaUpload = false
    var uploadedMediaCalls = mutableListOf<String>()

    override suspend fun uploadPostMedia(
        userId: String,
        mediaUriString: String,
        isVideo: Boolean,
        onProgress: (Float) -> Unit
    ): Result<String> {
        uploadedMediaCalls.add(mediaUriString)
        if (shouldFailMediaUpload) {
            return Result.failure(IllegalStateException("Storage bucket unavailable"))
        }
        onProgress(0.5f)
        val hostedUrl = if (mediaUriString.startsWith("http")) {
            mediaUriString
        } else {
            val ext = if (isVideo) "mp4" else "jpg"
            "https://firebasestorage.googleapis.com/v0/b/test-bucket/o/users%2F$userId%2Fposts%2Fupload.$ext?alt=media"
        }
        onProgress(1f)
        return Result.success(hostedUrl)
    }

    override suspend fun createPost(
        userId: String,
        username: String,
        avatarUrl: String,
        caption: String,
        imageUrl: String,
        location: String?,
        hashtags: List<String>
    ): Result<String> {
        val newId = "user_post_${userPostsFlow.value.size + 1}"
        val post = FirestoreUserPost(
            id = newId,
            userId = userId,
            authorUsername = username,
            authorAvatarUrl = avatarUrl,
            location = location,
            caption = caption,
            imageUrl = imageUrl,
            hashtags = hashtags,
            likesCount = 1L,
            liked = true,
            saved = false,
            commentsCount = 0L
        )
        userPostsFlow.update { listOf(post) + it }
        return Result.success(newId)
    }

    override suspend fun deletePost(userId: String, postId: String): Result<Unit> {
        userPostsFlow.update { list -> list.filterNot { it.id == postId } }
        return Result.success(Unit)
    }

    override suspend fun savePostInteraction(
        userId: String,
        postId: String,
        isUserPost: Boolean,
        liked: Boolean,
        likesCount: Int,
        saved: Boolean
    ): Result<Unit> {
        if (isUserPost) {
            userPostsFlow.update { list ->
                list.map {
                    if (it.id == postId) it.copy(liked = liked, likesCount = likesCount.toLong(), saved = saved)
                    else it
                }
            }
        } else {
            postStatesFlow.update { list ->
                val without = list.filterNot { it.postId == postId }
                without + FirestorePostState(
                    id = postId,
                    userId = userId,
                    postId = postId,
                    liked = liked,
                    saved = saved
                )
            }
        }
        return Result.success(Unit)
    }

    override suspend fun addUserComment(
        userId: String,
        postId: String,
        isUserPost: Boolean,
        username: String,
        avatarUrl: String,
        text: String
    ): Result<Unit> {
        val comment = FirestoreUserComment(
            id = "comment_${userCommentsFlow.value.size + 1}",
            userId = userId,
            postId = postId,
            authorUsername = username,
            authorAvatarUrl = avatarUrl,
            text = text,
            likesCount = 0L,
            liked = false
        )
        userCommentsFlow.update { listOf(comment) + it }
        return Result.success(Unit)
    }
}

private class FakeAiAssistantDataSource : AiAssistantDataSource {
    override suspend fun generatePostCaptions(
        topicOrVibe: String,
        location: String?,
        filterName: String
    ): Result<AiCaptionResult> {
        return Result.success(
            AiCaptionResult(
                captions = listOf(
                    "Golden hour vibes in ${location ?: "the city"} ✨",
                    "Chasing light with a $filterName mood.",
                    "Captured the quiet magic of the moment."
                ),
                hashtags = listOf("#goldenhour", "#instasnap", "#photography"),
                engagementTip = "Ask followers about their favorite spot to spark comments."
            )
        )
    }

    override fun streamPostCompanion(
        authorUsername: String,
        caption: String,
        location: String?,
        hashtags: List<String>
    ): Flow<AiStreamChunk> = flow {
        emit(AiStreamChunk.Streaming("Analyzing @$authorUsername..."))
        emit(AiStreamChunk.Completed("Warm golden tones and vibrant storytelling."))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class InstagramViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeSocialDataSource
    private lateinit var aiService: FakeAiAssistantDataSource
    private lateinit var viewModel: InstagramViewModel

    @Before
    fun setUp() {
        repository = FakeSocialDataSource()
        aiService = FakeAiAssistantDataSource()
        viewModel = InstagramViewModel(
            repository = repository,
            aiService = aiService,
            currentUserId = "test_user_123",
            initialDisplayName = "Alex Mercer",
            initialPhotoUrl = "https://example.com/avatar.jpg"
        )
    }

    @Test
    fun testInitialFeedPostsLoaded() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.posts.collect {} }
        val posts = viewModel.posts.value
        assertTrue("Initial posts should not be empty", posts.isNotEmpty())
    }

    @Test
    fun testToggleLikePost() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.posts.collect {} }
        val firstPost = viewModel.posts.value.first()
        val initialLiked = firstPost.isLiked
        val initialCount = firstPost.likesCount

        viewModel.toggleLikePost(firstPost.id)

        val updatedPost = viewModel.posts.value.first { it.id == firstPost.id }
        assertEquals(!initialLiked, updatedPost.isLiked)
        if (updatedPost.isLiked) {
            assertEquals(initialCount + 1, updatedPost.likesCount)
        } else {
            assertEquals(maxOf(0, initialCount - 1), updatedPost.likesCount)
        }
    }

    @Test
    fun testDoubleTapLikePost() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.posts.collect {} }
        val post = viewModel.posts.value.first { !it.isLiked }
        val initialLikes = post.likesCount

        viewModel.doubleTapLikePost(post.id)

        val updatedPost = viewModel.posts.value.first { it.id == post.id }
        assertTrue(updatedPost.isLiked)
        assertEquals(initialLikes + 1, updatedPost.likesCount)
    }

    @Test
    fun testAddComment() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.posts.collect {} }
        val post = viewModel.posts.value.first()
        val initialCommentsCount = post.commentsCount

        viewModel.addComment(post.id, "Amazing perspective!")

        val updatedPost = viewModel.posts.value.first { it.id == post.id }
        assertEquals(initialCommentsCount + 1, updatedPost.commentsCount)
        assertEquals("Amazing perspective!", updatedPost.comments.first().text)
    }

    @Test
    fun testReplyToCommentAndToggleLikeComment() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.posts.collect {} }
        val post = viewModel.posts.value.first { it.comments.isNotEmpty() }
        val targetComment = post.comments.first()
        val wasLiked = targetComment.isLiked
        val initialCommentLikes = targetComment.likesCount

        viewModel.toggleLikeComment(post.id, targetComment.id)
        val afterLikePost = viewModel.posts.value.first { it.id == post.id }
        val likedComment = afterLikePost.comments.first { it.id == targetComment.id }
        assertEquals(!wasLiked, likedComment.isLiked)
        if (!wasLiked) {
            assertEquals(initialCommentLikes + 1, likedComment.likesCount)
        } else {
            assertEquals(maxOf(0, initialCommentLikes - 1), likedComment.likesCount)
        }

        viewModel.replyToComment(post.id, targetComment, "Totally agree with your take!")
        val afterReplyPost = viewModel.posts.value.first { it.id == post.id }
        val replyComment = afterReplyPost.comments.firstOrNull {
            it.text == "@${targetComment.username} Totally agree with your take!"
        }
        assertNotNull(replyComment)
        assertEquals(targetComment.username, replyComment!!.replyToUsername)
    }

    @Test
    fun testToggleSavePost() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.posts.collect {} }
        val post = viewModel.posts.value.first()
        val initialSaved = post.isSaved

        viewModel.toggleSavePost(post.id)

        val updatedPost = viewModel.posts.value.first { it.id == post.id }
        assertEquals(!initialSaved, updatedPost.isSaved)
    }

    @Test
    fun testCreatePost() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.posts.collect {} }
        val initialPostsCount = viewModel.posts.value.size

        viewModel.createPost(
            caption = "Exploring the city lights #goldenhour #cityvibes",
            imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb",
            location = "Tokyo, Japan"
        )

        val updatedPosts = viewModel.posts.value
        assertEquals(initialPostsCount + 1, updatedPosts.size)
        val createdPost = updatedPosts.first()
        assertEquals("Tokyo, Japan", createdPost.location)
        assertTrue(createdPost.hashtags.contains("#goldenhour"))
        assertEquals(NavigationTab.HOME, viewModel.currentTab.value)
    }

    @Test
    fun testStoryNavigation() {
        val stories = viewModel.stories.value
        assertTrue(stories.size > 1)

        val firstStory = stories[0]
        viewModel.openStory(firstStory)
        assertEquals(firstStory.id, viewModel.activeStory.value?.id)

        viewModel.advanceStory()
        assertEquals(stories[1].id, viewModel.activeStory.value?.id)

        viewModel.previousStory()
        assertEquals(firstStory.id, viewModel.activeStory.value?.id)

        viewModel.closeStory()
        assertNull(viewModel.activeStory.value)
    }

    @Test
    fun testStoryLikeReactionAndReply() {
        val story = viewModel.stories.value[1]
        viewModel.openStory(story)

        viewModel.toggleLikeStory(story.id)
        assertTrue(viewModel.activeStory.value?.isLiked == true)

        viewModel.reactToStory(story, "🔥")
        assertEquals("🔥", viewModel.activeStory.value?.lastReaction)

        viewModel.replyToStory(story, "Love the lighting!")
        val dm = viewModel.directMessages.value.firstOrNull { it.user.id == story.userId }
        assertNotNull(dm)
        assertEquals("Replied to story: Love the lighting!", dm?.lastMessage)
    }

    @Test
    fun testGenerateAiCaptions() = runTest {
        viewModel.generateAiCaptions(
            topicOrVibe = "Sunset coffee",
            location = "San Francisco, CA",
            filterName = "Clarendon"
        )
        val state = viewModel.aiCaptionState.value
        assertTrue(state is AiCaptionUiState.Success)
        val success = state as AiCaptionUiState.Success
        assertEquals(3, success.result.captions.size)
        assertTrue(success.result.hashtags.contains("#goldenhour"))
    }

    @Test
    fun testAiCompanionStreaming() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.posts.collect {} }
        val post = viewModel.posts.value.first()
        viewModel.openAiCompanion(post)

        val state = viewModel.aiCompanionState.value
        assertTrue(state is AiCompanionUiState.Success)
        assertEquals("Warm golden tones and vibrant storytelling.", (state as AiCompanionUiState.Success).text)

        viewModel.closeAiCompanion()
        assertNull(viewModel.activeAiCompanionPost.value)
        assertEquals(AiCompanionUiState.Idle, viewModel.aiCompanionState.value)
    }

    @Test
    fun testRefreshFeedFetchesLatestFirestorePosts() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.posts.collect {} }
        val remotePost = FirestoreUserPost(
            id = "remote_post_99",
            userId = "test_user_123",
            authorUsername = "alex.creates",
            authorAvatarUrl = "https://example.com/avatar.jpg",
            caption = "Freshly fetched from Firestore #refresh",
            imageUrl = "https://example.com/photo.jpg",
            hashtags = listOf("#refresh"),
            likesCount = 5L
        )
        repository.userPostsFlow.value = listOf(remotePost)

        viewModel.refreshFeed()

        assertFalse(viewModel.isRefreshingFeed.value)
        assertTrue(viewModel.posts.value.any { it.id == "remote_post_99" })
        assertEquals("Feed refreshed (1 latest post)", viewModel.snackbarMessage.value)
    }

    @Test
    fun testDeleteUserPost() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.posts.collect {} }
        viewModel.createPost(
            caption = "Temporary cloud post #cleanup",
            imageUrl = "https://example.com/temp.jpg",
            location = "Seattle"
        )
        val created = viewModel.posts.value.firstOrNull { it.caption.contains("Temporary cloud post") }
        assertNotNull(created)

        viewModel.deletePost(created!!.id)

        assertFalse(viewModel.posts.value.any { it.id == created.id })
        assertEquals("Post deleted", viewModel.snackbarMessage.value)
    }

    @Test
    fun testCreatePostUploadsCameraPhotoToStorageAndPublishesWithCaption() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.posts.collect {} }
        val localCameraUri = "content://com.aistudio.instasnap.qzvm.fileprovider/camera_images/capture_12345.jpg"

        viewModel.createPost(
            caption = "Fresh camera shot at Golden Gate #camera #sanfrancisco",
            imageUrl = localCameraUri,
            location = "San Francisco, CA",
            isVideo = false
        )

        assertTrue(repository.uploadedMediaCalls.contains(localCameraUri))
        val published = viewModel.posts.value.firstOrNull {
            it.caption.contains("Fresh camera shot at Golden Gate")
        }
        assertNotNull(published)
        assertTrue(published!!.mediaUrls.first().startsWith("https://firebasestorage.googleapis.com/"))
        assertEquals(NavigationTab.HOME, viewModel.currentTab.value)
        assertEquals(com.example.ui.viewmodel.PostUploadState.Idle, viewModel.postUploadState.value)
    }

    @Test
    fun testCreatePostStorageUploadFailureKeepsUserOnCreateScreen() = runTest {
        repository.shouldFailMediaUpload = true
        viewModel.selectTab(NavigationTab.CREATE)

        viewModel.createPost(
            caption = "Failed upload test",
            imageUrl = "content://com.aistudio.instasnap.qzvm.fileprovider/camera_images/fail.jpg"
        )

        assertEquals(NavigationTab.CREATE, viewModel.currentTab.value)
        assertTrue(viewModel.postUploadState.value is com.example.ui.viewmodel.PostUploadState.Error)
        assertEquals("Storage bucket unavailable", viewModel.snackbarMessage.value)
    }

    @Test
    fun testReelsInteractionsCommentShareAndMute() {
        val initialReel = viewModel.reels.value.first()
        val initialComments = initialReel.commentsCount
        val initialShares = initialReel.sharesCount
        val initialLikes = initialReel.likesCount

        viewModel.toggleLikeReel(initialReel.id)
        val afterToggle = viewModel.reels.value.first { it.id == initialReel.id }
        assertEquals(!initialReel.isLiked, afterToggle.isLiked)

        if (!afterToggle.isLiked) {
            viewModel.doubleTapLikeReel(initialReel.id)
            assertTrue(viewModel.reels.value.first { it.id == initialReel.id }.isLiked)
            assertEquals(initialLikes, viewModel.reels.value.first { it.id == initialReel.id }.likesCount)
        }

        viewModel.addReelComment(initialReel.id, "Amazing cinematography!")
        val afterComment = viewModel.reels.value.first { it.id == initialReel.id }
        assertEquals(initialComments + 1, afterComment.commentsCount)
        assertEquals("Amazing cinematography!", afterComment.comments.first().text)

        viewModel.shareReel(afterComment, com.example.data.SampleData.sampleUsers.first())
        val afterShare = viewModel.reels.value.first { it.id == initialReel.id }
        assertEquals(initialShares + 1, afterShare.sharesCount)

        assertFalse(viewModel.isReelMuted.value)
        viewModel.toggleReelMute()
        assertTrue(viewModel.isReelMuted.value)
    }

    @Test
    fun testLocalActivityNotificationsAndUnreadState() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.unreadNotificationsCount.collect {}
        }
        val actor = com.example.data.SampleData.sampleUsers.first()
        val initialUnread = viewModel.unreadNotificationsCount.value

        viewModel.recordPostLikeActivity(
            actor = actor,
            postId = "post_1",
            postThumbnailUrl = "https://example.com/thumb.jpg"
        )
        assertEquals(initialUnread + 1, viewModel.unreadNotificationsCount.value)
        assertEquals(actor.username, viewModel.activeActivityAlert.value?.user?.username)
        assertEquals(com.example.model.NotificationType.LIKE, viewModel.activeActivityAlert.value?.type)

        viewModel.recordFollowActivity(actor = com.example.data.SampleData.sampleUsers[1])
        assertEquals(initialUnread + 2, viewModel.unreadNotificationsCount.value)
        assertEquals(com.example.model.NotificationType.FOLLOW, viewModel.activeActivityAlert.value?.type)

        viewModel.setShowNotifications(true)
        assertEquals(0, viewModel.unreadNotificationsCount.value)
        assertNull(viewModel.activeActivityAlert.value)
    }

    @Test
    fun testProfileViewingMessageAndShare() {
        val creator = com.example.data.SampleData.sampleUsers.first()

        viewModel.openUserProfile(creator)
        assertEquals(NavigationTab.PROFILE, viewModel.currentTab.value)
        assertEquals(creator.id, viewModel.viewedProfileUser.value?.id)

        viewModel.messageUser(creator)
        assertTrue(viewModel.showDirectMessages.value)
        assertEquals(creator.id, viewModel.directMessages.value.first().user.id)

        viewModel.shareProfile(creator)
        assertEquals("Profile link copied for @${creator.username}", viewModel.snackbarMessage.value)

        viewModel.closeViewedUserProfile()
        assertNull(viewModel.viewedProfileUser.value)

        // Selecting Profile tab directly resets to account owner
        viewModel.openUserProfile(creator)
        viewModel.selectTab(NavigationTab.PROFILE)
        assertNull(viewModel.viewedProfileUser.value)
    }

    @Test
    fun testDirectMessagingOpenSendMediaReactAndReadReceipts() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activeChatThread.collect {}
        }
        val firstConversation = viewModel.directMessages.value.first { it.unreadCount > 0 }

        // Opening chat clears unreadCount and marks incoming messages as SEEN
        viewModel.openChat(firstConversation.id)
        val openedThread = viewModel.activeChatThread.value
        assertNotNull(openedThread)
        assertEquals(firstConversation.id, openedThread?.id)
        assertEquals(0, openedThread?.unreadCount)
        assertTrue(
            openedThread?.messages?.filter { !it.isFromMe }?.all {
                it.status == com.example.model.MessageDeliveryStatus.SEEN
            } == true
        )

        val initialCount = openedThread?.messages?.size ?: 0

        // Send a text message
        viewModel.sendChatMessage(firstConversation.id, text = "See you at golden hour!")
        val afterText = viewModel.directMessages.value.first { it.id == firstConversation.id }
        assertEquals(initialCount + 1, afterText.messages.size)
        val sentTextMsg = afterText.messages.last()
        assertEquals("See you at golden hour!", sentTextMsg.text)
        assertTrue(sentTextMsg.isFromMe)
        assertTrue(sentTextMsg.timestamp.isNotBlank())
        assertEquals("You: See you at golden hour!", afterText.lastMessage)

        // Toggle emoji reaction on that message
        viewModel.toggleMessageReaction(firstConversation.id, sentTextMsg.id, "🔥")
        val afterReact = viewModel.directMessages.value.first { it.id == firstConversation.id }
        assertEquals("🔥", afterReact.messages.last().reactions[viewModel.currentUser.value.id])

        // Toggle same emoji again removes reaction
        viewModel.toggleMessageReaction(firstConversation.id, sentTextMsg.id, "🔥")
        val afterUnreact = viewModel.directMessages.value.first { it.id == firstConversation.id }
        assertFalse(afterUnreact.messages.last().reactions.containsKey(viewModel.currentUser.value.id))

        // Send image and video attachments
        viewModel.sendChatMessage(
            conversationId = firstConversation.id,
            mediaUri = "https://example.com/photo.jpg",
            mediaType = com.example.model.ChatMediaType.IMAGE
        )
        viewModel.sendChatMessage(
            conversationId = firstConversation.id,
            mediaUri = "https://example.com/video.mp4",
            mediaType = com.example.model.ChatMediaType.VIDEO
        )
        val afterMedia = viewModel.directMessages.value.first { it.id == firstConversation.id }
        assertEquals(initialCount + 3, afterMedia.messages.size)
        assertEquals(com.example.model.ChatMediaType.IMAGE, afterMedia.messages[initialCount + 1].mediaType)
        assertEquals(com.example.model.ChatMediaType.VIDEO, afterMedia.messages[initialCount + 2].mediaType)
        assertEquals("You sent a video", afterMedia.lastMessage)

        // Close chat clears activeChatId
        viewModel.closeChat()
        assertNull(viewModel.activeChatId.value)

        // Starting a new conversation with chef_clara creates & opens the thread
        val newRecipient = com.example.data.SampleData.sampleUsers.last()
        viewModel.startNewConversation(newRecipient)
        assertTrue(viewModel.showDirectMessages.value)
        assertEquals("dm_${newRecipient.id}", viewModel.activeChatId.value)
    }

    @Test
    fun testNotificationsCategoriesFollowRequestsAndStoryInteractions() {
        val notifications = viewModel.notifications.value
        val types = notifications.map { it.type }.toSet()

        assertTrue(types.contains(com.example.model.NotificationType.LIKE))
        assertTrue(types.contains(com.example.model.NotificationType.COMMENT))
        assertTrue(types.contains(com.example.model.NotificationType.FOLLOW))
        assertTrue(types.contains(com.example.model.NotificationType.MENTION))
        assertTrue(types.contains(com.example.model.NotificationType.STORY_INTERACTION))
        assertTrue(types.contains(com.example.model.NotificationType.FOLLOW_REQUEST))
        assertTrue(notifications.all { it.timestamp.isNotBlank() })

        // Confirming a follow request converts it into a FOLLOW notification
        val requests = notifications.filter { it.type == com.example.model.NotificationType.FOLLOW_REQUEST }
        assertTrue(requests.size >= 2)
        val firstReq = requests[0]
        val secondReq = requests[1]

        viewModel.confirmFollowRequest(firstReq.id)
        val afterConfirm = viewModel.notifications.value.first { it.id == firstReq.id }
        assertEquals(com.example.model.NotificationType.FOLLOW, afterConfirm.type)
        assertEquals("started following you.", afterConfirm.text)

        // Deleting a follow request removes it
        viewModel.deleteFollowRequest(secondReq.id)
        assertFalse(viewModel.notifications.value.any { it.id == secondReq.id })

        // Recording comment, mention, and story interaction notifications
        val actor = com.example.data.SampleData.sampleUsers.first()
        viewModel.recordStoryInteractionActivity(actor = actor, storyId = "story_me")
        assertEquals(com.example.model.NotificationType.STORY_INTERACTION, viewModel.activeActivityAlert.value?.type)

        viewModel.recordCommentActivity(actor = actor, commentText = "Awesome frame!", postId = "post_1")
        assertEquals(com.example.model.NotificationType.COMMENT, viewModel.activeActivityAlert.value?.type)

        viewModel.recordMentionActivity(actor = actor, postId = "post_1")
        assertEquals(com.example.model.NotificationType.MENTION, viewModel.activeActivityAlert.value?.type)

        // Opening story by id from notification opens StoryViewer
        viewModel.openStoryById("story_me")
        assertEquals("story_me", viewModel.activeStory.value?.id)
    }

    @Test
    fun testExploreHashtagsExtractionAndSyncRecovery() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.posts.collect {} }

        // 1. Firestore post with inline and explicit hashtags surfaces for Explore filtering
        repository.userPostsFlow.value = listOf(
            FirestoreUserPost(
                id = "explore_cloud_1",
                userId = "test_user_123",
                authorUsername = "alex.creates",
                authorAvatarUrl = "https://example.com/avatar.jpg",
                caption = "Concrete light study #MasonryExplore #Architecture",
                imageUrl = "https://example.com/explore1.jpg",
                hashtags = listOf("#masonryexplore", "#minimal"),
                likesCount = 14L
            )
        )
        val cloudPost = viewModel.posts.value.firstOrNull { it.id == "explore_cloud_1" }
        assertNotNull(cloudPost)
        val normalized = com.example.ui.screens.extractNormalizedHashtags(cloudPost!!)
        assertEquals(listOf("#masonryexplore", "#minimal", "#architecture"), normalized)

        // 2. Refresh failure updates syncState to Error, retry recovers to Ready
        repository.shouldFailFetchLatestPosts = true
        viewModel.refreshFeed()
        val errorState = viewModel.syncState.value
        assertTrue(errorState is com.example.ui.viewmodel.FirestoreSyncState.Error)
        assertEquals(
            "Firestore temporarily unavailable",
            (errorState as com.example.ui.viewmodel.FirestoreSyncState.Error).message
        )

        repository.shouldFailFetchLatestPosts = false
        viewModel.refreshFeed()
        assertEquals(com.example.ui.viewmodel.FirestoreSyncState.Ready, viewModel.syncState.value)
    }

    @Test
    fun testCreateVideoPostPopulatesReelsFeed() = runTest {
        val initialReelsCount = viewModel.reels.value.size
        val localVideoUri = "content://media/external/video/media/901"

        viewModel.createPost(
            caption = "Golden hour skate clip #reels #video",
            imageUrl = localVideoUri,
            location = "Venice Beach, CA",
            isVideo = true
        )

        assertEquals(initialReelsCount + 1, viewModel.reels.value.size)
        val newReel = viewModel.reels.value.first()
        assertEquals("Golden hour skate clip #reels #video", newReel.caption)
        assertTrue(newReel.videoUrl.startsWith("https://firebasestorage.googleapis.com/"))
        assertTrue(newReel.videoUrl.endsWith(".mp4?alt=media"))
    }

    @Test
    fun testProfileFollowAndUnfollowInteraction() {
        val creator = com.example.data.SampleData.sampleUsers.first { !it.isFollowing }
        viewModel.openUserProfile(creator)
        assertEquals(NavigationTab.PROFILE, viewModel.currentTab.value)
        assertEquals(creator.id, viewModel.viewedProfileUser.value?.id)
        assertEquals(false, viewModel.followedUsers.value[creator.id])

        viewModel.toggleFollowUser(creator.id)
        assertEquals(true, viewModel.followedUsers.value[creator.id])
        assertEquals("Following @${creator.username}", viewModel.snackbarMessage.value)

        viewModel.toggleFollowUser(creator.id)
        assertEquals(false, viewModel.followedUsers.value[creator.id])
        assertEquals("Unfollowed @${creator.username}", viewModel.snackbarMessage.value)
    }

    @Test
    fun testRefreshReelsReloadsTrendingAndRemoteVideos() = runTest {
        val initialTopId = viewModel.reels.value.first().id

        // 1. Refresh with curated trending reels rotates top trending reel forward
        viewModel.refreshReels()
        assertFalse(viewModel.isRefreshingReels.value)
        assertEquals("Latest trending reels loaded", viewModel.snackbarMessage.value)
        assertNotEquals(initialTopId, viewModel.reels.value.first().id)

        // 2. Refresh when a new remote video post exists in Firestore surfaces it at the top
        repository.userPostsFlow.value = listOf(
            FirestoreUserPost(
                id = "remote_video_reel_1",
                userId = "test_user_123",
                authorUsername = "alex.creates",
                authorAvatarUrl = "https://example.com/avatar.jpg",
                caption = "Night drone flight #reels #trending",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/test/o/drone_clip.mp4?alt=media",
                likesCount = 12L
            )
        )
        viewModel.refreshReels()
        assertFalse(viewModel.isRefreshingReels.value)
        assertEquals("remote_video_reel_1", viewModel.reels.value.first().id)
        assertEquals("Reels refreshed (1 latest video)", viewModel.snackbarMessage.value)

        // 3. Refresh failure resets isRefreshingReels and surfaces error message
        repository.shouldFailFetchLatestPosts = true
        viewModel.refreshReels()
        assertFalse(viewModel.isRefreshingReels.value)
        assertEquals("Firestore temporarily unavailable", viewModel.snackbarMessage.value)
    }
}
