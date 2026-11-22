package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Story
import com.example.model.User
import com.example.ui.theme.InstagramGradient

@Composable
fun StoriesTray(
    currentUser: User,
    stories: List<Story>,
    onStoryClick: (Story) -> Unit,
    onAddStoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seenBorderColor = MaterialTheme.colorScheme.outline

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stories_tray"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(stories, key = { it.id }) { story ->
            val avatarUrl = if (story.isYourStory) currentUser.avatarUrl else story.userAvatar
            StoryItem(
                story = story,
                avatarUrl = avatarUrl,
                seenBorderColor = seenBorderColor,
                onClick = { onStoryClick(story) },
                onAddStoryClick = onAddStoryClick
            )
        }
    }
}

@Composable
private fun StoryItem(
    story: Story,
    avatarUrl: String,
    seenBorderColor: Color,
    onClick: () -> Unit,
    onAddStoryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clickable(onClick = onClick)
            .testTag("story_item_${story.username}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(74.dp),
            contentAlignment = Alignment.Center
        ) {
            val showGradientRing = story.hasUnseenStory || story.isYourStory
            val ringModifier = when {
                story.hasUnseenStory -> Modifier
                    .fillMaxSize()
                    .border(2.5.dp, InstagramGradient, CircleShape)
                story.isYourStory -> Modifier
                    .fillMaxSize()
                    .border(1.5.dp, seenBorderColor, CircleShape)
                else -> Modifier
                    .fillMaxSize()
                    .border(1.5.dp, seenBorderColor, CircleShape)
            }

            Box(
                modifier = ringModifier,
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (showGradientRing) 66.dp else 66.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(3.dp)
                ) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = if (story.isYourStory) "Your story" else "${story.username} story",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            if (story.isYourStory) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-1).dp, y = (-1).dp)
                        .size(24.dp)
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

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = if (story.isYourStory) "Your story" else story.username,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = if (story.hasUnseenStory) FontWeight.Medium else FontWeight.Normal
            ),
            color = if (story.hasUnseenStory || story.isYourStory) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
