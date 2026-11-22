package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.NavigationTab
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.InstaSnapTheme
import com.example.ui.viewmodel.InstagramViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: InstagramViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

            InstaSnapTheme(darkTheme = isDarkMode) {
                InstagramApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun InstagramApp(
    viewModel: InstagramViewModel,
    modifier: Modifier = Modifier
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val reels by viewModel.reels.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val activeStory by viewModel.activeStory.collectAsStateWithLifecycle()
    val activeCommentsPost by viewModel.activeCommentsPost.collectAsStateWithLifecycle()
    val activeSharePost by viewModel.activeSharePost.collectAsStateWithLifecycle()
    val activeOptionsPost by viewModel.activeOptionsPost.collectAsStateWithLifecycle()
    val showDirectMessages by viewModel.showDirectMessages.collectAsStateWithLifecycle()
    val showNotifications by viewModel.showNotifications.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val directMessages by viewModel.directMessages.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMorePosts.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Handle Back Button
    BackHandler(
        enabled = showDirectMessages || showNotifications || currentTab != NavigationTab.HOME
    ) {
        when {
            showDirectMessages -> viewModel.setShowDirectMessages(false)
            showNotifications -> viewModel.setShowNotifications(false)
            currentTab != NavigationTab.HOME -> viewModel.selectTab(NavigationTab.HOME)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth >= 720.dp

        if (isWideScreen) {
            // Tablet / Desktop Layout: Navigation Rail on the Left
            Row(modifier = Modifier.fillMaxSize()) {
                AdaptiveNavigationRail(
                    selectedTab = currentTab,
                    onTabSelected = { viewModel.selectTab(it) },
                    currentUser = currentUser,
                    onOpenNotifications = { viewModel.setShowNotifications(true) },
                    onOpenMessages = { viewModel.setShowDirectMessages(true) }
                )

                VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    ScreenContent(
                        currentTab = currentTab,
                        viewModel = viewModel,
                        posts = posts,
                        stories = stories,
                        reels = reels,
                        currentUser = currentUser,
                        isLoadingMore = isLoadingMore,
                        isDarkMode = isDarkMode
                    )
                }
            }
        } else {
            // Mobile Layout: Scaffold with TopBar and BottomNavBar
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    if (currentTab == NavigationTab.HOME) {
                        InstagramTopAppBar(
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { viewModel.toggleDarkMode() },
                            onOpenNotifications = { viewModel.setShowNotifications(true) },
                            onOpenDirectMessages = { viewModel.setShowDirectMessages(true) }
                        )
                    }
                },
                bottomBar = {
                    // Hide bottom bar in Reels mode or when creating post for maximum immersion
                    if (currentTab != NavigationTab.CREATE && !showDirectMessages && !showNotifications) {
                        InstagramBottomNavBar(
                            selectedTab = currentTab,
                            onTabSelected = { viewModel.selectTab(it) },
                            currentUser = currentUser
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = if (currentTab == NavigationTab.HOME) innerPadding.calculateTopPadding() else 0.dp,
                            bottom = if (currentTab != NavigationTab.CREATE && !showDirectMessages && !showNotifications) {
                                innerPadding.calculateBottomPadding()
                            } else 0.dp
                        )
                ) {
                    ScreenContent(
                        currentTab = currentTab,
                        viewModel = viewModel,
                        posts = posts,
                        stories = stories,
                        reels = reels,
                        currentUser = currentUser,
                        isLoadingMore = isLoadingMore,
                        isDarkMode = isDarkMode
                    )
                }
            }
        }

        // Overlay: Direct Messages Screen
        AnimatedVisibility(
            visible = showDirectMessages,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            DirectMessagesScreen(
                messages = directMessages,
                onBack = { viewModel.setShowDirectMessages(false) }
            )
        }

        // Overlay: Notifications Screen
        AnimatedVisibility(
            visible = showNotifications,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            NotificationsScreen(
                notifications = notifications,
                onBack = { viewModel.setShowNotifications(false) }
            )
        }

        // Dialog: Fullscreen Story Viewer
        activeStory?.let { story ->
            StoryViewerDialog(
                story = story,
                onDismiss = { viewModel.closeStory() },
                onAdvanceStory = { viewModel.advanceStory() },
                onPreviousStory = { viewModel.previousStory() }
            )
        }

        // Modal Bottom Sheet: Comments
        activeCommentsPost?.let { post ->
            CommentsBottomSheet(
                post = post,
                currentUser = currentUser,
                onDismiss = { viewModel.closeComments() },
                onAddComment = { text -> viewModel.addComment(post.id, text) },
                onLikeComment = { commentId -> viewModel.toggleLikeComment(post.id, commentId) }
            )
        }

        // Modal Bottom Sheet: Share Post
        activeSharePost?.let { post ->
            ShareBottomSheet(
                post = post,
                onDismiss = { viewModel.closeShare() }
            )
        }

        // Modal Bottom Sheet: Post Options
        activeOptionsPost?.let { post ->
            PostOptionsBottomSheet(
                post = post,
                onDismiss = { viewModel.closeOptions() },
                onToggleSave = { viewModel.toggleSavePost(post.id) },
                onUnfollow = { viewModel.toggleFollowUser(post.user.id) }
            )
        }
    }
}

@Composable
private fun ScreenContent(
    currentTab: NavigationTab,
    viewModel: InstagramViewModel,
    posts: List<com.example.model.Post>,
    stories: List<com.example.model.Story>,
    reels: List<com.example.model.Reel>,
    currentUser: com.example.model.User,
    isLoadingMore: Boolean,
    isDarkMode: Boolean
) {
    when (currentTab) {
        NavigationTab.HOME -> {
            HomeScreen(
                currentUser = currentUser,
                stories = stories,
                posts = posts,
                isLoadingMore = isLoadingMore,
                onStoryClick = { story -> viewModel.openStory(story) },
                onAddStoryClick = { viewModel.selectTab(NavigationTab.CREATE) },
                onLikeClick = { postId -> viewModel.toggleLikePost(postId) },
                onDoubleTapLike = { postId -> viewModel.doubleTapLikePost(postId) },
                onCommentClick = { post -> viewModel.openComments(post) },
                onShareClick = { post -> viewModel.openShare(post) },
                onSaveClick = { postId -> viewModel.toggleSavePost(postId) },
                onOptionsClick = { post -> viewModel.openOptions(post) },
                onUserClick = { viewModel.selectTab(NavigationTab.PROFILE) },
                onAddQuickComment = { postId, text -> viewModel.addComment(postId, text) },
                onLoadMore = { viewModel.loadMorePosts() }
            )
        }
        NavigationTab.EXPLORE -> {
            ExploreScreen()
        }
        NavigationTab.CREATE -> {
            CreatePostScreen(
                onSharePost = { caption, imageUrl, location ->
                    viewModel.createPost(caption, imageUrl, location)
                },
                onCancel = { viewModel.selectTab(NavigationTab.HOME) }
            )
        }
        NavigationTab.REELS -> {
            ReelsScreen(
                reels = reels,
                onLikeReel = { reelId -> viewModel.toggleLikeReel(reelId) },
                onSaveReel = { reelId -> viewModel.toggleSaveReel(reelId) }
            )
        }
        NavigationTab.PROFILE -> {
            ProfileScreen(
                user = currentUser,
                posts = posts,
                highlights = com.example.data.SampleData.sampleHighlights,
                onEditProfileClick = { /* Edit profile */ },
                onPostClick = { post -> viewModel.openComments(post) }
            )
        }
    }
}
