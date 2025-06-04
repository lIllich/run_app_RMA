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
    )
) {
    val searchQuery by followViewModel.searchQuery.collectAsState()
    val users = followViewModel.users
    val isLoading by followViewModel.isLoading
    val errorMessage by followViewModel.errorMessage
    val isFollowingMap = followViewModel.isFollowingMap

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
                UserCard(
                    user = user,
                    isFollowing = isFollowingMap[user.id] ?: false,
                    onToggleFollow = { followViewModel.toggleFollow(user.id) }
                )
            }
        }
    }
}

@Composable
fun UserCard(
    user: User,
    isFollowing: Boolean,
    onToggleFollow: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (user.profileImageUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(user.profileImageUrl),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default Profile Picture",
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = { onToggleFollow(user.id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    contentColor = if (isFollowing) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = if (isFollowing) "Pratim" else "Zaprati")
            }
        }
    }
}