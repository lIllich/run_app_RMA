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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.run_app_rma.data.firestore.model.RunPost
import com.example.run_app_rma.presentation.common.RunPostCard
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPostsScreen(
    modifier: Modifier = Modifier,
    userPostsViewModel: UserPostsViewModel = viewModel(factory = UserPostsViewModel.Factory),
    onBack: () -> Unit,
    onUserClick: (String) -> Unit // New parameter: Lambda to navigate to another user's profile
) {
    val userPosts by userPostsViewModel.userPosts.collectAsState(initial = emptyList())
    val userLikedPostIds by userPostsViewModel.userLikedPostIds.collectAsState(initial = emptySet())
    val isLoading by userPostsViewModel.isLoading.collectAsState()
    val errorMessage by userPostsViewModel.errorMessage.collectAsState()
    val postAuthor by userPostsViewModel.postAuthor.collectAsState(initial = null) // Collect the author of the posts

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val decimalFormat = DecimalFormat("#.##")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = postAuthor?.displayName?.let { "$it objave" } ?: "Objave") },
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
                Text("Učitavanje objava...")
            } else if (errorMessage != null) {
                Text("Greška: $errorMessage", color = MaterialTheme.colorScheme.error)
                Button(onClick = { userPostsViewModel.fetchUserPosts() }) {
                    Text("Pokušaj ponovo")
                }
            } else if (userPosts.isEmpty()) {
                Text("Nema objava za prikaz.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(userPosts, key = { it.id }) { post ->
                        RunPostCard(
                            post = post,
                            user = postAuthor, // Pass the author of the posts
                            dateFormat = dateFormat,
                            decimalFormat = decimalFormat,
                            onLikeClick = { postId, isLiked -> userPostsViewModel.toggleLike(postId, isLiked) },
                            isLiked = userLikedPostIds.contains(post.id),
                            onUserClick = onUserClick // Pass the onUserClick lambda
                        )
                    }
                }
            }
        }
    }
}
