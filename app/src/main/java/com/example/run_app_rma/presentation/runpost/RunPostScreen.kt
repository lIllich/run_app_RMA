package com.example.run_app_rma.presentation.runpost

import androidx.compose.foundation.ExperimentalFoundationApi // New import for HorizontalPager
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider // Added import for Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.run_app_rma.data.firestore.model.Comment
import com.example.run_app_rma.data.firestore.model.RunPost
import com.example.run_app_rma.data.firestore.model.User
import com.example.run_app_rma.presentation.common.UserCard // Import UserCard
import androidx.compose.foundation.pager.HorizontalPager // Changed import
import androidx.compose.foundation.pager.rememberPagerState // Changed import
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class) // Changed OptIn
@Composable
fun RunPostScreen(
    modifier: Modifier = Modifier,
    runPostViewModel: RunPostViewModel = viewModel(factory = RunPostViewModel.Factory),
    onBack: () -> Unit,
    onUserClick: (String) -> Unit, // To navigate to post creator's profile
    onViewLikedUsers: (String, String) -> Unit, // To navigate to UserListScreen for liked users
    onViewComments: (String) -> Unit // To be implemented later for comments, currently for toast
) {
    val runPost by runPostViewModel.runPost.collectAsState()
    val postUser by runPostViewModel.postUser.collectAsState()
    val isLoading by runPostViewModel.isLoading.collectAsState()
    val errorMessage by runPostViewModel.errorMessage.collectAsState()
    val userLikedPostIds by runPostViewModel.userLikedPostIds.collectAsState()
    val likedUsers by runPostViewModel.likedUsers.collectAsState()
    val comments by runPostViewModel.comments.collectAsState()
    val commentUsers by runPostViewModel.commentUsers.collectAsState()
    val commentInput by runPostViewModel.commentInput.collectAsState()

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val decimalFormat = DecimalFormat("#.##")

    // The pagerState will be used for tabs
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()

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
                .padding(horizontal = 16.dp),
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
                val currentPostUser = postUser

                // User Header (Profile Picture, Display Name)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { currentPostUser?.id?.let { onUserClick(it) } }
                        .padding(vertical = 8.dp)
                ) {
                    Image(
                        painter = if (currentPostUser?.profileImageUrl != null && currentPostUser.profileImageUrl.isNotEmpty()) {
                            rememberAsyncImagePainter(currentPostUser.profileImageUrl)
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
                        text = currentPostUser?.displayName ?: "Nepoznat korisnik",
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

                // Divider after caption
                Divider(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp)) // Small gap after the divider

                // "Sviđa li vam se objava?" text and Like Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sviđa li vam se objava?",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center // Center the text
                    )

                    val isLiked = userLikedPostIds.contains(post.id)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { runPostViewModel.toggleLike(post.id, isLiked) }
                            .weight(1f)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.Center
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

                Spacer(modifier = Modifier.height(8.dp)) // Reduced gap

                // Tabs for Comments and Likes (swapped order)
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        text = { Text("Komentari (${comments.size})") } // Comments tab first
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text("Sviđa se (${likedUsers.size})") } // Likes tab second
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Take remaining height
                ) { page ->
                    when (page) {
                        0 -> {
                            // Komentari Tab Content (now at page 0)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                // Comment Input Field
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextField(
                                        value = commentInput,
                                        onValueChange = { runPostViewModel.onCommentInputChanged(it) },
                                        placeholder = { Text("Dodaj komentar...") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { runPostViewModel.addComment() },
                                        enabled = commentInput.isNotBlank()
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "Pošalji komentar")
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // List of Comments
                                if (comments.isEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text("Nema komentara za prikaz.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(comments, key = { it.id }) { comment ->
                                            val commenter = commentUsers[comment.userId]
                                            CommentItem(
                                                comment = comment,
                                                commenter = commenter,
                                                dateFormat = dateFormat,
                                                onUserClick = onUserClick
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Lajkovi Tab Content (now at page 1)
                            if (likedUsers.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("Nema lajkova za prikaz.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(likedUsers, key = { it.id }) { user ->
                                        UserCard(
                                            user = user,
                                            onClick = { userId -> onUserClick(userId) }, // Navigate to user profile
                                            showFollowButton = false // No follow button in this context
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Text("Objava nije pronađena.")
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    commenter: User?,
    dateFormat: SimpleDateFormat,
    onUserClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { commenter?.id?.let { onUserClick(it) } }
            ) {
                Image(
                    painter = if (commenter?.profileImageUrl != null && commenter.profileImageUrl.isNotEmpty()) {
                        rememberAsyncImagePainter(commenter.profileImageUrl)
                    } else {
                        painterResource(R.drawable.ic_profile_placeholder)
                    },
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = commenter?.displayName ?: "Nepoznat korisnik",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    comment.timestamp?.let {
                        Text(
                            text = dateFormat.format(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
