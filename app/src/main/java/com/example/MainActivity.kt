package com.example

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.firebase.GeminiAiService
import com.example.firebase.InstagramRepository
import com.example.firebase.configureAppCheck
import com.example.model.NavigationTab
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.InstaSnapTheme
import com.example.ui.viewmodel.InstagramViewModel
import com.example.update.AppUpdateChecker
import com.example.update.AppUpdatePreferences
import com.example.update.GitHubReleaseInfo
import com.example.update.UpdateCheckResult
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must initialize App Check before any Firebase AI or Firestore calls
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            val authUser by remember {
                Firebase.auth.authStateFlow()
            }.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val credentialManager = remember(context) { CredentialManager.create(context) }

            val updatePrefs = remember(context) {
                AppUpdatePreferences(context, context.getString(R.string.github_repo))
            }
            var configuredRepo by remember { mutableStateOf(updatePrefs.getConfiguredRepo()) }
            var isCheckingUpdates by remember { mutableStateOf(false) }
            var lastUpdateCheckResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
            var pendingUpdateRelease by remember { mutableStateOf<GitHubReleaseInfo?>(null) }
            var showUpdatesSheet by remember { mutableStateOf(false) }

            // Automatically check GitHub Releases for a newer APK when the app opens
            LaunchedEffect(Unit) {
                val repo = configuredRepo
                if (repo.isNotBlank()) {
                    isCheckingUpdates = true
                    val result = AppUpdateChecker.checkForUpdates(
                        repoSlug = repo,
                        currentVersion = BuildConfig.VERSION_NAME
                    )
                    lastUpdateCheckResult = result
                    isCheckingUpdates = false
                    if (result is UpdateCheckResult.UpdateAvailable) {
                        pendingUpdateRelease = result.release
                    }
                }
            }

            val user = authUser
            if (user == null) {
                InstaSnapTheme(darkTheme = isSystemInDarkTheme()) {
                    AuthScreen(onAuthSuccess = {})
                    pendingUpdateRelease?.let { release ->
                        UpdateAvailableDialog(
                            currentVersion = BuildConfig.VERSION_NAME,
                            release = release,
                            onUpdateNow = {
                                openReleaseInBrowser(context, release)
                                pendingUpdateRelease = null
                            },
                            onDismiss = { pendingUpdateRelease = null }
                        )
                    }
                }
            } else {
                key(user.uid) {
                    val viewModel: InstagramViewModel = viewModel(
                        key = user.uid,
                        factory = viewModelFactory {
                            initializer {
                                val app = checkNotNull(this[APPLICATION_KEY])
                                val db = FirebaseFirestore.getInstance(
                                    app.getString(R.string.firestore_database_id)
                                )
                                InstagramViewModel(
                                    repository = InstagramRepository(db),
                                    aiService = GeminiAiService(),
                                    currentUserId = user.uid,
                                    initialDisplayName = user.displayName,
                                    initialPhotoUrl = user.photoUrl?.toString()
                                )
                            }
                        }
                    )
                    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

                    InstaSnapTheme(darkTheme = isDarkMode) {
                        InstagramApp(
                            viewModel = viewModel,
                            onSignOutClick = {
                                signOut(
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            },
                            onOpenUpdatesSettings = { showUpdatesSheet = true }
                        )

                        pendingUpdateRelease?.let { release ->
                            UpdateAvailableDialog(
                                currentVersion = BuildConfig.VERSION_NAME,
                                release = release,
                                onUpdateNow = {
                                    openReleaseInBrowser(context, release)
                                    pendingUpdateRelease = null
                                },
                                onDismiss = { pendingUpdateRelease = null }
                            )
                        }

                        if (showUpdatesSheet) {
                            AppUpdatesSettingsSheet(
                                currentVersion = BuildConfig.VERSION_NAME,
                                configuredRepo = configuredRepo,
                                isChecking = isCheckingUpdates,
                                lastCheckResult = lastUpdateCheckResult,
                                onSaveAndCheck = { newRepo ->
                                    updatePrefs.setConfiguredRepo(newRepo)
                                    configuredRepo = newRepo
                                    scope.launch {
                                        isCheckingUpdates = true
                                        val result = AppUpdateChecker.checkForUpdates(
                                            repoSlug = newRepo,
                                            currentVersion = BuildConfig.VERSION_NAME
                                        )
                                        lastUpdateCheckResult = result
                                        isCheckingUpdates = false
                                    }
                                },
                                onOpenRelease = { release ->
                                    openReleaseInBrowser(context, release)
                                },
                                onDismiss = { showUpdatesSheet = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun openReleaseInBrowser(context: Context, release: GitHubReleaseInfo) {
    val targetUrl = release.apkDownloadUrl ?: release.htmlUrl
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No browser found to open update link.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun InstagramApp(
    viewModel: InstagramViewModel,
    onSignOutClick: () -> Unit,
    onOpenUpdatesSettings: () -> Unit = {},
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
    val activeAiCompanionPost by viewModel.activeAiCompanionPost.collectAsStateWithLifecycle()
    val aiCaptionState by viewModel.aiCaptionState.collectAsStateWithLifecycle()
    val aiCompanionState by viewModel.aiCompanionState.collectAsStateWithLifecycle()
    val showDirectMessages by viewModel.showDirectMessages.collectAsStateWithLifecycle()
    val showNotifications by viewModel.showNotifications.collectAsStateWithLifecycle()
    val showEditProfileDialog by viewModel.showEditProfileDialog.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val directMessages by viewModel.directMessages.collectAsStateWithLifecycle()
    val isRefreshingFeed by viewModel.isRefreshingFeed.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMorePosts.collectAsStateWithLifecycle()
    val followedUsers by viewModel.followedUsers.collectAsStateWithLifecycle()
    val isReelMuted by viewModel.isReelMuted.collectAsStateWithLifecycle()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val activeActivityAlert by viewModel.activeActivityAlert.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Trigger a local activity alert shortly after entering the feed if notifications haven't been viewed yet
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3500)
        if (!viewModel.showNotifications.value) {
            viewModel.simulateLocalActivity()
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
                    onOpenMessages = { viewModel.setShowDirectMessages(true) },
                    hasUnreadNotifications = unreadNotificationsCount > 0
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
                        isRefreshingFeed = isRefreshingFeed,
                        isLoadingMore = isLoadingMore,
                        followedUsers = followedUsers,
                        isReelMuted = isReelMuted,
                        aiCaptionState = aiCaptionState,
                        onSignOutClick = onSignOutClick,
                        onOpenUpdatesSettings = onOpenUpdatesSettings
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
                            onOpenDirectMessages = { viewModel.setShowDirectMessages(true) },
                            hasUnreadNotifications = unreadNotificationsCount > 0,
                            unreadMessagesCount = directMessages.sumOf { it.unreadCount }
                        )
                    }
                },
                bottomBar = {
                    // Hide bottom bar when creating post or in overlays for maximum immersion
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
                        isRefreshingFeed = isRefreshingFeed,
                        isLoadingMore = isLoadingMore,
                        followedUsers = followedUsers,
                        isReelMuted = isReelMuted,
                        aiCaptionState = aiCaptionState,
                        onSignOutClick = onSignOutClick,
                        onOpenUpdatesSettings = onOpenUpdatesSettings
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

        // Overlay: Notifications / Local Activity Feed Screen
        AnimatedVisibility(
            visible = showNotifications,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            NotificationsScreen(
                notifications = notifications,
                followedUsers = followedUsers,
                onToggleFollowUser = { userId -> viewModel.toggleFollowUser(userId) },
                onPostNotificationClick = { item ->
                    val linkedPost = item.postId?.let { id -> posts.firstOrNull { it.id == id } }
                    if (linkedPost != null) {
                        viewModel.setShowNotifications(false)
                        viewModel.openComments(linkedPost)
                    } else if (item.type == com.example.model.NotificationType.FOLLOW) {
                        viewModel.toggleFollowUser(item.user.id)
                    }
                },
                onSimulateActivity = { viewModel.simulateLocalActivity() },
                onBack = { viewModel.setShowNotifications(false) }
            )
        }

        // Floating In-App Local Activity Alert Banner (Post Likes & New Followers)
        if (!showNotifications) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.TopCenter
            ) {
                ActivityAlertBanner(
                    notification = activeActivityAlert,
                    onOpenActivityFeed = { viewModel.setShowNotifications(true) },
                    onDismiss = { viewModel.dismissActivityAlert() }
                )
            }
        }

        // Dialog: Fullscreen Story Viewer
        activeStory?.let { story ->
            val currentIndex = stories.indexOfFirst { it.id == story.id }
            StoryViewerDialog(
                story = story,
                hasPreviousStory = currentIndex > 0,
                hasNextStory = currentIndex in 0 until (stories.size - 1),
                onDismiss = { viewModel.closeStory() },
                onAdvanceStory = { viewModel.advanceStory() },
                onPreviousStory = { viewModel.previousStory() },
                onToggleLikeStory = { viewModel.toggleLikeStory(story.id) },
                onSendReply = { message -> viewModel.replyToStory(story, message) },
                onReactEmoji = { emoji -> viewModel.reactToStory(story, emoji) },
                onShareStory = { viewModel.shareStory(story) }
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

        // Modal Bottom Sheet: Gemini AI Post Companion
        activeAiCompanionPost?.let { post ->
            AiPostCompanionSheet(
                post = post,
                uiState = aiCompanionState,
                onRetry = { viewModel.retryAiCompanion() },
                onDismiss = { viewModel.closeAiCompanion() }
            )
        }

        // Dialog: Edit Profile (Persisted to Firestore)
        if (showEditProfileDialog) {
            EditProfileDialog(
                user = currentUser,
                onDismiss = { viewModel.setShowEditProfileDialog(false) },
                onSave = { username, fullName, bio, link ->
                    viewModel.updateProfile(username, fullName, bio, link)
                }
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
    isRefreshingFeed: Boolean,
    isLoadingMore: Boolean,
    followedUsers: Map<String, Boolean>,
    isReelMuted: Boolean,
    aiCaptionState: com.example.ui.viewmodel.AiCaptionUiState,
    onSignOutClick: () -> Unit,
    onOpenUpdatesSettings: () -> Unit
) {
    when (currentTab) {
        NavigationTab.HOME -> {
            HomeScreen(
                currentUser = currentUser,
                stories = stories,
                posts = posts,
                isRefreshing = isRefreshingFeed,
                isLoadingMore = isLoadingMore,
                onRefresh = { viewModel.refreshFeed() },
                onStoryClick = { story -> viewModel.openStory(story) },
                onAddStoryClick = { viewModel.selectTab(NavigationTab.CREATE) },
                onLikeClick = { postId -> viewModel.toggleLikePost(postId) },
                onDoubleTapLike = { postId -> viewModel.doubleTapLikePost(postId) },
                onCommentClick = { post -> viewModel.openComments(post) },
                onShareClick = { post -> viewModel.openShare(post) },
                onSaveClick = { postId -> viewModel.toggleSavePost(postId) },
                onOptionsClick = { post -> viewModel.openOptions(post) },
                onAiInsightsClick = { post -> viewModel.openAiCompanion(post) },
                onUserClick = { viewModel.selectTab(NavigationTab.PROFILE) },
                onAddQuickComment = { postId, text -> viewModel.addComment(postId, text) },
                onLoadMore = { viewModel.loadMorePosts() }
            )
        }
        NavigationTab.EXPLORE -> {
            ExploreScreen(
                feedPosts = posts,
                followedUsers = followedUsers,
                onToggleFollowUser = { userId -> viewModel.toggleFollowUser(userId) },
                onLikePost = { postId -> viewModel.toggleLikePost(postId) },
                onDoubleTapLikePost = { postId -> viewModel.doubleTapLikePost(postId) },
                onCommentPost = { post -> viewModel.openComments(post) },
                onSharePost = { post -> viewModel.openShare(post) },
                onSavePost = { postId -> viewModel.toggleSavePost(postId) },
                onOptionsPost = { post -> viewModel.openOptions(post) },
                onAiInsightsPost = { post -> viewModel.openAiCompanion(post) },
                onAddQuickComment = { postId, text -> viewModel.addComment(postId, text) }
            )
        }
        NavigationTab.CREATE -> {
            CreatePostScreen(
                aiCaptionState = aiCaptionState,
                onGenerateAiCaptions = { topicOrVibe, location, filterName ->
                    viewModel.generateAiCaptions(topicOrVibe, location, filterName)
                },
                onSharePost = { caption, imageUrl, location ->
                    viewModel.createPost(caption, imageUrl, location)
                },
                onCancel = { viewModel.selectTab(NavigationTab.HOME) }
            )
        }
        NavigationTab.REELS -> {
            ReelsScreen(
                reels = reels,
                currentUser = currentUser,
                followedUsers = followedUsers,
                isMuted = isReelMuted,
                onToggleMute = { viewModel.toggleReelMute() },
                onLikeReel = { reelId -> viewModel.toggleLikeReel(reelId) },
                onDoubleTapLikeReel = { reelId -> viewModel.doubleTapLikeReel(reelId) },
                onSaveReel = { reelId -> viewModel.toggleSaveReel(reelId) },
                onToggleFollowUser = { userId -> viewModel.toggleFollowUser(userId) },
                onAddReelComment = { reelId, text -> viewModel.addReelComment(reelId, text) },
                onShareReel = { reel, recipient -> viewModel.shareReel(reel, recipient) },
                onCreateReelClick = { viewModel.selectTab(NavigationTab.CREATE) }
            )
        }
        NavigationTab.PROFILE -> {
            ProfileScreen(
                user = currentUser,
                posts = posts,
                highlights = com.example.data.SampleData.sampleHighlights,
                onEditProfileClick = { viewModel.setShowEditProfileDialog(true) },
                onSignOutClick = onSignOutClick,
                onMenuClick = onOpenUpdatesSettings,
                onPostClick = { post -> viewModel.openComments(post) }
            )
        }
    }
}
