package com.example.run_app_rma.presentation.profile

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.run_app_rma.data.firestore.model.RunPost
import com.example.run_app_rma.data.firestore.model.User
import com.example.run_app_rma.data.firestore.repository.RunPostRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserPostsViewModel(
    private val runPostRepository: RunPostRepository,
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _userPosts = MutableStateFlow<List<RunPost>>(emptyList())
    val userPosts: StateFlow<List<RunPost>> = _userPosts.asStateFlow()

    private val _userLikedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val userLikedPostIds: StateFlow<Set<String>> = _userLikedPostIds.asStateFlow()

    private val _isLoading = MutableStateFlow(false) // General screen loading
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingAction = MutableStateFlow(false) // Specific action loading (e.g., like/unlike)
    val isLoadingAction: StateFlow<Boolean> = _isLoadingAction.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _postAuthor = MutableStateFlow<User?>(null)
    val postAuthor: StateFlow<User?> = _postAuthor.asStateFlow()

    private val TAG = "UserPostsViewModel"

    // Changed from private var to var to allow access from Composable
    var viewedUserId: String? = null

    init {
        savedStateHandle.get<String>("userId")?.let { userId ->
            viewedUserId = userId
            // The calls in init block are now redundant due to LaunchedEffect in Screen,
            // but keeping them for ViewModel's internal consistency if used elsewhere.
            // Screen's LaunchedEffect will ensure refresh on re-composition.
            // fetchUserPosts()
            // fetchUserLikedPosts(firebaseAuth.currentUser?.uid)
            // fetchPostAuthorProfile(userId)
        }
    }

    fun fetchUserPosts() {
        val userId = viewedUserId
        if (userId == null) {
            _errorMessage.value = "User ID is missing to fetch posts."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                // Corrected: Use getRunPostsByUsers which takes a list of user IDs
                val result = runPostRepository.getRunPostsByUsers(listOf(userId))
                if (result.isSuccess) {
                    _userPosts.value = result.getOrNull()?.sortedByDescending { it.timestamp } ?: emptyList()
                    Log.d(TAG, "Fetched ${_userPosts.value.size} posts for user $userId")
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to load user posts."
                    Log.e(TAG, "Error fetching user posts for $userId: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred while loading posts."
                Log.e(TAG, "Unexpected error in fetchUserPosts: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Changed from private fun to fun to allow access from Composable
    fun fetchUserLikedPosts(currentLoggedInUserId: String?) {
        if (currentLoggedInUserId == null) {
            _userLikedPostIds.value = emptySet()
            Log.d(TAG, "fetchUserLikedPosts called but currentLoggedInUserId is null.")
            return
        }
        viewModelScope.launch {
            val result = runPostRepository.getLikesByUser(currentLoggedInUserId)
            if (result.isSuccess) {
                _userLikedPostIds.value = result.getOrNull()?.map { it.postId }?.toSet() ?: emptySet()
                Log.d(TAG, "Fetched ${_userLikedPostIds.value.size} liked posts for current user $currentLoggedInUserId")
            } else {
                Log.e(TAG, "Error fetching current user's liked posts: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    // Changed from private fun to fun to allow access from Composable
    fun fetchPostAuthorProfile(userId: String) {
        viewModelScope.launch {
            val result = userRepository.getUserProfile(userId)
            result.onSuccess { user ->
                _postAuthor.value = user
            }.onFailure { e ->
                Log.e(TAG, "Error fetching post author profile for $userId: ${e.message}")
            }
        }
    }

    fun toggleLike(postId: String, isCurrentlyLiked: Boolean) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId == null) {
            _errorMessage.value = "Morate biti prijavljeni za lajkanje."
            Log.w(TAG, "toggleLike called but currentUserId is null.")
            return
        }
        Log.d(TAG, "Toggling like for post $postId by user $currentUserId. Currently liked: $isCurrentlyLiked")

        _isLoadingAction.value = true // Set action loading to true
        _errorMessage.value = null // Clear any previous error messages

        // Optimistic update: Immediately reflect the new like status in the UI
        val previousLikedState = _userLikedPostIds.value
        _userLikedPostIds.value = if (isCurrentlyLiked) {
            _userLikedPostIds.value.minus(postId) // Remove postId from set
        } else {
            _userLikedPostIds.value.plus(postId) // Add postId to set
        }
        Log.d(TAG, "Optimistically updated userLikedPostIds: ${_userLikedPostIds.value}. Icon should change.")

        viewModelScope.launch {
            try {
                val result = if (isCurrentlyLiked) {
                    runPostRepository.unlikePost(postId, currentUserId)
                } else {
                    runPostRepository.likePost(postId, currentUserId)
                }

                result.onSuccess {
                    Log.d(TAG, "Like/Unlike successful for post $postId. Re-fetching posts and likes for confirmation.")
                    // Re-fetch posts for the viewed user to get updated like count
                    fetchUserPosts()
                    // Re-fetch current user's likes to ensure _userLikedPostIds is in sync with DB
                    fetchUserLikedPosts(currentUserId)
                }.onFailure { e ->
                    // Revert optimistic update on failure
                    _userLikedPostIds.value = previousLikedState
                    _errorMessage.value = e.message ?: "Greška pri lajkanju/otlajkavanju objave."
                    Log.e(TAG, "Error liking/unliking post $postId: ${e.message}", e)
                }
            } catch (e: Exception) {
                // Revert optimistic update on unexpected error
                _userLikedPostIds.value = previousLikedState
                _errorMessage.value = e.message ?: "Došlo je do neočekivane greške pri lajkanju."
                Log.e(TAG, "Unexpected error in toggleLike", e)
            } finally {
                _isLoadingAction.value = false // Hide action loading
                Log.d(TAG, "toggleLike operation finished. isLoadingAction: ${_isLoadingAction.value}")
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                val firebaseAuth = FirebaseAuth.getInstance()
                val firestore = FirebaseFirestore.getInstance()
                UserPostsViewModel(
                    runPostRepository = RunPostRepository(firestore),
                    userRepository = UserRepository(firestore),
                    firebaseAuth = firebaseAuth,
                    savedStateHandle = savedStateHandle
                )
            }
        }
    }
}
