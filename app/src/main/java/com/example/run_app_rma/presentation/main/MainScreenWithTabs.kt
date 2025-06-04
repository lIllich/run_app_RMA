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
import androidx.compose.material.icons.filled.UploadFile // New icon for "Objavi"
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
import com.example.run_app_rma.presentation.feed.FeedScreen
import com.example.run_app_rma.presentation.follow.FollowScreen
import com.example.run_app_rma.presentation.follow.FollowViewModel
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
    FOLLOW("Prati", Icons.Default.People),
    RUNNING("Trčanje", Icons.AutoMirrored.Filled.DirectionsRun),
    PUBLISH("Objavi", Icons.Default.UploadFile), // New tab for publishing runs
    PROFILE("Profil", Icons.Default.AccountCircle)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreenWithTabs(
    modifier: Modifier = Modifier,
    runViewModel: RunViewModel,
    userRepository: UserRepository,
    firebaseAuth: FirebaseAuth,
    authRepository: AuthRepository,
    onLogout: () -> Unit,
    onEditProfile: (String) -> Unit,
    // Pass RunPostRepository and AppDatabase instance
    runPostRepository: RunPostRepository,
    appDatabase: com.example.run_app_rma.data.db.AppDatabase // Fully qualify AppDatabase
) {
    val pagerState = rememberPagerState(initialPage = TabScreen.RUNNING.ordinal) {
        TabScreen.values().size
    }
    val scope = rememberCoroutineScope()

    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(
            userRepository = userRepository,
            firebaseAuth = firebaseAuth,
            authRepository = authRepository
        )
    )

    val followViewModel: FollowViewModel = viewModel(
        factory = FollowViewModel.Factory(
            userRepository = userRepository,
            followRepository = FollowRepository(FirebaseFirestore.getInstance()),
            firebaseAuth = firebaseAuth
        )
    )

    // Initialize PublishRunViewModel
    val publishRunViewModel: PublishRunViewModel = viewModel(
        factory = PublishRunViewModel.Factory(
            runDao = appDatabase.runDao(), // Pass the runDao from AppDatabase
            runPostRepository = runPostRepository, // Pass the RunPostRepository
            userRepository = userRepository, // Pass UserRepository
            firebaseAuth = firebaseAuth // Pass FirebaseAuth
        )
    )

    LaunchedEffect(pagerState.currentPage) {
        when (TabScreen.values()[pagerState.currentPage]) {
            TabScreen.PROFILE -> profileViewModel.fetchUserProfile()
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
                TabScreen.FEED -> FeedScreen()
                TabScreen.FOLLOW -> FollowScreen(followViewModel = followViewModel)
                TabScreen.RUNNING -> RunningScreen(runViewModel = runViewModel)
                TabScreen.PUBLISH -> PublishRunScreen(publishRunViewModel = publishRunViewModel) // New tab screen
                TabScreen.PROFILE -> ProfileScreen(
                    profileViewModel = profileViewModel,
                    onLogout = onLogout,
                    onEditProfile = onEditProfile
                )
            }
        }
    }
}