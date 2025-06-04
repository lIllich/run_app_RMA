package com.example.run_app_rma.presentation.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Upload // Import for Publish tab
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.run_app_rma.data.firestore.repository.FollowRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.example.run_app_rma.data.remote.AuthRepository
import com.example.run_app_rma.presentation.feed.FeedScreen
import com.example.run_app_rma.presentation.feed.FeedViewModel
import com.example.run_app_rma.presentation.follow.FollowScreen
import com.example.run_app_rma.presentation.follow.FollowViewModel
import com.example.run_app_rma.presentation.profile.ProfileScreen
import com.example.run_app_rma.presentation.profile.ProfileViewModel
import com.example.run_app_rma.presentation.publish.PublishRunScreen
import com.example.run_app_rma.presentation.publish.PublishRunViewModel
import com.example.run_app_rma.presentation.track.RunningScreen
import com.example.run_app_rma.presentation.track.RunViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

// Corrected order of tabs
enum class TabScreen(val title: String, val icon: ImageVector) {
    FEED("Feed", Icons.Default.DynamicFeed),
    FOLLOW("Prati", Icons.Default.People),
    RUNNING("Trčanje", Icons.AutoMirrored.Filled.DirectionsRun),
    PUBLISH("Objavi", Icons.Default.Upload), // Added Publish tab
    PROFILE("Profil", Icons.Default.AccountCircle)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreenWithTabs(
    modifier: Modifier = Modifier,
    runViewModel: RunViewModel,
    profileViewModel: ProfileViewModel,
    followViewModel: FollowViewModel,
    publishRunViewModel: PublishRunViewModel,
    feedViewModel: FeedViewModel,
    onLogout: () -> Unit,
    onEditProfile: (String) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = TabScreen.FEED.ordinal) {
        TabScreen.values().size
    }
    val scope = rememberCoroutineScope()

    // When the Profile tab becomes visible, refresh the user profile data
    LaunchedEffect(pagerState.currentPage) {
        when (TabScreen.values()[pagerState.currentPage]) {
            TabScreen.PROFILE -> profileViewModel.fetchUserProfileAndPosts()
            TabScreen.FEED -> feedViewModel.loadFeedPosts()
            TabScreen.PUBLISH -> publishRunViewModel.loadLocalRuns() // Load runs when Publish tab is selected
            else -> { /* Do nothing for other tabs */ }
        }
    }

    Scaffold(
        bottomBar = {
            TabRow(
                selectedTabIndex = pagerState.currentPage
            ) {
                TabScreen.values().forEachIndexed { index, screen ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(screen.title) },
                        icon = { Icon(screen.icon, contentDescription = screen.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = modifier.padding(innerPadding)
        ) { page ->
            when (TabScreen.values()[page]) {
                TabScreen.FEED -> FeedScreen(feedViewModel = feedViewModel)
                TabScreen.FOLLOW -> FollowScreen(followViewModel = followViewModel)
                TabScreen.RUNNING -> RunningScreen(runViewModel = runViewModel)
                TabScreen.PUBLISH -> PublishRunScreen(publishRunViewModel = publishRunViewModel) // Render PublishScreen
                TabScreen.PROFILE -> ProfileScreen(
                    profileViewModel = profileViewModel,
                    onLogout = onLogout,
                    onEditProfile = onEditProfile
                )
            }
        }
    }
}
