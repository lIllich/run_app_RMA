package com.example.run_app_rma.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.run_app_rma.data.firestore.repository.FollowRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.example.run_app_rma.presentation.common.UserCard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SearchUserScreen(
    modifier: Modifier = Modifier,
    searchUserViewModel: SearchUserViewModel = viewModel(
        factory = SearchUserViewModel.Factory(
            userRepository = UserRepository(FirebaseFirestore.getInstance()),
            followRepository = FollowRepository(FirebaseFirestore.getInstance()),
            firebaseAuth = FirebaseAuth.getInstance()
        )
    ),
    onUserClick: (String) -> Unit
) {
    val searchQuery by searchUserViewModel.searchQuery.collectAsState()
    val users = searchUserViewModel.users
    val isLoading by searchUserViewModel.isLoading
    val errorMessage by searchUserViewModel.errorMessage
    val isFollowingMap = searchUserViewModel.isFollowingMap
    val isTogglingFollowMap = searchUserViewModel.isTogglingFollowMap
    val currentLoggedInUserId = searchUserViewModel.currentUserId

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchUserViewModel.onSearchQueryChanged(it) },
            label = { Text("Pretraži korisnike") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // show main CircularProgressIndicator only when searching
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
                text = "Nema pronađenih korisnika za \"$searchQuery\"",
                modifier = Modifier.padding(16.dp)
            )
        } else if (users.isEmpty() && searchQuery.isBlank() && !isLoading) {
            Text(
                text = "Počnite upisivati za pretraživanje korisnika",
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users, key = { it.id }) { user ->
                // ensure the follow button is not shown for the current user's own card
                val showFollowBtn = user.id != currentLoggedInUserId
                UserCard(
                    user = user,
                    onClick = onUserClick,
                    showFollowButton = showFollowBtn,
                    isFollowing = isFollowingMap[user.id] ?: false,
                    onToggleFollow = { userIdToToggle, _ ->
                        searchUserViewModel.toggleFollow(userIdToToggle)
                    },
                    isTogglingFollow = isTogglingFollowMap[user.id] ?: false
                )
            }
        }
    }
}
