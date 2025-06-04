package com.example.run_app_rma.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.run_app_rma.presentation.common.UserCard // Import UserCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    modifier: Modifier = Modifier,
    userListViewModel: UserListViewModel = viewModel(factory = UserListViewModel.Factory),
    onBack: () -> Unit,
    onUserClick: (String) -> Unit // Lambda to navigate to another user's profile
) {
    val users by userListViewModel.users.collectAsState()
    val isLoading by userListViewModel.isLoading.collectAsState()
    val errorMessage by userListViewModel.errorMessage.collectAsState()
    val isFollowingMap = userListViewModel.isFollowingMap // Observe the map
    val isTogglingFollowMap = userListViewModel.isTogglingFollowMap // Observe the map

    // Access savedStateHandle directly from the ViewModel instance
    val listType = userListViewModel.savedStateHandle.get<String>("listType") ?: "Korisnici"
    val userId = userListViewModel.savedStateHandle.get<String>("userId")

    val title = when (listType) {
        "following" -> "Pratim"
        "followers" -> "Pratitelji"
        else -> "Korisnici"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }

            if (users.isEmpty() && !isLoading) {
                Text("Nema korisnika za prikaz.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(users, key = { it.id }) { user ->
                        // Determine whether to show the follow button based on listType
                        // Don't show follow button for the current user's own profile
                        val showFollowButtonInCard = listType != "following" && user.id != userListViewModel.firebaseAuth.currentUser?.uid
                        val isFollowing = isFollowingMap[user.id] ?: false
                        val isTogglingFollow = isTogglingFollowMap[user.id] ?: false

                        UserCard(
                            user = user,
                            onClick = onUserClick,
                            showFollowButton = showFollowButtonInCard,
                            isFollowing = isFollowing,
                            onToggleFollow = { userIdToToggle, _ -> // _ because we don't need isCurrentlyFollowing here, ViewModel handles it
                                userListViewModel.toggleFollow(userIdToToggle)
                            },
                            isTogglingFollow = isTogglingFollow
                        )
                    }
                }
            }
        }
    }
}
