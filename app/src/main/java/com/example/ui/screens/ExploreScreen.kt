package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.SampleData
import com.example.model.Comment
import com.example.model.Post
import com.example.model.User
import com.example.ui.components.PostCard
import com.example.ui.theme.IgBlue
import com.example.ui.theme.IgPink
import com.example.ui.theme.IgRed
import com.example.ui.theme.InstagramGradient
import java.text.NumberFormat
import java.util.Locale

private data class ExploreDiscoveryItem(
    val id: String,
    val post: Post,
    val category: String,
    val masonryAspectRatio: Float,
    val isTrending: Boolean = false
)

private data class PopularHashtagItem(
    val tag: String,
    val postVolume: String
)

@Composable
fun ExploreScreen(
    feedPosts: List<Post> = SampleData.samplePosts,
    followedUsers: Map<String, Boolean> = emptyMap(),
    onToggleFollowUser: (String) -> Unit = {},
    onLikePost: (String) -> Unit = {},
    onDoubleTapLikePost: (String) -> Unit = {},
    onCommentPost: (Post) -> Unit = {},
    onSharePost: (Post) -> Unit = {},
    onSavePost: (String) -> Unit = {},
    onOptionsPost: (Post) -> Unit = {},
    onAiInsightsPost: (Post) -> Unit = {},
    onAddQuickComment: (String, String) -> Unit = { _, _ -> },
    onLikeComment: (String, String) -> Unit = { _, _ -> },
    onReplyComment: (String, Comment, String) -> Unit = { _, _, _ -> },
    onUserClick: (User) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SampleData.exploreCategories[0]) }
    var selectedHashtag by remember { mutableStateOf<String?>(null) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }

    // Local interaction state for discovery-only items not present in main feedPosts
    var discoveryLikes by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var discoverySaves by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    val popularHashtags = remember {
        listOf(
            PopularHashtagItem("#minimalarchitecture", "248K"),
            PopularHashtagItem("#tokyo", "1.4M"),
            PopularHashtagItem("#goldenhour", "890K"),
            PopularHashtagItem("#editorial", "412K"),
            PopularHashtagItem("#nordichome", "185K"),
            PopularHashtagItem("#artisanbread", "96K"),
            PopularHashtagItem("#streetphotography", "630K"),
            PopularHashtagItem("#filmphoto", "310K")
        )
    }

    val suggestedAccounts = remember { SampleData.sampleUsers }

    val discoveryItems = remember(feedPosts, discoveryLikes, discoverySaves) {
        val baseFeedMap = feedPosts.associateBy { it.id }
        val curatedCatalog = listOf(
            ExploreDiscoveryItem(
                id = "post_1",
                post = baseFeedMap["post_1"] ?: SampleData.samplePosts[0],
                category = "Architecture",
                masonryAspectRatio = 0.78f,
                isTrending = true
            ),
            ExploreDiscoveryItem(
                id = "post_2",
                post = baseFeedMap["post_2"] ?: SampleData.samplePosts[1],
                category = "Travel",
                masonryAspectRatio = 0.68f,
                isTrending = true
            ),
            ExploreDiscoveryItem(
                id = "post_3",
                post = baseFeedMap["post_3"] ?: SampleData.samplePosts[2],
                category = "Style",
                masonryAspectRatio = 1.05f,
                isTrending = true
            ),
            ExploreDiscoveryItem(
                id = "post_4",
                post = baseFeedMap["post_4"] ?: SampleData.samplePosts[3],
                category = "Interior",
                masonryAspectRatio = 0.85f,
                isTrending = false
            ),
            ExploreDiscoveryItem(
                id = "post_5",
                post = baseFeedMap["post_5"] ?: SampleData.samplePosts[4],
                category = "Food & Coffee",
                masonryAspectRatio = 0.75f,
                isTrending = true
            ),
            ExploreDiscoveryItem(
                id = "exp_6",
                post = Post(
                    id = "exp_6",
                    user = SampleData.sampleUsers[0],
                    location = "Yosemite Valley, CA",
                    mediaUrls = listOf(SampleData.sampleExploreItems[5]),
                    isVideo = true,
                    videoDuration = "0:22",
                    likesCount = 6420 + if (discoveryLikes["exp_6"] == true) 1 else 0,
                    isLiked = discoveryLikes["exp_6"] ?: false,
                    isSaved = discoverySaves["exp_6"] ?: false,
                    caption = "Morning mist rolling across the valley at dawn. Worth the 4am hike every time.",
                    hashtags = listOf("#goldenhour", "#travel", "#filmphoto"),
                    commentsCount = 48,
                    timestamp = "3h ago"
                ),
                category = "Travel",
                masonryAspectRatio = 1.15f
            ),
            ExploreDiscoveryItem(
                id = "exp_7",
                post = Post(
                    id = "exp_7",
                    user = SampleData.sampleUsers[0],
                    location = "Berlin, Germany",
                    mediaUrls = listOf(SampleData.sampleExploreItems[6]),
                    isVideo = false,
                    likesCount = 3190 + if (discoveryLikes["exp_7"] == true) 1 else 0,
                    isLiked = discoveryLikes["exp_7"] ?: false,
                    isSaved = discoverySaves["exp_7"] ?: false,
                    caption = "Glass reflections and sharp steel angles after the rain.",
                    hashtags = listOf("#minimalarchitecture", "#streetphotography"),
                    commentsCount = 29,
                    timestamp = "5h ago"
                ),
                category = "Architecture",
                masonryAspectRatio = 0.72f
            ),
            ExploreDiscoveryItem(
                id = "exp_8",
                post = Post(
                    id = "exp_8",
                    user = SampleData.sampleUsers[2],
                    location = "Milan, Italy",
                    mediaUrls = listOf(SampleData.sampleExploreItems[7]),
                    isVideo = true,
                    videoDuration = "0:18",
                    likesCount = 9180 + if (discoveryLikes["exp_8"] == true) 1 else 0,
                    isLiked = discoveryLikes["exp_8"] ?: false,
                    isSaved = discoverySaves["exp_8"] ?: false,
                    caption = "Backstage motion study from Milan street style week.",
                    hashtags = listOf("#editorial", "#fashionstyle", "#streetart"),
                    commentsCount = 112,
                    timestamp = "6h ago"
                ),
                category = "Style",
                masonryAspectRatio = 0.66f,
                isTrending = true
            ),
            ExploreDiscoveryItem(
                id = "exp_9",
                post = Post(
                    id = "exp_9",
                    user = SampleData.sampleUsers[1],
                    location = "San Francisco, CA",
                    mediaUrls = listOf(SampleData.sampleExploreItems[8]),
                    isVideo = false,
                    likesCount = 4870 + if (discoveryLikes["exp_9"] == true) 1 else 0,
                    isLiked = discoveryLikes["exp_9"] ?: false,
                    isSaved = discoverySaves["exp_9"] ?: false,
                    caption = "Golden Gate silhouette cutting through the Pacific fog.",
                    hashtags = listOf("#goldenhour", "#filmphoto", "#travel"),
                    commentsCount = 54,
                    timestamp = "9h ago"
                ),
                category = "Film Photo",
                masonryAspectRatio = 0.92f
            ),
            ExploreDiscoveryItem(
                id = "exp_10",
                post = Post(
                    id = "exp_10",
                    user = SampleData.sampleUsers[3],
                    location = "Stockholm, Sweden",
                    mediaUrls = listOf(SampleData.sampleExploreItems[9]),
                    isVideo = false,
                    likesCount = 2150 + if (discoveryLikes["exp_10"] == true) 1 else 0,
                    isLiked = discoveryLikes["exp_10"] ?: false,
                    isSaved = discoverySaves["exp_10"] ?: false,
                    caption = "Clean studio desk setup with warm oak and afternoon light.",
                    hashtags = listOf("#nordichome", "#minimalarchitecture"),
                    commentsCount = 19,
                    timestamp = "11h ago"
                ),
                category = "Interior",
                masonryAspectRatio = 1.18f
            ),
            ExploreDiscoveryItem(
                id = "exp_11",
                post = Post(
                    id = "exp_11",
                    user = SampleData.sampleUsers[1],
                    location = "Shinjuku, Tokyo",
                    mediaUrls = listOf(SampleData.sampleExploreItems[10]),
                    isVideo = true,
                    videoDuration = "0:29",
                    likesCount = 7540 + if (discoveryLikes["exp_11"] == true) 1 else 0,
                    isLiked = discoveryLikes["exp_11"] ?: false,
                    isSaved = discoverySaves["exp_11"] ?: false,
                    caption = "Neon alleyways and late-night train crossings in Tokyo.",
                    hashtags = listOf("#tokyo", "#streetphotography", "#streetart"),
                    commentsCount = 87,
                    timestamp = "14h ago"
                ),
                category = "Street Art",
                masonryAspectRatio = 0.74f
            ),
            ExploreDiscoveryItem(
                id = "exp_12",
                post = Post(
                    id = "exp_12",
                    user = SampleData.sampleUsers[4],
                    location = "Montmartre, Paris",
                    mediaUrls = listOf(SampleData.sampleExploreItems[11]),
                    isVideo = false,
                    likesCount = 3810 + if (discoveryLikes["exp_12"] == true) 1 else 0,
                    isLiked = discoveryLikes["exp_12"] ?: false,
                    isSaved = discoverySaves["exp_12"] ?: false,
                    caption = "Morning espresso pour before the bakery doors open.",
                    hashtags = listOf("#artisanbread", "#filmphoto"),
                    commentsCount = 33,
                    timestamp = "1d ago"
                ),
                category = "Food & Coffee",
                masonryAspectRatio = 0.88f
            )
        )

        // Include any newly authored cloud posts at the top of discovery
        val userAuthoredDiscovery = feedPosts
            .filter { p -> curatedCatalog.none { it.id == p.id } && !p.id.startsWith("p_more_") }
            .mapIndexed { idx, post ->
                ExploreDiscoveryItem(
                    id = post.id,
                    post = post,
                    category = "For You",
                    masonryAspectRatio = if (idx % 2 == 0) 0.82f else 1.0f,
                    isTrending = true
                )
            }

        userAuthoredDiscovery + curatedCatalog
    }

    val queryTrimmed = searchQuery.trim().lowercase()

    val filteredHashtags = remember(popularHashtags, queryTrimmed) {
        if (queryTrimmed.isEmpty()) popularHashtags
        else popularHashtags.filter { it.tag.lowercase().contains(queryTrimmed) }
    }

    val filteredAccounts = remember(suggestedAccounts, queryTrimmed) {
        if (queryTrimmed.isEmpty()) suggestedAccounts
        else suggestedAccounts.filter {
            it.username.lowercase().contains(queryTrimmed) ||
                it.fullName.lowercase().contains(queryTrimmed) ||
                it.bio.lowercase().contains(queryTrimmed)
        }
    }

    val filteredTrending = remember(discoveryItems, queryTrimmed, selectedHashtag) {
        discoveryItems.filter { item ->
            item.isTrending &&
                matchesQueryAndHashtag(item.post, item.category, queryTrimmed, selectedHashtag)
        }
    }

    val filteredMasonryItems = remember(discoveryItems, selectedCategory, queryTrimmed, selectedHashtag) {
        discoveryItems.filter { item ->
            val matchesCat = selectedCategory == "For You" || item.category.equals(selectedCategory, ignoreCase = true)
            matchesCat && matchesQueryAndHashtag(item.post, item.category, queryTrimmed, selectedHashtag)
        }
    }

    val selectedDetailItem = remember(selectedPostId, discoveryItems, feedPosts) {
        selectedPostId?.let { targetId ->
            discoveryItems.firstOrNull { it.id == targetId }?.post
                ?: feedPosts.firstOrNull { it.id == targetId }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val columnCount = when {
            maxWidth >= 900.dp -> 4
            maxWidth >= 600.dp -> 3
            else -> 2
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Sticky Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search posts, creators, #hashtags...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Explore",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty() || selectedHashtag != null) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedHashtag = null
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
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
                        .testTag("explore_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IgBlue,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    singleLine = true
                )
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SampleData.exploreCategories) { category ->
                    val isSelected = category == selectedCategory
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { selectedCategory = category }
                            .testTag("category_chip_$category")
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.5.sp
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Unified Responsive Masonry Staggered Grid
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(columnCount),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("explore_grid"),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                verticalItemSpacing = 10.dp,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Popular Hashtags Section
                if (filteredHashtags.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ExploreSectionHeader(
                                title = "Popular Hashtags",
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Tag,
                                        contentDescription = null,
                                        tint = IgBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("popular_hashtags_row"),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredHashtags, key = { it.tag }) { item ->
                                    val isTagActive = selectedHashtag == item.tag
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isTagActive) IgBlue else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clickable {
                                                selectedHashtag = if (isTagActive) null else item.tag
                                            }
                                            .testTag("hashtag_pill_${item.tag}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.tag,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = if (isTagActive) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = item.postVolume,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isTagActive) {
                                                    Color.White.copy(alpha = 0.85f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Trending Posts Carousel
                if (filteredTrending.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ExploreSectionHeader(
                                title = "Trending Now",
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = IgPink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("trending_posts_row"),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredTrending.size, key = { idx -> "trend_${filteredTrending[idx].id}" }) { index ->
                                    val item = filteredTrending[index]
                                    TrendingPostCard(
                                        rank = index + 1,
                                        post = item.post,
                                        onClick = { selectedPostId = item.id }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Suggested Accounts Carousel
                if (filteredAccounts.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ExploreSectionHeader(
                                title = "Suggested Accounts",
                                subtitle = "Creators you may like"
                            )
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("suggested_accounts_row"),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredAccounts, key = { it.id }) { account ->
                                    val isFollowing = followedUsers[account.id] ?: account.isFollowing
                                    SuggestedAccountCard(
                                        user = account,
                                        isFollowing = isFollowing,
                                        onFollowClick = { onToggleFollowUser(account.id) },
                                        onAccountClick = { onUserClick(account) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Discovery Masonry Section Header
                item(span = StaggeredGridItemSpan.FullLine) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (selectedHashtag != null) {
                                "Discover • $selectedHashtag"
                            } else {
                                "Discover Photos & Videos"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${filteredMasonryItems.size} posts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 5. Masonry Discovery Grid Items or Empty State
                if (filteredMasonryItems.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No matching posts found",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedHashtag = null
                                    selectedCategory = SampleData.exploreCategories[0]
                                }
                            ) {
                                Text("Reset filters", color = IgBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    items(filteredMasonryItems, key = { it.id }) { item ->
                        MasonryDiscoveryTile(
                            item = item,
                            onClick = { selectedPostId = item.id }
                        )
                    }
                }
            }
        }
    }

    // Detailed Post View Dialog when clicking any Trending or Discovery post
    selectedDetailItem?.let { post ->
        val isFeedPost = feedPosts.any { it.id == post.id }
        val isFollowingAuthor = followedUsers[post.user.id] ?: post.user.isFollowing
        ExplorePostDetailDialog(
            post = post,
            isFollowingAuthor = isFollowingAuthor,
            onToggleFollow = { onToggleFollowUser(post.user.id) },
            onLikeClick = {
                if (isFeedPost) {
                    onLikePost(post.id)
                } else {
                    discoveryLikes = discoveryLikes + (post.id to !(discoveryLikes[post.id] ?: false))
                }
            },
            onDoubleTapLike = {
                if (isFeedPost) {
                    onDoubleTapLikePost(post.id)
                } else if (discoveryLikes[post.id] != true) {
                    discoveryLikes = discoveryLikes + (post.id to true)
                }
            },
            onCommentClick = { onCommentPost(post) },
            onShareClick = { onSharePost(post) },
            onSaveClick = {
                if (isFeedPost) {
                    onSavePost(post.id)
                } else {
                    discoverySaves = discoverySaves + (post.id to !(discoverySaves[post.id] ?: false))
                }
            },
            onOptionsClick = { onOptionsPost(post) },
            onAiInsightsClick = { onAiInsightsPost(post) },
            onAddQuickComment = { text -> onAddQuickComment(post.id, text) },
            onLikeComment = { commentId -> onLikeComment(post.id, commentId) },
            onReplyComment = { parent, text -> onReplyComment(post.id, parent, text) },
            onDismiss = { selectedPostId = null }
        )
    }
}

private fun matchesQueryAndHashtag(
    post: Post,
    category: String,
    query: String,
    selectedHashtag: String?
): Boolean {
    val matchesTag = selectedHashtag == null ||
        post.hashtags.any { it.equals(selectedHashtag, ignoreCase = true) } ||
        post.caption.contains(selectedHashtag, ignoreCase = true)
    if (!matchesTag) return false
    if (query.isEmpty()) return true
    return post.user.username.lowercase().contains(query) ||
        post.user.fullName.lowercase().contains(query) ||
        post.caption.lowercase().contains(query) ||
        (post.location?.lowercase()?.contains(query) == true) ||
        category.lowercase().contains(query) ||
        post.hashtags.any { it.lowercase().contains(query) }
}

@Composable
private fun ExploreSectionHeader(
    title: String,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon?.invoke()
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrendingPostCard(
    rank: Int,
    post: Post,
    onClick: () -> Unit
) {
    val formattedLikes = remember(post.likesCount) {
        NumberFormat.getNumberInstance(Locale.US).format(post.likesCount)
    }
    Box(
        modifier = Modifier
            .width(196.dp)
            .height(242.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("trending_post_${post.id}")
    ) {
        AsyncImage(
            model = post.mediaUrls.firstOrNull(),
            contentDescription = "Trending post by ${post.user.username}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Bottom gradient scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.25f),
                            0.45f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Top badges (Rank + Video indicator)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.65f)
            ) {
                Text(
                    text = "#$rank Trending",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (post.isVideo) {
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Bottom author + caption + likes
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = post.user.avatarUrl,
                    contentDescription = post.user.username,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = post.user.username,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = post.caption,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = IgRed,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = formattedLikes,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun SuggestedAccountCard(
    user: User,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onAccountClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.75.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .width(154.dp)
            .clickable(onClick = onAccountClick)
            .testTag("suggested_account_${user.username}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .border(2.dp, InstagramGradient, CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape)
            ) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = user.username,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (user.isVerified) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = IgBlue,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Text(
                text = user.fullName,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onFollowClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) MaterialTheme.colorScheme.surface else IgBlue,
                    contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag("follow_suggested_${user.username}")
            ) {
                Text(
                    text = if (isFollowing) "Following" else "Follow",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun MasonryDiscoveryTile(
    item: ExploreDiscoveryItem,
    onClick: () -> Unit
) {
    val post = item.post
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(item.masonryAspectRatio)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("explore_tile_${item.id}")
    ) {
        AsyncImage(
            model = post.mediaUrls.firstOrNull(),
            contentDescription = "Explore post by ${post.user.username}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Subtle bottom scrim for author tag
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(52.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )

        // Top-right media type badge (Video / Carousel)
        if (post.isVideo) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = post.videoDuration ?: "Reel",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                    )
                }
            }
        } else if (post.mediaUrls.size > 1) {
            Icon(
                imageVector = Icons.Default.Collections,
                contentDescription = "Multiple photos",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(18.dp)
            )
        }

        // Bottom author & likes overlay
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "@${post.user.username}",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = item.category,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
            )
        }
    }
}

@Composable
private fun ExplorePostDetailDialog(
    post: Post,
    isFollowingAuthor: Boolean,
    onToggleFollow: () -> Unit,
    onLikeClick: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onOptionsClick: () -> Unit,
    onAiInsightsClick: () -> Unit,
    onAddQuickComment: (String) -> Unit,
    onLikeComment: (String) -> Unit,
    onReplyComment: (Comment, String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .heightIn(max = 760.dp)
                .testTag("explore_post_detail_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Detail Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Explore Post",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(onClick = onToggleFollow) {
                            Text(
                                text = if (isFollowingAuthor) "Following" else "Follow",
                                color = if (isFollowingAuthor) MaterialTheme.colorScheme.onSurfaceVariant else IgBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("close_explore_detail_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close detail view",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), thickness = 0.5.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    PostCard(
                        post = post,
                        onLikeClick = onLikeClick,
                        onDoubleTapLike = onDoubleTapLike,
                        onCommentClick = onCommentClick,
                        onShareClick = onShareClick,
                        onSaveClick = onSaveClick,
                        onOptionsClick = onOptionsClick,
                        onAiInsightsClick = onAiInsightsClick,
                        onUserClick = {},
                        onAddQuickComment = onAddQuickComment,
                        onLikeComment = onLikeComment,
                        onReplyComment = onReplyComment
                    )
                }
            }
        }
    }
}
