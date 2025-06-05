package com.example.run_app_rma.presentation.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.run_app_rma.presentation.common.RunPostCard // Import the new reusable card
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    feedViewModel: FeedViewModel = viewModel(), // ViewModel will be provided by MainScreenWithTabs
    onUserClick: (String) -> Unit, // New parameter: Lambda to navigate to another user's profile
    onPostClick: (String) -> Unit // New parameter: Lambda to navigate to a specific post
) {
    val newPosts = feedViewModel.newPosts
    val olderPosts = feedViewModel.olderPosts
    val isLoading by feedViewModel.isLoading.collectAsState()
    val errorMessage by feedViewModel.errorMessage.collectAsState()
    val userProfiles by feedViewModel.userProfiles // Directly observe the State<Map>
    val userLikedPostIds by feedViewModel.userLikedPostIds.collectAsState() // Observe liked post IDs

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val decimalFormat = DecimalFormat("#.##")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
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

        if (newPosts.isEmpty() && olderPosts.isEmpty() && !isLoading) {
            Text("Nema objava za prikaz.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (newPosts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Nove objave",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(newPosts, key = { it.id }) { post ->
                        RunPostCard( // Changed from FeedPostCard to RunPostCard
                            post = post,
                            user = userProfiles[post.userId],
                            dateFormat = dateFormat,
                            decimalFormat = decimalFormat,
                            onLikeClick = { postId, isLiked -> feedViewModel.toggleLike(postId, isLiked) },
                            isLiked = userLikedPostIds.contains(post.id), // Pass actual liked status
                            onUserClick = onUserClick, // Pass the onUserClick lambda
                            onPostClick = onPostClick // Pass the onPostClick lambda
                        )
                    }
                }

                if (olderPosts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Starije objave",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(olderPosts, key = { it.id }) { post ->
                        RunPostCard( // Changed from FeedPostCard to RunPostCard
                            post = post,
                            user = userProfiles[post.userId],
                            dateFormat = dateFormat,
                            decimalFormat = decimalFormat,
                            onLikeClick = { postId, isLiked -> feedViewModel.toggleLike(postId, isLiked) },
                            isLiked = userLikedPostIds.contains(post.id), // Pass actual liked status
                            onUserClick = onUserClick, // Pass the onUserClick lambda
                            onPostClick = onPostClick // Pass the onPostClick lambda
                        )
                    }
                }
            }
        }
    }
}
