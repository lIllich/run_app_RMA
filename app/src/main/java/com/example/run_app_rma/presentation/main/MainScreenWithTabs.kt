// MainScreenWithTabs.kt
package com.example.run_app_rma.presentation.main

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.UploadFile // Keep this for "Objavi"
import androidx.compose.material.icons.filled.Search // Import for Search icon
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
import com.example.run_app_rma.data.firestore.repository.RunPostRepository // Import RunPostRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.example.run_app_rma.data.remote.AuthRepository
import com.example.run_app_rma.presentation.feed.FeedScreen // Import FeedScreen
import com.example.run_app_rma.presentation.feed.FeedViewModel // Import FeedViewModel
import com.example.run_app_rma.presentation.search.SearchUserScreen // Renamed from FollowScreen
import com.example.run_app_rma.presentation.search.SearchUserViewModel // Renamed from FollowViewModel
import com.example.run_app_rma.presentation.profile.ProfileScreen
import com.example.run_app_rma.presentation.profile.ProfileViewModel
import com.example.run_app_rma.presentation.publish.PublishRunScreen // Import PublishRunScreen
import com.example.run_app_rma.presentation.publish.PublishRunViewModel // Import PublishRunViewModel
import com.example.run_app_rma.presentation.track.RunningScreen
import com.example.run_app_rma.presentation.track.RunViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

enum class TabScreen(val title: String, val icon: ImageVector) {
    FEED("Feed", Icons.Default.DynamicFeed),
    SEARCH("Traži", Icons.Default.Search),
    RUNNING("Trčanje", Icons.AutoMirrored.Filled.DirectionsRun),
    PUBLISH("Objavi", Icons.Default.ArrowUpward),
    PROFILE("Profil", Icons.Default.AccountCircle)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreenWithTabs(
    modifier: Modifier = Modifier,
    application: Application,
    runViewModel: RunViewModel,
    userRepository: UserRepository,
    firebaseAuth: FirebaseAuth,
    authRepository: AuthRepository,
    onLogout: () -> Unit,
    onEditProfile: (String) -> Unit,
    runPostRepository: RunPostRepository, // Passed from MainActivity
    appDatabase: com.example.run_app_rma.data.db.AppDatabase, // Passed from MainActivity
    // New parameters to pass down navigation actions
    onViewUserPosts: (String) -> Unit,
    onViewFollowing: (String) -> Unit,
    onViewFollowers: (String) -> Unit,
    onUserClick: (String) -> Unit, // For UserCard clicks in FollowScreen and RunPostCard clicks
    onPostClick: (String) -> Unit, // Added onPostClick parameter
    onRunClick: (Long) -> Unit // Added onRunClick parameter for PublishRunScreen
) {
    val pagerState = rememberPagerState(initialPage = TabScreen.FEED.ordinal) { // Set initial page to Feed
        TabScreen.values().size
    }
    val scope = rememberCoroutineScope()

    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(
            userRepository = userRepository,
            firebaseAuth = firebaseAuth,
            authRepository = authRepository,
            runPostRepository = runPostRepository,
            followRepository = FollowRepository(FirebaseFirestore.getInstance())
        )
    )

    val searchUserViewModel: SearchUserViewModel = viewModel( // Renamed from followViewModel
        factory = SearchUserViewModel.Factory( // Renamed from FollowViewModel.Factory
            userRepository = userRepository,
            followRepository = FollowRepository(FirebaseFirestore.getInstance()),
            firebaseAuth = firebaseAuth
        )
    )

    val publishRunViewModel: PublishRunViewModel = viewModel(
        factory = PublishRunViewModel.Factory(
            runDao = appDatabase.runDao(),
            runPostRepository = runPostRepository,
            userRepository = userRepository,
            firebaseAuth = firebaseAuth
        )
    )

    val feedViewModel: FeedViewModel = viewModel(
        factory = FeedViewModel.Factory(
            application = application,
            runPostRepository = runPostRepository,
            userRepository = userRepository,
            firebaseAuth = firebaseAuth,
            followRepository = FollowRepository(FirebaseFirestore.getInstance())
        )
    )

    // Observe changes in the selected tab and refresh data accordingly
    LaunchedEffect(pagerState.currentPage) {
        when (TabScreen.values()[pagerState.currentPage]) {
            TabScreen.FEED -> feedViewModel.loadFeedPosts() // Load posts when Feed tab is selected
            TabScreen.PROFILE -> profileViewModel.fetchUserProfileAndCounts()
            TabScreen.PUBLISH -> publishRunViewModel.loadLocalRuns()
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
                TabScreen.FEED -> FeedScreen(
                    feedViewModel = feedViewModel,
                    onUserClick = onUserClick, // Pass the onUserClick lambda
                    onPostClick = onPostClick // Pass onPostClick to FeedScreen
                )
                TabScreen.RUNNING -> RunningScreen(runViewModel = runViewModel)
                TabScreen.SEARCH -> SearchUserScreen( // Renamed from FollowScreen
                    searchUserViewModel = searchUserViewModel, // Renamed from followViewModel
                    onUserClick = onUserClick // Pass the onUserClick lambda
                )
                TabScreen.PUBLISH -> PublishRunScreen(
                    publishRunViewModel = publishRunViewModel,
                    onRunClick = onRunClick // Pass onRunClick to PublishRunScreen
                )
                TabScreen.PROFILE -> ProfileScreen(
                    profileViewModel = profileViewModel,
                    onLogout = onLogout,
                    onEditProfile = onEditProfile,
                    onViewUserPosts = onViewUserPosts,
                    onViewFollowing = onViewFollowing,
                    onViewFollowers = onViewFollowers,
                    onUserClick = onUserClick // Pass the onUserClick lambda to ProfileScreen for its posts
                )
            }
        }
    }
}