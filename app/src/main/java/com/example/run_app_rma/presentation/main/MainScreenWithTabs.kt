package com.example.run_app_rma.presentation.main

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
import com.example.run_app_rma.data.firestore.repository.RunPostRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.example.run_app_rma.data.remote.AuthRepository
import com.example.run_app_rma.presentation.feed.FeedScreen
import com.example.run_app_rma.presentation.feed.FeedViewModel
import com.example.run_app_rma.presentation.profile.ProfileScreen
import com.example.run_app_rma.presentation.profile.ProfileViewModel
import com.example.run_app_rma.presentation.publish.PublishRunScreen
import com.example.run_app_rma.presentation.publish.PublishRunViewModel
import com.example.run_app_rma.presentation.search.SearchUserScreen
import com.example.run_app_rma.presentation.search.SearchUserViewModel
import com.example.run_app_rma.presentation.track.RunViewModel
import com.example.run_app_rma.presentation.track.RunningScreen
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
    runPostRepository: RunPostRepository,
    appDatabase: com.example.run_app_rma.data.db.AppDatabase,
    onViewUserPosts: (String) -> Unit,
    onViewFollowing: (String) -> Unit,
    onViewFollowers: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onRunClick: (Long) -> Unit
) {
    // set initial page to feed
    val pagerState = rememberPagerState(initialPage = TabScreen.FEED.ordinal) {
        TabScreen.entries.size
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

    val searchUserViewModel: SearchUserViewModel = viewModel(
        factory = SearchUserViewModel.Factory(
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

    // observe changes in the selected tab and refresh data accordingly
    LaunchedEffect(pagerState.currentPage) {
        when (TabScreen.entries[pagerState.currentPage]) {
            TabScreen.FEED -> feedViewModel.loadFeedPosts() // load posts when feed tab is selected
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
                TabScreen.entries.forEachIndexed { index, screen ->
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
            when (TabScreen.entries[page]) {
                TabScreen.FEED -> FeedScreen(
                    feedViewModel = feedViewModel,
                    onUserClick = onUserClick,
                    onPostClick = onPostClick
                )
                TabScreen.RUNNING -> RunningScreen(runViewModel = runViewModel)
                TabScreen.SEARCH -> SearchUserScreen(
                    searchUserViewModel = searchUserViewModel,
                    onUserClick = onUserClick
                )
                TabScreen.PUBLISH -> PublishRunScreen(
                    publishRunViewModel = publishRunViewModel,
                    onRunClick = onRunClick
                )
                TabScreen.PROFILE -> ProfileScreen(
                    profileViewModel = profileViewModel,
                    onLogout = onLogout,
                    onEditProfile = onEditProfile,
                    onViewUserPosts = onViewUserPosts,
                    onViewFollowing = onViewFollowing,
                    onViewFollowers = onViewFollowers,
                    onUserClick = onUserClick
                )
            }
        }
    }
}