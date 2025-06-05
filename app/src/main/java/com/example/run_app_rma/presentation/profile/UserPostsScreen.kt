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
import androidx.compose.runtime.LaunchedEffect // Keep for initial fetch if needed, but not for resume
import androidx.compose.runtime.DisposableEffect // New import for lifecycle observation
import androidx.compose.ui.platform.LocalLifecycleOwner // New import for lifecycle observation
import androidx.lifecycle.Lifecycle // New import for lifecycle observation
import androidx.lifecycle.LifecycleEventObserver // New import for lifecycle observation
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth
import android.util.Log // Import Log for debugging

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPostsScreen(
    modifier: Modifier = Modifier,
    userPostsViewModel: UserPostsViewModel = viewModel(factory = UserPostsViewModel.Factory),
    onBack: () -> Unit,
    onUserClick: (String) -> Unit, // New parameter: Lambda to navigate to another user's profile
    onPostClick: (String) -> Unit // New parameter: Lambda to navigate to a specific post
) {
    val userPosts by userPostsViewModel.userPosts.collectAsState(initial = emptyList())
    val userLikedPostIds by userPostsViewModel.userLikedPostIds.collectAsState(initial = emptySet())
    val isLoading by userPostsViewModel.isLoading.collectAsState()
    val errorMessage by userPostsViewModel.errorMessage.collectAsState()
    val postAuthor by userPostsViewModel.postAuthor.collectAsState(initial = null) // Collect the author of the posts

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val decimalFormat = DecimalFormat("#.##")

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d("UserPostsScreen", "Lifecycle Event: ON_RESUME detected. Refreshing data.")
                // When the screen comes to the foreground (resumes), refresh all data
                userPostsViewModel.viewedUserId?.let { userId ->
                    Log.d("UserPostsScreen", "Calling fetchUserPosts() for user ID: $userId")
                    userPostsViewModel.fetchUserPosts()
                    Log.d("UserPostsScreen", "Calling fetchUserLikedPosts() for current user ID: ${FirebaseAuth.getInstance().currentUser?.uid}")
                    userPostsViewModel.fetchUserLikedPosts(FirebaseAuth.getInstance().currentUser?.uid)
                    Log.d("UserPostsScreen", "Calling fetchPostAuthorProfile() for user ID: $userId")
                    userPostsViewModel.fetchPostAuthorProfile(userId)
                } ?: run {
                    Log.w("UserPostsScreen", "viewedUserId is null on ON_RESUME. Cannot refresh posts.")
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            Log.d("UserPostsScreen", "DisposableEffect disposing. Removing lifecycle observer.")
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // You can remove the LaunchedEffect here if you solely rely on DisposableEffect for refresh
    // However, keeping it might be useful for the very first composition when the ViewModel is created.
    // If you keep it, ensure it doesn't cause double fetches on initial load.
    // Given the ViewModel's init block already fetches, this LaunchedEffect might be redundant
    // for initial load if not for key changes. For clarity, I'm removing it here.
    /*
    LaunchedEffect(key1 = userPostsViewModel.viewedUserId, key2 = FirebaseAuth.getInstance().currentUser?.uid) {
        userPostsViewModel.fetchUserPosts()
        userPostsViewModel.fetchUserLikedPosts(FirebaseAuth.getInstance().currentUser?.uid)
        userPostsViewModel.viewedUserId?.let { userPostsViewModel.fetchPostAuthorProfile(it) }
    }
    */

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = postAuthor?.displayName?.let { "$it objave" } ?: "Objave") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                Text("Učitavanje objava...")
            } else if (errorMessage != null) {
                Text("Greška: $errorMessage", color = MaterialTheme.colorScheme.error)
                Button(onClick = { userPostsViewModel.fetchUserPosts() }) {
                    Text("Pokušaj ponovo")
                }
            } else if (userPosts.isEmpty()) {
                Text("Nema objava za prikaz.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(userPosts, key = { it.id }) { post ->
                        RunPostCard(
                            post = post,
                            user = postAuthor, // Pass the author of the posts
                            dateFormat = dateFormat,
                            decimalFormat = decimalFormat,
                            onLikeClick = { postId, isLiked -> userPostsViewModel.toggleLike(postId, isLiked) },
                            isLiked = userLikedPostIds.contains(post.id),
                            onUserClick = onUserClick, // Pass the onUserClick lambda
                            onPostClick = onPostClick // Pass the onPostClick lambda
                        )
                    }
                }
            }
        }
    }
}
