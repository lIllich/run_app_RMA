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
import com.example.run_app_rma.presentation.common.RunPostCard
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.accompanist.swiperefresh.SwipeRefresh // Import SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState // Import rememberSwipeRefreshState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    feedViewModel: FeedViewModel = viewModel(),
    onUserClick: (String) -> Unit,
    onPostClick: (String) -> Unit
) {
    val allPosts = feedViewModel.allPosts
    val isInitialLoading: Boolean by feedViewModel.isInitialLoading.collectAsState()
    val isRefreshing by feedViewModel.isRefreshing.collectAsState() // Observe refreshing state
    val errorMessage by feedViewModel.errorMessage.collectAsState()
    val userProfiles by feedViewModel.userProfiles
    val userLikedPostIds by feedViewModel.userLikedPostIds.collectAsState()

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val decimalFormat = DecimalFormat("#.##")

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Feed")
                        if (isInitialLoading && !isRefreshing) { // Show initial loading only if not refreshing
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        // Wrap the content with SwipeRefresh
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { feedViewModel.loadFeedPosts() }, // Trigger refresh
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                if (allPosts.isEmpty() && !isInitialLoading && !isRefreshing) { // Only show "No posts" if not loading/refreshing
                    Text("Nema objava za prikaz.")
                } else if (allPosts.isEmpty() && isInitialLoading && !isRefreshing) {
                    // This state should ideally be covered by the CircularProgressIndicator in TopAppBar
                    // or by the isInitialLoading check. Keeping it explicit for safety.
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(allPosts, key = { it.id }) { post ->
                            RunPostCard(
                                post = post,
                                user = userProfiles[post.userId],
                                dateFormat = dateFormat,
                                decimalFormat = decimalFormat,
                                onLikeClick = { postId, isLiked -> feedViewModel.toggleLike(postId, isLiked) },
                                isLiked = userLikedPostIds.contains(post.id),
                                onUserClick = onUserClick,
                                onPostClick = onPostClick
                            )
                        }
                    }
                }
            }
        }
    }
}
