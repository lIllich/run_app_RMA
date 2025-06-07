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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun RunPostCard(
    post: RunPost,
    user: User?,                    // user that created the post
    dateFormat: SimpleDateFormat,
    decimalFormat: DecimalFormat,
    onLikeClick: (String, Boolean) -> Unit,
    isLiked: Boolean,               // if current user has liked this post
    onUserClick: (String) -> Unit,  // callback for user profile click
    onPostClick: (String) -> Unit   // callback for post click
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPostClick(post.id) } // make the whole card clickable for post details
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // user header (Profile Picture, Display Name, Timestamp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { user?.id?.let { onUserClick(it) } }    // make user header clickable
            ) {
                Image(
                    painter = if (user?.profileImageUrl != null && user.profileImageUrl.isNotEmpty()) {
                        rememberAsyncImagePainter(user.profileImageUrl)
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
                    text = user?.displayName ?: "Nepoznat korisnik",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))  // pushes timestamp to the end
                post.timestamp?.let {
                    Text(
                        text = dateFormat.format(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // run details
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
                            "$seconds s"
                        } else {
                            "$minutes min i $seconds s"
                        }
                    } else {
                        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) - TimeUnit.HOURS.toMinutes(hours)
                        String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
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

            // like and comment icons (switched order, comment is not clickable)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // comment section (first, not clickable)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Comment,
                        contentDescription = "Comment",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${post.commentsCount}", style = MaterialTheme.typography.bodyMedium)
                }

                // like button (second, clickable)
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
            }
        }
    }
}
