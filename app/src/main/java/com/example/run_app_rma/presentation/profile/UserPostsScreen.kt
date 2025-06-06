package com.example.run_app_rma.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.run_app_rma.data.firestore.model.RunPost
import com.example.run_app_rma.presentation.common.RunPostCard
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import com.google.accompanist.swiperefresh.SwipeRefresh // Import SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState // Import rememberSwipeRefreshState


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
    val isRefreshing by userPostsViewModel.isRefreshing.collectAsState() // Observe refreshing state
    val errorMessage by userPostsViewModel.errorMessage.collectAsState()
    val postAuthor by userPostsViewModel.postAuthor.collectAsState(initial = null)

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val decimalFormat = DecimalFormat("#.##")

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    DisposableEffect(lifecycleOwner, userPostsViewModel) { // Add userPostsViewModel to key
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d("UserPostsScreen", "Lifecycle Event: ON_RESUME detected. Refreshing data.")
                userPostsViewModel.viewedUserId?.let { userId ->
                    userPostsViewModel.fetchAllUserDataForScreen() // Call the combined fetch
                } ?: run {
                    Log.w("UserPostsScreen", "viewedUserId is null on ON_RESUME. Cannot refresh posts.")
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // Initial data load when the ViewModel is first provided to the screen
        userPostsViewModel.viewedUserId?.let {
            Log.d("UserPostsScreen", "Initial fetch in DisposableEffect for user ID: $it")
            userPostsViewModel.fetchAllUserDataForScreen()
        }


        onDispose {
            Log.d("UserPostsScreen", "DisposableEffect disposing. Removing lifecycle observer.")
            lifecycleOwner.lifecycle.removeObserver(observer)
            userPostsViewModel.clearMessages() // Clear messages when screen is disposed
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = postAuthor?.displayName?.let { "$it objave" } ?: "Objave")
                        if (isLoading && !isRefreshing) { // Show initial loading only if not refreshing
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
        // Wrap the content with SwipeRefresh
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { userPostsViewModel.fetchAllUserDataForScreen() }, // Trigger refresh
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
                    Button(onClick = { userPostsViewModel.fetchAllUserDataForScreen() }) { // Retry button
                        Text("Pokušaj ponovo")
                    }
                }

                if (userPosts.isEmpty() && !isLoading && !isRefreshing) { // Only show "No posts" if not loading/refreshing
                    Text("Nema objava za prikaz.", modifier = Modifier.padding(16.dp))
                } else if (userPosts.isEmpty() && (isLoading || isRefreshing)) {
                    // This state should ideally be covered by the CircularProgressIndicator in TopAppBar
                    // or by the isInitialLoading check. Keeping it explicit for safety.
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
