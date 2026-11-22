package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.Post
import com.example.model.Story
import com.example.model.User
import com.example.ui.components.PostCard
import com.example.ui.components.StoriesTray
import com.example.ui.theme.IgBlue

@Composable
fun HomeScreen(
    currentUser: User,
    stories: List<Story>,
    posts: List<Post>,
    isLoadingMore: Boolean,
    onStoryClick: (Story) -> Unit,
    onAddStoryClick: () -> Unit,
    onLikeClick: (String) -> Unit,
    onDoubleTapLike: (String) -> Unit,
    onCommentClick: (Post) -> Unit,
    onShareClick: (Post) -> Unit,
    onSaveClick: (String) -> Unit,
    onOptionsClick: (Post) -> Unit,
    onAiInsightsClick: (Post) -> Unit,
    onUserClick: (User) -> Unit,
    onAddQuickComment: (String, String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Infinite scroll detection: when approaching the end of the list, load more
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 2
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isLoadingMore) {
            onLoadMore()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .testTag("home_feed_list")
        ) {
            // Stories Tray at Top
            item {
                StoriesTray(
                    currentUser = currentUser,
                    stories = stories,
                    onStoryClick = onStoryClick,
                    onAddStoryClick = onAddStoryClick
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    thickness = 0.5.dp
                )
            }

            // Feed Posts
            items(posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    onLikeClick = { onLikeClick(post.id) },
                    onDoubleTapLike = { onDoubleTapLike(post.id) },
                    onCommentClick = { onCommentClick(post) },
                    onShareClick = { onShareClick(post) },
                    onSaveClick = { onSaveClick(post.id) },
                    onOptionsClick = { onOptionsClick(post) },
                    onAiInsightsClick = { onAiInsightsClick(post) },
                    onUserClick = { onUserClick(post.user) },
                    onAddQuickComment = { text -> onAddQuickComment(post.id, text) }
                )
            }

            // Loading more spinner at bottom of feed
            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = IgBlue,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "You're all caught up ✨",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
