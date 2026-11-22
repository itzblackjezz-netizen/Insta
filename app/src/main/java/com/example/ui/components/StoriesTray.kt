package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Story
import com.example.model.User
import com.example.ui.theme.InstagramGradient
import com.example.ui.theme.InstagramStorySeenRing
import com.example.ui.theme.InstagramStorySeenRingDark

@Composable
fun StoriesTray(
    currentUser: User,
    stories: List<Story>,
    onStoryClick: (Story) -> Unit,
    onAddStoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val seenBorderColor = if (isDark) InstagramStorySeenRingDark else InstagramStorySeenRing

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stories_tray"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(stories, key = { it.id }) { story ->
            StoryItem(
                story = story,
                isDark = isDark,
                seenBorderColor = seenBorderColor,
                onClick = {
                    if (story.isYourStory) {
                        onStoryClick(story)
                    } else {
                        onStoryClick(story)
                    }
                },
                onAddStoryClick = onAddStoryClick
            )
        }
    }
}

@Composable
private fun StoryItem(
    story: Story,
    isDark: Boolean,
    seenBorderColor: Color,
    onClick: () -> Unit,
    onAddStoryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(74.dp)
            .clickable(onClick = onClick)
            .testTag("story_item_${story.username}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ring Border
            val borderModifier = when {
                story.isYourStory -> Modifier
                story.hasUnseenStory -> Modifier
                    .fillMaxSize()
                    .border(2.5.dp, InstagramGradient, CircleShape)
                else -> Modifier
                    .fillMaxSize()
                    .border(1.5.dp, seenBorderColor, CircleShape)
            }

            Box(
                modifier = borderModifier,
                contentAlignment = Alignment.Center
            ) {
                // Gap between border and avatar
                Box(
                    modifier = Modifier
                        .size(if (story.isYourStory) 68.dp else 64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(if (story.isYourStory) 0.dp else 2.5.dp)
                ) {
                    AsyncImage(
                        model = story.userAvatar,
                        contentDescription = "${story.username} story",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // If "Your Story", show plus badge with gradient
            if (story.isYourStory) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(InstagramGradient)
                        .clickable(onClick = onAddStoryClick)
                        .testTag("add_story_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Story",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Username Label
        Text(
            text = if (story.isYourStory) "Your story" else story.username,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
