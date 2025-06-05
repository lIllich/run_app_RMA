package com.example.run_app_rma.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.run_app_rma.R
import com.example.run_app_rma.data.firestore.model.RunPost
import com.example.run_app_rma.data.firestore.model.User
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit // Import TimeUnit for duration calculations

/**
 * Reusable Composable for displaying a run post card.
 * Can be used in both FeedScreen and ProfileScreen.
 *
 * @param post The RunPost object to display.
 * @param user The User object of the post creator. Can be null if profile not found.
 * @param dateFormat SimpleDateFormat for formatting timestamps.
 * @param decimalFormat DecimalFormat for formatting numerical values like distance and pace.
 * @param onLikeClick Lambda to be invoked when the like button is clicked.
 * Receives the postId and a boolean indicating if it's currently liked.
 * @param isLiked Indicates if the current user has liked this post.
 * @param onUserClick Lambda to be invoked when the user header (profile picture/name) is clicked.
 * Receives the userId of the post creator.
 */
@Composable
fun RunPostCard(
    post: RunPost,
    user: User?,
    dateFormat: SimpleDateFormat,
    decimalFormat: DecimalFormat,
    onLikeClick: (String, Boolean) -> Unit,
    isLiked: Boolean,
    onUserClick: (String) -> Unit // New parameter for user header click
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { user?.id?.let { onUserClick(it) } } // Make the whole row clickable
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
                    Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = "Distance")
                    // Distance formatting logic
                    val distanceText = if (post.distance < 1000) { // If distance is less than 1km (1000 meters)
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
                    // Duration formatting logic
                    val durationMillis = post.endTime - post.startTime
                    val durationText = if (durationMillis < TimeUnit.HOURS.toMillis(1)) { // Less than 1 hour
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
                        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) - TimeUnit.MINUTES.toSeconds(minutes)
                        if (minutes == 0L) { // If duration is only in seconds
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
                    modifier = Modifier.clickable { onLikeClick(post.id, isLiked) }
                ) {
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
                        imageVector = Icons.AutoMirrored.Filled.Comment, // Placeholder icon for comments
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