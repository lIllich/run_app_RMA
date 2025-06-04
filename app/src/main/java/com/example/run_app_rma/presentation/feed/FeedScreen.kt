package com.example.run_app_rma.presentation.feed

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.DirectionsRun // Import DirectionsRun icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.run_app_rma.R // Assuming you have a default profile placeholder
import com.example.run_app_rma.data.firestore.model.RunPost
import com.example.run_app_rma.data.firestore.model.User
import com.example.run_app_rma.presentation.common.RunPostCard // Import RunPostCard
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    feedViewModel: FeedViewModel = viewModel(), // ViewModel will be provided by MainScreenWithTabs
    onUserClick: (String) -> Unit // New parameter: Lambda to navigate to another user's profile
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
                            onUserClick = onUserClick // Pass the onUserClick lambda
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
                            onUserClick = onUserClick // Pass the onUserClick lambda
                        )
                    }
                }
            }
        }
    }
}
