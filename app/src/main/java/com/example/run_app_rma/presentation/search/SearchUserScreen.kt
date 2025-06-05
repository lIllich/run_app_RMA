package com.example.run_app_rma.presentation.search // New package name

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.run_app_rma.R
import com.example.run_app_rma.data.firestore.model.User
import com.example.run_app_rma.data.firestore.repository.FollowRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.run_app_rma.presentation.common.UserCard // Import the reusable UserCard
import androidx.compose.material.icons.filled.Search // Import for Search icon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUserScreen( // Renamed from FollowScreen
    modifier: Modifier = Modifier,
    searchUserViewModel: SearchUserViewModel = viewModel( // Renamed from followViewModel
        factory = SearchUserViewModel.Factory( // Renamed from FollowViewModel.Factory
            userRepository = UserRepository(FirebaseFirestore.getInstance()),
            followRepository = FollowRepository(FirebaseFirestore.getInstance()),
            firebaseAuth = FirebaseAuth.getInstance()
        )
    ),
    onUserClick: (String) -> Unit // New parameter: Lambda to navigate to another user's profile
) {
    val searchQuery by searchUserViewModel.searchQuery.collectAsState()
    val users = searchUserViewModel.users
    val isLoading by searchUserViewModel.isLoading // This now primarily reflects search loading
    val errorMessage by searchUserViewModel.errorMessage
    val isFollowingMap = searchUserViewModel.isFollowingMap
    val isTogglingFollowMap = searchUserViewModel.isTogglingFollowMap // New: Observe individual toggle loading
    val currentLoggedInUserId = searchUserViewModel.currentUserId // Get current user ID from ViewModel

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchUserViewModel.onSearchQueryChanged(it) }, // Renamed from followViewModel
            label = { Text("Pretraži korisnike") }, // Changed label
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
                text = "Nema pronađenih korisnika za \"$searchQuery\"", // Changed text
                modifier = Modifier.padding(16.dp)
            )
        } else if (users.isEmpty() && searchQuery.isBlank() && !isLoading) {
            Text(
                text = "Počnite upisivati za pretraživanje korisnika", // Changed text
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
                        searchUserViewModel.toggleFollow(userIdToToggle) // Renamed from followViewModel
                    },
                    isTogglingFollow = isTogglingFollowMap[user.id] ?: false // Pass individual toggle loading state
                )
            }
        }
    }
}
