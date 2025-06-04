package com.example.run_app_rma.presentation.follow

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.run_app_rma.data.firestore.model.User
import com.example.run_app_rma.data.firestore.repository.FollowRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.run_app_rma.presentation.common.UserCard // Import the reusable UserCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowScreen(
    modifier: Modifier = Modifier,
    followViewModel: FollowViewModel = viewModel(
        factory = FollowViewModel.Factory(
            userRepository = UserRepository(FirebaseFirestore.getInstance()),
            followRepository = FollowRepository(FirebaseFirestore.getInstance()),
            firebaseAuth = FirebaseAuth.getInstance()
        )
    ),
    onUserClick: (String) -> Unit // New parameter: Lambda to navigate to another user's profile
) {
    val searchQuery by followViewModel.searchQuery.collectAsState()
    val users = followViewModel.users
    val isLoading by followViewModel.isLoading // This now primarily reflects search loading
    val errorMessage by followViewModel.errorMessage
    val isFollowingMap = followViewModel.isFollowingMap
    val isTogglingFollowMap = followViewModel.isTogglingFollowMap // New: Observe individual toggle loading
    val currentLoggedInUserId = followViewModel.currentUserId // Get current user ID from ViewModel

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { followViewModel.onSearchQueryChanged(it) },
            label = { Text("Search Users") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Show main CircularProgressIndicator only when searching
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }

        if (users.isEmpty() && searchQuery.isNotBlank() && !isLoading) {
            Text(
                text = "No users found for \"$searchQuery\"",
                modifier = Modifier.padding(16.dp)
            )
        } else if (users.isEmpty() && searchQuery.isBlank() && !isLoading) {
            Text(
                text = "Start typing to search for users",
                modifier = Modifier.padding(16.dp)
            )
        }


        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users, key = { it.id }) { user ->
                // Ensure the follow button is not shown for the current user's own card
                val showFollowBtn = user.id != currentLoggedInUserId
                UserCard(
                    user = user,
                    onClick = onUserClick, // Pass the onUserClick lambda here
                    showFollowButton = showFollowBtn,
                    isFollowing = isFollowingMap[user.id] ?: false,
                    onToggleFollow = { userIdToToggle, isCurrentlyFollowing ->
                        followViewModel.toggleFollow(userIdToToggle)
                    },
                    isTogglingFollow = isTogglingFollowMap[user.id] ?: false // Pass individual toggle loading state
                )
            }
        }
    }
}
