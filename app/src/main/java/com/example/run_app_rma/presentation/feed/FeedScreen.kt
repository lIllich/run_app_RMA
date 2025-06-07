package com.example.run_app_rma.presentation.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.run_app_rma.presentation.common.RunPostCard
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale


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
    val isRefreshing by feedViewModel.isRefreshing.collectAsState()
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
                        if (isInitialLoading && !isRefreshing) {    // show initial loading only if not refreshing
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
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { feedViewModel.loadFeedPosts() },
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

                if (allPosts.isEmpty() && !isInitialLoading && !isRefreshing) {
                    Text("Nema objava za prikaz.")
                } else if (allPosts.isEmpty() && isInitialLoading && !isRefreshing) {
                    // should be covered by the CircularProgressIndicator in TopAppBar or by the isInitialLoading check
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
