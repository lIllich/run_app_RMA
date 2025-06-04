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
import androidx.compose.material.icons.filled.DirectionsRun
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
import com.example.run_app_rma.R
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
    feedViewModel: FeedViewModel = viewModel() // ViewModel will be provided by MainScreenWithTabs
) {
    // CORRECTED: Directly access mutableStateListOf properties. No .collectAsState() needed here.
    // These are already observable by Compose.
    val newPosts = feedViewModel.newPosts
    val olderPosts = feedViewModel.olderPosts
    val isLoading by feedViewModel.isLoading.collectAsState()
    val errorMessage by feedViewModel.errorMessage.collectAsState()
    val userProfiles by feedViewModel.userProfiles // userProfiles is a State<Map>, so direct access to its value
    val userLikedPostIds by feedViewModel.userLikedPostIds.collectAsState() // userLikedPostIds is a StateFlow, so collectAsState is correct

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
                color = MaterialTheme.colorScheme.error, // Correct usage
                modifier = Modifier.padding(8.dp)
            )
        }

        // Check if both lists are empty
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
                            style = MaterialTheme.typography.titleLarge, // Correct usage
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(newPosts, key = { it.id }) { post -> // Explicitly define type for 'post'
                        RunPostCard(
                            post = post,
                            user = userProfiles[post.userId], // Access value of State<Map>
                            dateFormat = dateFormat,
                            decimalFormat = decimalFormat,
                            onLikeClick = { postId, isLiked -> feedViewModel.toggleLike(postId, isLiked) },
                            isLiked = userLikedPostIds.contains(post.id)
                        )
                    }
                }

                if (olderPosts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Starije objave",
                            style = MaterialTheme.typography.titleLarge, // Correct usage
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(olderPosts, key = { it.id }) { post -> // Explicitly define type for 'post'
                        RunPostCard(
                            post = post,
                            user = userProfiles[post.userId], // Access value of State<Map>
                            dateFormat = dateFormat,
                            decimalFormat = decimalFormat,
                            onLikeClick = { postId, isLiked -> feedViewModel.toggleLike(postId, isLiked) },
                            isLiked = userLikedPostIds.contains(post.id)
                        )
                    }
                }
            }
        }
    }
}
