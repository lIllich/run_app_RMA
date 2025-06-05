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

@OptIn(ExperimentalMaterial3Api::class) // Opt-in for ExperimentalMaterial3Api for TopAppBar
@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    feedViewModel: FeedViewModel = viewModel(), // ViewModel will be provided by MainScreenWithTabs
    onUserClick: (String) -> Unit, // New parameter: Lambda to navigate to another user's profile
    onPostClick: (String) -> Unit // New parameter: Lambda to navigate to a specific post
) {
    // Observe the single list of all posts
    val allPosts = feedViewModel.allPosts
    // Corrected: Observe isInitialLoading from FeedViewModel
    val isInitialLoading: Boolean by feedViewModel.isInitialLoading.collectAsState()
    val errorMessage by feedViewModel.errorMessage.collectAsState()
    val userProfiles by feedViewModel.userProfiles // Directly observe the State<Map>
    val userLikedPostIds by feedViewModel.userLikedPostIds.collectAsState() // Observe liked post IDs

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val decimalFormat = DecimalFormat("#.##")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Feed")
                        if (isInitialLoading) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp), // Smaller size for TopAppBar
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding) // Apply padding from Scaffold
                .padding(horizontal = 16.dp), // Add horizontal padding for content
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Removed the old CircularProgressIndicator from here as it's now in the TopAppBar

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Check if allPosts is empty and not initially loading
            if (allPosts.isEmpty() && !isInitialLoading) { // Corrected: Use isInitialLoading
                Text("Nema objava za prikaz.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Display all posts from the 'allPosts' list
                    items(allPosts, key = { it.id }) { post ->
                        RunPostCard(
                            post = post,
                            user = userProfiles[post.userId], // Access user profile by post.userId
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
