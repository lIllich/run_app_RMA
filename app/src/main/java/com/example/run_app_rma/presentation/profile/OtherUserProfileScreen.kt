package com.example.run_app_rma.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserProfileScreen(
    modifier: Modifier = Modifier,
    otherUserProfileViewModel: OtherUserProfileViewModel = viewModel(factory = OtherUserProfileViewModel.Factory),
    onBack: () -> Unit,
    onEditProfile: (String) -> Unit,    // if it's the current user's profile
    onLogout: () -> Unit,               // if it's the current user's profile
    onViewUserPosts: (String) -> Unit,  // posts of this user
    onViewFollowing: (String) -> Unit,  // who this user follows
    onViewFollowers: (String) -> Unit,  // who follows this user
    onUserClick: (String) -> Unit
) {
    val viewedUser by otherUserProfileViewModel.viewedUser.collectAsState(initial = null)
    val isLoading by otherUserProfileViewModel.isLoading.collectAsState(initial = false)
    val errorMessage by otherUserProfileViewModel.errorMessage.collectAsState(initial = null)
    val followingCount by otherUserProfileViewModel.followingCount.collectAsState()
    val followersCount by otherUserProfileViewModel.followersCount.collectAsState()
    val viewedUserPosts by otherUserProfileViewModel.viewedUserPosts.collectAsState()
//    val userLikedPostIds by otherUserProfileViewModel.userLikedPostIds.collectAsState()
    val isFollowingViewedUser by otherUserProfileViewModel.isFollowingViewedUser.collectAsState() // New: Observe follow status
    val isTogglingFollow by otherUserProfileViewModel.isTogglingFollow.collectAsState() // New: Observe button loading

    val currentLoggedInUserId = otherUserProfileViewModel.currentLoggedInUserId
    val isCurrentUserProfile = viewedUser?.id == currentLoggedInUserId

//    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
//    val decimalFormat = DecimalFormat("#.##")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = viewedUser?.displayName ?: "Profil korisnika") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                Text("Učitavanje profila...")
            } else if (errorMessage != null) {
                Text("Greška: $errorMessage", color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewedUser?.id?.let { otherUserProfileViewModel.fetchUserProfileAndData(it) } }) {
                    Text("Pokušaj ponovo")
                }
            } else if (viewedUser != null) {
                // reusing UserProfileContent for displaying basic profile details
                UserProfileContent(user = viewedUser!!)
                Spacer(modifier = Modifier.height(32.dp))

                // conditional buttons for current user or follow button for other users
                if (isCurrentUserProfile) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Button(
                            onClick = { viewedUser?.let { onEditProfile(it.id) } },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text("Uredi profil")
                        }
                        Button(
                            onClick = { onLogout() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        ) {
                            Text("Odjavi se")
                        }
                    }
                } else {
                    // show follow button for other users
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isTogglingFollow) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Button(
                            onClick = { otherUserProfileViewModel.toggleFollowViewedUser() },
                            enabled = !isTogglingFollow, // Disable button while loading
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowingViewedUser)
                                    MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.primary,
                                contentColor = if (isFollowingViewedUser)
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(if (isFollowingViewedUser) "Pratim" else "Prati")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // buttons for posts, following, and followers (always visible)
                Button(
                    onClick = { viewedUser?.let { onViewUserPosts(it.id) } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Objave (${viewedUserPosts.size})")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(
                        onClick = { viewedUser?.let { onViewFollowing(it.id) } },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$followingCount", style = MaterialTheme.typography.titleLarge)
                            Text("Pratim", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(
                        onClick = { viewedUser?.let { onViewFollowers(it.id) } },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$followersCount", style = MaterialTheme.typography.titleLarge)
                            Text("Pratitelji", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Text(text = "Korisnik nije pronađen.")
            }
        }
    }
}