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

    override fun observeUserProfile(userId: String): Flow<FirestoreUserProfile?> = profileFlow
    override fun observeUserPosts(userId: String): Flow<List<FirestoreUserPost>> = userPostsFlow
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
}
