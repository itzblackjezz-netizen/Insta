package com.example

import com.example.model.NavigationTab
import com.example.ui.viewmodel.InstagramViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InstagramViewModelTest {

    private lateinit var viewModel: InstagramViewModel

    @Before
    fun setUp() {
        viewModel = InstagramViewModel()
    }

    @Test
    fun testInitialFeedPostsLoaded() {
        val posts = viewModel.posts.value
        assertTrue("Initial posts should not be empty", posts.isNotEmpty())
    }

    @Test
    fun testToggleLikePost() {
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
    fun testDoubleTapLikePost() {
        val post = viewModel.posts.value.first { !it.isLiked }
        val initialLikes = post.likesCount

        viewModel.doubleTapLikePost(post.id)

        val updatedPost = viewModel.posts.value.first { it.id == post.id }
        assertTrue(updatedPost.isLiked)
        assertEquals(initialLikes + 1, updatedPost.likesCount)
    }

    @Test
    fun testAddComment() {
        val post = viewModel.posts.value.first()
        val initialCommentsCount = post.commentsCount

        viewModel.addComment(post.id, "Amazing perspective!")

        val updatedPost = viewModel.posts.value.first { it.id == post.id }
        assertEquals(initialCommentsCount + 1, updatedPost.commentsCount)
        assertEquals("Amazing perspective!", updatedPost.comments.first().text)
    }

    @Test
    fun testToggleSavePost() {
        val post = viewModel.posts.value.first()
        val initialSaved = post.isSaved

        viewModel.toggleSavePost(post.id)

        val updatedPost = viewModel.posts.value.first { it.id == post.id }
        assertEquals(!initialSaved, updatedPost.isSaved)
    }

    @Test
    fun testCreatePost() {
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

        viewModel.closeStory()
        assertNull(viewModel.activeStory.value)
    }
}
