package com.example.run_app_rma.presentation.runpost

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.run_app_rma.data.firestore.model.User
import com.example.run_app_rma.presentation.common.UserCard
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RunPostScreen(
    modifier: Modifier = Modifier,
    runPostViewModel: RunPostViewModel = viewModel(factory = RunPostViewModel.Factory),
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onViewLikedUsers: (String, String) -> Unit,
    onViewComments: (String) -> Unit,
    onPostDeleted: () -> Unit,
    onViewMap: (String) -> Unit,
    onViewElevationGraph: (String) -> Unit
) {
    val runPost by runPostViewModel.runPost.collectAsState()
    val postUser by runPostViewModel.postUser.collectAsState()
    val isInitialLoading: Boolean by runPostViewModel.isInitialLoading.collectAsState()
    val isRefreshing by runPostViewModel.isRefreshing.collectAsState()
    val isLoadingAction: Boolean by runPostViewModel.isLoadingAction.collectAsState()
    val errorMessage by runPostViewModel.errorMessage.collectAsState()
    val userLikedPostIds by runPostViewModel.userLikedPostIds.collectAsState()
    val likedUsers by runPostViewModel.likedUsers.collectAsState()
    val comments by runPostViewModel.comments.collectAsState()
    val commentUsers by runPostViewModel.commentUsers.collectAsState()
    val commentInput by runPostViewModel.commentInput.collectAsState()
    val currentUserId by runPostViewModel.currentUserId.collectAsState()

    var showPostMenu by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val decimalFormat = DecimalFormat("#.##")

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()

    val swipeRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { runPost?.id?.let { runPostViewModel.fetchRunPostAndRelatedData(it) } }
    )

    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Natrag")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Objava Trčanja", style = MaterialTheme.typography.headlineSmall)
                if (isInitialLoading && !isRefreshing) {
                    Spacer(modifier = Modifier.width(12.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                if (runPost != null && currentUserId == runPost?.userId) {
                    IconButton(onClick = { showPostMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opcije objave")
                    }
                    DropdownMenu(
                        expanded = showPostMenu,
                        onDismissRequest = { showPostMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Obriši objavu") },
                            onClick = {
                                showPostMenu = false
                                runPostViewModel.deletePost(
                                    postId = runPost!!.id,
                                    onSuccessAction = { onPostDeleted() }
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = "Obriši objavu")
                            }
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(swipeRefreshState)
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                if (errorMessage != null) {
                    Text("Greška: $errorMessage", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { runPost?.id?.let { runPostViewModel.fetchRunPostAndRelatedData(it) } }) {
                        Text("Pokušaj ponovo")
                    }
                } else if (runPost != null) {
                    val post = runPost!!
                    val currentPostUser = postUser

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
                        Spacer(modifier = Modifier.width(12.dp))
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

                    Spacer(modifier = Modifier.height(8.dp))

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

                    Spacer(modifier = Modifier.height(8.dp))

                    // elevation graph and map buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,        // distribute buttons evenly
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { runPost?.id?.let { onViewMap(it) } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Karta")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { runPost?.id?.let { onViewElevationGraph(it) } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Graf elevacije")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (post.caption.isNotEmpty()) {
                        Text(
                            text = post.caption,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    HorizontalDivider(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sviđa li vam se objava?",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )

                        val isLiked = userLikedPostIds.contains(post.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(enabled = !isLoadingAction) { runPostViewModel.toggleLike(post.id, isLiked) }
                                .weight(1f)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isLoadingAction) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${post.likesCount}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = pagerState.currentPage == 0,
                            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                            text = { Text("Komentari (${comments.size})") }
                        )
                        Tab(
                            selected = pagerState.currentPage == 1,
                            onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                            text = { Text("Sviđa se (${likedUsers.size})") }
                        )
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .weight(1f)
                    ) { page ->
                        when (page) {
                            0 -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                ) {
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
                                        Spacer(modifier = Modifier.width(12.dp))
                                        IconButton(
                                            onClick = { runPostViewModel.addComment() },
                                            enabled = commentInput.isNotBlank() && !isLoadingAction
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Pošalji komentar")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

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
                                                    currentUserId = currentUserId,
                                                    postOwnerId = post.userId,
                                                    onUserClick = onUserClick,
                                                    onDeleteComment = { commentId -> runPostViewModel.deleteComment(commentId) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> {
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
                                                onClick = { userId -> onUserClick(userId) },
                                                showFollowButton = false
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

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = swipeRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    commenter: User?,
    dateFormat: SimpleDateFormat,
    currentUserId: String?,
    postOwnerId: String?,
    onUserClick: (String) -> Unit,
    onDeleteComment: (String) -> Unit
) {
    // state to control dropdown menu visibility
    var showMenu by remember { mutableStateOf(false) }

    val canDeleteComment = currentUserId != null &&
            (currentUserId == comment.userId || currentUserId == postOwnerId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable( // combinedClickable used for long press
                onClick = { /* no op */ },
                onLongClick = {
                    if (canDeleteComment) {
                        showMenu = true
                    }
                }
            ),
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
                Spacer(modifier = Modifier.width(12.dp))
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

            // dropdown menu for delete option
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (canDeleteComment) {
                    DropdownMenuItem(
                        text = { Text("Obriši komentar") },
                        onClick = {
                            onDeleteComment(comment.id)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = "Obriši")
                        }
                    )
                }
            }
        }
    }
}
