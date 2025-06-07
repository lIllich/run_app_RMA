package com.example.run_app_rma.presentation.profile

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.run_app_rma.presentation.common.RunPostCard
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPostsScreen(
    modifier: Modifier = Modifier,
    userPostsViewModel: UserPostsViewModel = viewModel(factory = UserPostsViewModel.Factory),
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onPostClick: (String) -> Unit
) {
    val userPosts by userPostsViewModel.userPosts.collectAsState(initial = emptyList())
    val userLikedPostIds by userPostsViewModel.userLikedPostIds.collectAsState(initial = emptySet())
    val isLoading by userPostsViewModel.isLoading.collectAsState()
    val isRefreshing by userPostsViewModel.isRefreshing.collectAsState()
    val errorMessage by userPostsViewModel.errorMessage.collectAsState()
    val postAuthor by userPostsViewModel.postAuthor.collectAsState(initial = null)

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val decimalFormat = DecimalFormat("#.##")

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    DisposableEffect(lifecycleOwner, userPostsViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d("UserPostsScreen", "Lifecycle Event: ON_RESUME detected. Refreshing data.")
                userPostsViewModel.viewedUserId?.let { _ ->
                    userPostsViewModel.fetchAllUserDataForScreen()
                } ?: run {
                    Log.w("UserPostsScreen", "viewedUserId is null on ON_RESUME. Cannot refresh posts.")
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // initial data load when the ViewModel is first provided to the screen
        userPostsViewModel.viewedUserId?.let {
            Log.d("UserPostsScreen", "Initial fetch in DisposableEffect for user ID: $it")
            userPostsViewModel.fetchAllUserDataForScreen()
        }


        onDispose {
            Log.d("UserPostsScreen", "DisposableEffect disposing. Removing lifecycle observer.")
            lifecycleOwner.lifecycle.removeObserver(observer)
            userPostsViewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = postAuthor?.displayName?.let { "$it objave" } ?: "Objave")
                        if (isLoading && !isRefreshing) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { userPostsViewModel.fetchAllUserDataForScreen() },
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                errorMessage?.let { message ->
                    Text("Greška: $message", color = MaterialTheme.colorScheme.error)
                    // retry button
                    Button(onClick = { userPostsViewModel.fetchAllUserDataForScreen() }) {
                        Text("Pokušaj ponovo")
                    }
                }

                if (userPosts.isEmpty() && !isLoading && !isRefreshing) {
                    Text("Nema objava za prikaz.", modifier = Modifier.padding(16.dp))
                } else if (userPosts.isEmpty() && (isLoading || isRefreshing)) {
                    // covered by the CircularProgressIndicator in TopAppBar or by the isInitialLoading check
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(userPosts, key = { it.id }) { post ->
                            RunPostCard(
                                post = post,
                                user = postAuthor,
                                dateFormat = dateFormat,
                                decimalFormat = decimalFormat,
                                onLikeClick = { postId, isLiked -> userPostsViewModel.toggleLike(postId, isLiked) },
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
