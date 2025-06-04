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
    val newPosts = feedViewModel.newPosts
    val olderPosts = feedViewModel.olderPosts
    val isLoading by feedViewModel.isLoading.collectAsState()
    val errorMessage by feedViewModel.errorMessage.collectAsState()
    val userProfiles by feedViewModel.userProfiles // Directly observe the State<Map>

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val decimalFormat = DecimalFormat("#.##")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Feed",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

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

        // CORRECTED: Use .size == 0 for isEmpty() check
        if (newPosts.size == 0 && olderPosts.size == 0 && !isLoading) {
            Text("Nema objava za prikaz.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // CORRECTED: Use .isNotEmpty() for Collection
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
                        FeedPostCard(
                            post = post,
                            user = userProfiles[post.userId], // Pass the user profile
                            dateFormat = dateFormat,
                            decimalFormat = decimalFormat,
                            onLikeClick = { postId, isLiked -> feedViewModel.toggleLike(postId, isLiked) }
                            // Add onCommentClick if you implement comments later
                        )
                    }
                }

                // CORRECTED: Use .isNotEmpty() for Collection
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
                        FeedPostCard(
                            post = post,
                            user = userProfiles[post.userId],
                            dateFormat = dateFormat,
                            decimalFormat = decimalFormat,
                            onLikeClick = { postId, isLiked -> feedViewModel.toggleLike(postId, isLiked) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeedPostCard(
    post: RunPost,
    user: User?, // User profile for the post creator
    dateFormat: SimpleDateFormat,
    decimalFormat: DecimalFormat,
    onLikeClick: (String, Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // User Header (Profile Picture, Display Name)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = if (user?.profileImageUrl != null && user.profileImageUrl.isNotEmpty()) {
                        rememberAsyncImagePainter(user.profileImageUrl)
                    } else {
                        painterResource(R.drawable.ic_profile_placeholder) // Default placeholder
                    },
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = user?.displayName ?: "Nepoznat korisnik",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = post.timestamp?.let { dateFormat.format(it) } ?: "N/A",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Run Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsRun, contentDescription = "Distance") // DirectionsRun is now imported
                    Text("${decimalFormat.format(post.distance / 1000)} km", style = MaterialTheme.typography.bodyLarge)
                    Text("Udaljenost", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Pace")
                    Text("${decimalFormat.format(post.avgPace)} min/km", style = MaterialTheme.typography.bodyLarge)
                    Text("Tempo", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Favorite, contentDescription = "Duration") // Placeholder icon
                    val durationMillis = post.endTime - post.startTime
                    val minutes = (durationMillis / (1000 * 60)) % 60
                    val hours = (durationMillis / (1000 * 60 * 60))
                    Text(
                        text = String.format("%02d:%02d", hours, minutes),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text("Trajanje", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Caption
            if (post.caption.isNotEmpty()) {
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Like and Comment Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLikeClick(post.id, post.likesCount > 0) } // Simplified isLiked check for now
                ) {
                    val isLiked = post.likesCount > 0 // Placeholder, will need actual check
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${post.likesCount}", style = MaterialTheme.typography.bodyMedium)
                }

                // Comment Button (Placeholder)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { /* TODO: Implement comment functionality */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn, // Placeholder icon for comments
                        contentDescription = "Comment",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${post.commentsCount}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
