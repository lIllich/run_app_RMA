package com.example.run_app_rma.presentation.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect // IMPORT LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.example.run_app_rma.data.remote.AuthRepository
import com.example.run_app_rma.presentation.feed.FeedScreen
import com.example.run_app_rma.presentation.follow.FollowScreen
import com.example.run_app_rma.presentation.profile.ProfileScreen
import com.example.run_app_rma.presentation.profile.ProfileViewModel
import com.example.run_app_rma.presentation.track.RunningScreen // IMPORT RunningScreen
import com.example.run_app_rma.presentation.track.RunViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore // Import FirebaseFirestore for ProfileViewModel Factory
import kotlinx.coroutines.launch

enum class TabScreen(val title: String, val icon: ImageVector) {
    FEED("Feed", Icons.Default.DynamicFeed),
    RUNNING("Trčanje", Icons.Default.DirectionsRun), // Changed to "Trčanje" for consistency
    FOLLOW("Prati", Icons.Default.People), // Changed to "Prati" for consistency
    PROFILE("Profil", Icons.Default.AccountCircle) // Changed to "Profil" for consistency
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreenWithTabs(
    modifier: Modifier = Modifier,
    runViewModel: RunViewModel,
    userRepository: UserRepository,
    firebaseAuth: FirebaseAuth,
    authRepository: AuthRepository, // This is the AuthRepository instance from MainActivity
    onLogout: () -> Unit,
    onEditProfile: (String) -> Unit
) {
    // Start on Profile tab as initial page if that's desired behavior.
    // Given the previous code, it started on "RUNNING.ordinal", so keeping that for now.
    val pagerState = rememberPagerState(initialPage = TabScreen.RUNNING.ordinal) {
        TabScreen.values().size
    }
    val scope = rememberCoroutineScope()

    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(
            userRepository = userRepository,
            firebaseAuth = firebaseAuth,
            authRepository = authRepository // Pass the correct AuthRepository instance
        )
    )

    // Observe changes in the selected tab and refresh profile data when the Profile tab is selected
    LaunchedEffect(pagerState.currentPage) {
        if (TabScreen.values()[pagerState.currentPage] == TabScreen.PROFILE) {
            profileViewModel.fetchUserProfile()
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
                TabScreen.RUNNING -> RunningScreen(runViewModel = runViewModel) // Corrected: RunningScreen
                TabScreen.FOLLOW -> FollowScreen()
                TabScreen.PROFILE -> ProfileScreen(
                    profileViewModel = profileViewModel,
                    onLogout = onLogout,
                    onEditProfile = onEditProfile
                )
            }
        }
    }
}