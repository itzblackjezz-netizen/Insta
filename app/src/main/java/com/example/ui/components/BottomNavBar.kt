package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.NavigationTab
import com.example.model.User

@Composable
fun InstagramBottomNavBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    currentUser: User,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                thickness = 0.5.dp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(52.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Home
                BottomNavItem(
                    isSelected = selectedTab == NavigationTab.HOME,
                    onClick = { onTabSelected(NavigationTab.HOME) },
                    testTag = "nav_home"
                ) {
                    Icon(
                        imageVector = if (selectedTab == NavigationTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = "Home",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Explore
                BottomNavItem(
                    isSelected = selectedTab == NavigationTab.EXPLORE,
                    onClick = { onTabSelected(NavigationTab.EXPLORE) },
                    testTag = "nav_explore"
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Create
                BottomNavItem(
                    isSelected = selectedTab == NavigationTab.CREATE,
                    onClick = { onTabSelected(NavigationTab.CREATE) },
                    testTag = "nav_create"
                ) {
                    Icon(
                        imageVector = if (selectedTab == NavigationTab.CREATE) Icons.Filled.AddBox else Icons.Outlined.AddBox,
                        contentDescription = "Create",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Reels
                BottomNavItem(
                    isSelected = selectedTab == NavigationTab.REELS,
                    onClick = { onTabSelected(NavigationTab.REELS) },
                    testTag = "nav_reels"
                ) {
                    Icon(
                        imageVector = if (selectedTab == NavigationTab.REELS) Icons.Filled.Movie else Icons.Outlined.Movie,
                        contentDescription = "Reels",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Profile Avatar
                val isProfileSelected = selectedTab == NavigationTab.PROFILE
                BottomNavItem(
                    isSelected = isProfileSelected,
                    onClick = { onTabSelected(NavigationTab.PROFILE) },
                    testTag = "nav_profile"
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .then(
                                if (isProfileSelected) {
                                    Modifier
                                        .border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                        .padding(2.dp)
                                } else Modifier
                            )
                    ) {
                        AsyncImage(
                            model = currentUser.avatarUrl,
                            contentDescription = "Profile",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
