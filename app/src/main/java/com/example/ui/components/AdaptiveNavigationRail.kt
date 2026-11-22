package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.NavigationTab
import com.example.model.User
import com.example.ui.theme.GrandHotelFontFamily

@Composable
fun AdaptiveNavigationRail(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    currentUser: User,
    onOpenNotifications: () -> Unit,
    onOpenMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        header = {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "InstaSnap",
                fontFamily = GrandHotelFontFamily,
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NavigationRailItem(
                    selected = selectedTab == NavigationTab.HOME,
                    onClick = { onTabSelected(NavigationTab.HOME) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home", fontSize = 11.sp) },
                    modifier = Modifier.testTag("rail_nav_home")
                )

                NavigationRailItem(
                    selected = selectedTab == NavigationTab.EXPLORE,
                    onClick = { onTabSelected(NavigationTab.EXPLORE) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Explore"
                        )
                    },
                    label = { Text("Explore", fontSize = 11.sp) },
                    modifier = Modifier.testTag("rail_nav_explore")
                )

                NavigationRailItem(
                    selected = selectedTab == NavigationTab.CREATE,
                    onClick = { onTabSelected(NavigationTab.CREATE) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.CREATE) Icons.Filled.AddBox else Icons.Outlined.AddBox,
                            contentDescription = "Create"
                        )
                    },
                    label = { Text("Create", fontSize = 11.sp) },
                    modifier = Modifier.testTag("rail_nav_create")
                )

                NavigationRailItem(
                    selected = selectedTab == NavigationTab.REELS,
                    onClick = { onTabSelected(NavigationTab.REELS) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.REELS) Icons.Filled.Movie else Icons.Outlined.Movie,
                            contentDescription = "Reels"
                        )
                    },
                    label = { Text("Reels", fontSize = 11.sp) },
                    modifier = Modifier.testTag("rail_nav_reels")
                )

                NavigationRailItem(
                    selected = false,
                    onClick = onOpenMessages,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Send,
                            contentDescription = "Messages"
                        )
                    },
                    label = { Text("Messages", fontSize = 11.sp) },
                    modifier = Modifier.testTag("rail_nav_messages")
                )

                NavigationRailItem(
                    selected = false,
                    onClick = onOpenNotifications,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = "Notifications"
                        )
                    },
                    label = { Text("Notifications", fontSize = 11.sp) },
                    modifier = Modifier.testTag("rail_nav_notifications")
                )
            }

            // Bottom Profile Item
            Box(
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .size(40.dp)
                    .clickable { onTabSelected(NavigationTab.PROFILE) }
                    .then(
                        if (selectedTab == NavigationTab.PROFILE) {
                            Modifier
                                .border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                .padding(2.dp)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
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
