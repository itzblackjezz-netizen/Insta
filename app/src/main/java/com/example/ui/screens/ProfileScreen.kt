package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AssignmentInd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.SampleData
import com.example.model.Post
import com.example.model.Reel
import com.example.model.StoryHighlight
import com.example.model.User
import com.example.ui.components.ProfileAvatarPreviewDialog
import com.example.ui.components.ProfileConnectionsSheet
import com.example.ui.components.ProfileSettingsBottomSheet
import com.example.ui.theme.IgBlue
import com.example.ui.theme.InstagramGradient
import java.text.NumberFormat
import java.util.Locale

enum class ProfileSubTab {
    GRID,
    REELS,
    SAVED,
    TAGGED
}

@Composable
fun ProfileScreen(
    user: User,
    isOwnProfile: Boolean,
    isFollowingUser: Boolean,
    followedUsers: Map<String, Boolean>,
    posts: List<Post>,
    reels: List<Reel>,
    highlights: List<StoryHighlight>,
    isDarkMode: Boolean,
    unreadNotificationsCount: Int,
    onToggleDarkMode: () -> Unit,
    onBackFromCreator: () -> Unit,
    onToggleFollow: () -> Unit,
    onToggleFollowOther: (String) -> Unit,
    onMessageClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onShareProfileClick: () -> Unit,
    onSelectCreator: (User) -> Unit,
    onCreatePostClick: () -> Unit,
    onOpenActivity: () -> Unit,
    onSignOutClick: () -> Unit,
    onMenuClick: () -> Unit,
    onPostClick: (Post) -> Unit,
    onReelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOwnProfile) {
        BackHandler { onBackFromCreator() }
    }

    var selectedSubTab by remember(user.id) { mutableStateOf(ProfileSubTab.GRID) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDiscoverStrip by remember(user.id) { mutableStateOf(false) }
    var connectionsSheetTitle by remember { mutableStateOf<String?>(null) }
    var showAvatarPreview by remember { mutableStateOf(false) }

    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current

    val profileGridPosts = remember(posts, user, isOwnProfile) {
        if (isOwnProfile) {
            posts
        } else {
            val authored = posts.filter { it.user.id == user.id || it.user.username == user.username }
            if (authored.isNotEmpty()) authored else posts.take(3)
        }
    }

    val profileReels = remember(reels, user, isOwnProfile) {
        if (isOwnProfile) {
            reels
        } else {
            val authoredReels = reels.filter { it.user.id == user.id || it.user.username == user.username }
            if (authoredReels.isNotEmpty()) authoredReels else reels.take(2)
        }
    }

    val taggedPosts = remember(posts, user) {
        val explicitlyTagged = posts.filter {
            it.caption.contains("@${user.username}", ignoreCase = true) ||
                it.comments.any { c -> c.username == user.username }
        }
        if (explicitlyTagged.isNotEmpty()) explicitlyTagged else posts.takeLast(minOf(3, posts.size))
    }

    val savedPosts = remember(posts) {
        posts.filter { it.isSaved }
    }

    val displayedPostsCount = remember(user.postsCount, profileGridPosts.size, isOwnProfile) {
        if (isOwnProfile) maxOf(user.postsCount, profileGridPosts.size)
        else maxOf(user.postsCount, profileGridPosts.size)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .testTag("profile_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
        ) {
            // Top Profile Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isOwnProfile) {
                        IconButton(
                            onClick = onBackFromCreator,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("profile_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = user.username,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.testTag("profile_username")
                    )
                    if (user.isVerified) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified account",
                            tint = IgBlue,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                if (isOwnProfile) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onCreatePostClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("profile_create_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create post",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("profile_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Profile settings",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

            // Profile Scrollable Body
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("profile_content_list")
            ) {
                // Avatar + Posts / Followers / Following Stats
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .border(2.5.dp, InstagramGradient, CircleShape)
                                .padding(3.5.dp)
                                .clip(CircleShape)
                                .clickable { showAvatarPreview = true }
                                .testTag("profile_avatar")
                        ) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = "${user.fullName} profile picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStatItem(
                                count = "$displayedPostsCount",
                                label = "Posts",
                                onClick = { selectedSubTab = ProfileSubTab.GRID },
                                testTag = "profile_posts_stat"
                            )
                            ProfileStatItem(
                                count = user.followersCount,
                                label = "Followers",
                                onClick = { connectionsSheetTitle = "Followers" },
                                testTag = "profile_followers_stat"
                            )
                            ProfileStatItem(
                                count = user.followingCount,
                                label = "Following",
                                onClick = { connectionsSheetTitle = "Following" },
                                testTag = "profile_following_stat"
                            )
                        }
                    }
                }

                // Full Name, Bio, Website Link, Mutuals
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.testTag("profile_full_name")
                        )
                        if (user.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = user.bio,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.testTag("profile_bio")
                            )
                        }
                        if (user.link.isNotBlank()) {
                            Spacer(modifier = Modifier.height(5.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val normalizedUrl = if (
                                            user.link.startsWith("http://", ignoreCase = true) ||
                                            user.link.startsWith("https://", ignoreCase = true)
                                        ) user.link else "https://${user.link}"
                                        runCatching { uriHandler.openUri(normalizedUrl) }
                                    }
                                    .padding(vertical = 2.dp)
                                    .testTag("profile_website_link")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "Website link",
                                    tint = IgBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = user.link,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = IgBlue,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                        if (user.mutualFollowersSummary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = user.mutualFollowersSummary,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action Buttons: Owner (Edit Profile, Share Profile, Discover) OR Visitor (Follow, Message)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOwnProfile) {
                            Button(
                                onClick = onEditProfileClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("edit_profile_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "Edit profile",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString("https://instasnap.app/@${user.username}"))
                                    onShareProfileClick()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("share_profile_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "Share profile",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                color = if (showDiscoverStrip) IgBlue.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                IconButton(
                                    onClick = { showDiscoverStrip = !showDiscoverStrip },
                                    modifier = Modifier.testTag("discover_creators_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Discover creators",
                                        tint = if (showDiscoverStrip) IgBlue else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = onToggleFollow,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("profile_follow_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowingUser) MaterialTheme.colorScheme.surfaceVariant else IgBlue,
                                    contentColor = if (isFollowingUser) MaterialTheme.colorScheme.onSurface else Color.White
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (isFollowingUser) "Following" else "Follow",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Button(
                                onClick = onMessageClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("profile_message_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "Message",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }

                // Expandable Discover Creators Strip (tap to visit creator profile with Follow & Message)
                item {
                    AnimatedVisibility(visible = isOwnProfile && showDiscoverStrip) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Text(
                                text = "Discover creators",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(SampleData.sampleUsers, key = { it.id }) { creator ->
                                    val following = followedUsers[creator.id] ?: creator.isFollowing
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        border = BorderStroke(
                                            0.75.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier
                                            .width(138.dp)
                                            .clickable { onSelectCreator(creator) }
                                            .testTag("discover_creator_card_${creator.username}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            AsyncImage(
                                                model = creator.avatarUrl,
                                                contentDescription = creator.username,
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = creator.username,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = { onToggleFollowOther(creator.id) },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (following) MaterialTheme.colorScheme.surface else IgBlue,
                                                    contentColor = if (following) MaterialTheme.colorScheme.onSurface else Color.White
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(30.dp)
                                            ) {
                                                Text(
                                                    text = if (following) "Following" else "Follow",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Story Highlights Tray
                if (isOwnProfile) {
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(highlights) { highlight ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(66.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(62.dp)
                                            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                            .padding(3.dp)
                                    ) {
                                        AsyncImage(
                                            model = highlight.coverUrl,
                                            contentDescription = highlight.title,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = highlight.title,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            item {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(66.dp)
                                        .clickable(onClick = onCreatePostClick)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(62.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "New Highlight",
                                            tint = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "New",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }

                // Sub Tab Bar (Post Grid, Reels Tab, Saved Tab for owner, Tagged Posts Tab)
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        SubTabItem(
                            icon = Icons.Default.GridOn,
                            contentDescription = "Posts Grid",
                            isSelected = selectedSubTab == ProfileSubTab.GRID,
                            testTag = "profile_tab_grid",
                            onClick = { selectedSubTab = ProfileSubTab.GRID }
                        )
                        SubTabItem(
                            icon = Icons.Default.Movie,
                            contentDescription = "Reels",
                            isSelected = selectedSubTab == ProfileSubTab.REELS,
                            testTag = "profile_tab_reels",
                            onClick = { selectedSubTab = ProfileSubTab.REELS }
                        )
                        if (isOwnProfile) {
                            SubTabItem(
                                icon = Icons.Default.Bookmark,
                                contentDescription = "Saved Posts",
                                isSelected = selectedSubTab == ProfileSubTab.SAVED,
                                testTag = "profile_tab_saved",
                                onClick = { selectedSubTab = ProfileSubTab.SAVED }
                            )
                        }
                        SubTabItem(
                            icon = Icons.Outlined.AssignmentInd,
                            contentDescription = "Tagged Posts",
                            isSelected = selectedSubTab == ProfileSubTab.TAGGED,
                            testTag = "profile_tab_tagged",
                            onClick = { selectedSubTab = ProfileSubTab.TAGGED }
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        thickness = 0.5.dp
                    )
                }

                // Tab Body
                if (selectedSubTab == ProfileSubTab.REELS) {
                    if (profileReels.isEmpty()) {
                        item {
                            ProfileEmptyTabState(
                                title = "No Reels yet",
                                subtitle = "Short videos shared by @${user.username} will appear here."
                            )
                        }
                    } else {
                        val reelRows = profileReels.chunked(3)
                        items(reelRows) { rowReels ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                rowReels.forEach { reel ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(9f / 14f)
                                            .clickable(onClick = onReelClick)
                                            .testTag("profile_reel_tile_${reel.id}")
                                    ) {
                                        AsyncImage(
                                            model = reel.mediaUrl,
                                            contentDescription = reel.caption,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(6.dp)
                                                .background(
                                                    Color.Black.copy(alpha = 0.55f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = formatCompactCount(reel.likesCount),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }
                                    }
                                }
                                repeat(3 - rowReels.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    val displayedPosts = when (selectedSubTab) {
                        ProfileSubTab.GRID -> profileGridPosts
                        ProfileSubTab.SAVED -> savedPosts
                        ProfileSubTab.TAGGED -> taggedPosts
                        ProfileSubTab.REELS -> emptyList()
                    }

                    if (displayedPosts.isEmpty()) {
                        item {
                            val emptyTitle = when (selectedSubTab) {
                                ProfileSubTab.SAVED -> "No saved posts yet"
                                ProfileSubTab.TAGGED -> "No tagged posts yet"
                                else -> "No posts yet"
                            }
                            val emptySubtitle = when (selectedSubTab) {
                                ProfileSubTab.SAVED -> "Bookmark posts from your feed to revisit them here."
                                ProfileSubTab.TAGGED -> "Photos and videos @${user.username} is tagged in appear here."
                                else -> "Share your first photo or story to build your grid."
                            }
                            ProfileEmptyTabState(title = emptyTitle, subtitle = emptySubtitle)
                        }
                    } else {
                        val rows = displayedPosts.chunked(3)
                        items(rows) { rowPosts ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                rowPosts.forEach { post ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clickable { onPostClick(post) }
                                            .testTag("profile_grid_tile_${post.id}")
                                    ) {
                                        AsyncImage(
                                            model = post.mediaUrls.firstOrNull() ?: "",
                                            contentDescription = "Post by ${post.user.username}",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (post.isVideo) {
                                            Icon(
                                                imageVector = Icons.Default.Movie,
                                                contentDescription = "Video post",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(16.dp)
                                            )
                                        } else if (post.mediaUrls.size > 1) {
                                            Icon(
                                                imageVector = Icons.Default.Collections,
                                                contentDescription = "Multiple photos",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(16.dp)
                                            )
                                        }
                                    }
                                }
                                repeat(3 - rowPosts.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showSettingsSheet) {
        ProfileSettingsBottomSheet(
            user = user,
            isDarkMode = isDarkMode,
            unreadNotificationsCount = unreadNotificationsCount,
            onToggleDarkMode = onToggleDarkMode,
            onEditProfile = onEditProfileClick,
            onOpenActivity = onOpenActivity,
            onOpenSavedTab = { selectedSubTab = ProfileSubTab.SAVED },
            onOpenUpdatesSettings = onMenuClick,
            onSignOut = onSignOutClick,
            onDismiss = { showSettingsSheet = false }
        )
    }

    connectionsSheetTitle?.let { sheetTitle ->
        ProfileConnectionsSheet(
            title = sheetTitle,
            followedUsers = followedUsers,
            onToggleFollow = onToggleFollowOther,
            onSelectUser = onSelectCreator,
            onDismiss = { connectionsSheetTitle = null }
        )
    }

    if (showAvatarPreview) {
        ProfileAvatarPreviewDialog(
            user = user,
            onDismiss = { showAvatarPreview = false }
        )
    }
}

@Composable
private fun ProfileEmptyTabState(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileStatItem(
    count: String,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SubTabItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.onBackground)
            )
        } else {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

private fun formatCompactCount(count: Int): String {
    return if (count >= 1000) {
        String.format(Locale.US, "%.1fK", count / 1000.0)
    } else {
        NumberFormat.getNumberInstance(Locale.US).format(count)
    }
}
