package com.example.run_app_rma.presentation.runpost

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextAlign
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
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunPostScreen(
    modifier: Modifier = Modifier,
    runPostViewModel: RunPostViewModel = viewModel(factory = RunPostViewModel.Factory),
    onBack: () -> Unit,
    onUserClick: (String) -> Unit, // To navigate to post creator's profile
    onViewLikedUsers: (String, String) -> Unit, // To navigate to UserListScreen for liked users
    onViewComments: (String) -> Unit // To be implemented later for comments
) {
    val runPost by runPostViewModel.runPost.collectAsState()
    val postUser by runPostViewModel.postUser.collectAsState() // This is the user who created the post
    val isLoading by runPostViewModel.isLoading.collectAsState()
    val errorMessage by runPostViewModel.errorMessage.collectAsState()
    val userLikedPostIds by runPostViewModel.userLikedPostIds.collectAsState()
    val likedUsers by runPostViewModel.likedUsers.collectAsState() // Observe liked users

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val decimalFormat = DecimalFormat("#.##")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Objava Trčanja") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Natrag")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                Text("Učitavanje objave...")
            } else if (errorMessage != null) {
                Text("Greška: $errorMessage", color = MaterialTheme.colorScheme.error)
                Button(onClick = { runPost?.id?.let { runPostViewModel.fetchRunPostAndRelatedData(it) } }) {
                    Text("Pokušaj ponovo")
                }
            } else if (runPost != null) {
                val post = runPost!!

                // Capture postUser into a local immutable variable for consistent null safety
                val currentUser = postUser

                // User Header (Profile Picture, Display Name)
                // This section directly displays the user info and is clickable to navigate to their profile
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { currentUser?.id?.let { onUserClick(it) } } // Use currentUser here
                ) {
                    Image(
                        painter = if (currentUser?.profileImageUrl != null && currentUser.profileImageUrl.isNotEmpty()) {
                            rememberAsyncImagePainter(currentUser.profileImageUrl)
                        } else {
                            painterResource(R.drawable.ic_profile_placeholder)
                        },
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentUser?.displayName ?: "Nepoznat korisnik", // Use currentUser here
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

                Spacer(modifier = Modifier.height(16.dp))

                // Run Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = "Distance")
                        val distanceText = if (post.distance < 1000) {
                            "${decimalFormat.format(post.distance)} m"
                        } else {
                            "${decimalFormat.format(post.distance / 1000)} km"
                        }
                        Text(distanceText, style = MaterialTheme.typography.bodyLarge)
                        Text("Udaljenost", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Speed, contentDescription = "Pace")
                        Text("${decimalFormat.format(post.avgPace)} min/km", style = MaterialTheme.typography.bodyLarge)
                        Text("Tempo", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AccessTime, contentDescription = "Duration")
                        val durationMillis = post.endTime - post.startTime
                        val durationText = if (durationMillis < TimeUnit.HOURS.toMillis(1)) {
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
                            val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) - TimeUnit.MINUTES.toSeconds(minutes)
                            if (minutes == 0L) {
                                "${seconds} s"
                            } else {
                                "${minutes} min i ${seconds} s"
                            }
                        } else {
                            val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) - TimeUnit.HOURS.toMinutes(hours)
                            String.format("%02d:%02d", hours, minutes)
                        }
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text("Trajanje", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Caption
                if (post.caption.isNotEmpty()) {
                    Text(
                        text = post.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Like and Comment Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like Button
                    val isLiked = userLikedPostIds.contains(post.id)
                    Button(
                        onClick = { runPostViewModel.toggleLike(post.id, isLiked) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onPrimary, // Adjust tint for button content
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${post.likesCount}", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Button to view liked users
                    Button(
                        onClick = { onViewLikedUsers(post.id, "liked_users") }, // Pass "liked_users" as listType
                        enabled = post.likesCount > 0, // Only enabled if there are likes
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sviđa se (${likedUsers.size})") // Display count of liked users
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Comment Button
                    Button(
                        onClick = { onViewComments(post.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Comment,
                                contentDescription = "Comment",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${post.commentsCount}", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            } else {
                Text("Objava nije pronađena.")
            }
        }
    }
}
