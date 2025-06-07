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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingAction = MutableStateFlow(false)
    val isLoadingAction: StateFlow<Boolean> = _isLoadingAction.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _postAuthor = MutableStateFlow<User?>(null)
    val postAuthor: StateFlow<User?> = _postAuthor.asStateFlow()

    private val TAG = "UserPostsViewModel"

    var viewedUserId: String? = null

    init {
        savedStateHandle.get<String>("userId")?.let { userId ->
            viewedUserId = userId
        }
    }

    fun fetchAllUserDataForScreen() {
        val userId = viewedUserId
        val currentLoggedInUserId = firebaseAuth.currentUser?.uid

        if (userId == null) {
            _errorMessage.value = "User ID is missing to fetch posts."
            _isLoading.value = false
            _isRefreshing.value = false
            return
        }

        // only set initial loading if it's the very first load, otherwise rely on isRefreshing
        if (!_isLoading.value) {
            _isRefreshing.value = true
        }

        _errorMessage.value = null
        viewModelScope.launch {
            try {
                // fetch user's posts
                val postsResult = runPostRepository.getRunPostsByUsers(listOf(userId))
                if (postsResult.isSuccess) {
                    _userPosts.value = postsResult.getOrNull()?.sortedByDescending { it.timestamp } ?: emptyList()
                    Log.d(TAG, "Fetched ${_userPosts.value.size} posts for user $userId")
                } else {
                    _errorMessage.value = postsResult.exceptionOrNull()?.message ?: "Failed to load user posts."
                    Log.e(TAG, "Error fetching user posts for $userId: ${postsResult.exceptionOrNull()?.message}")
                }

                // fetch current user's liked posts
                if (currentLoggedInUserId != null) {
                    val likesResult = runPostRepository.getLikesByUser(currentLoggedInUserId)
                    if (likesResult.isSuccess) {
                        _userLikedPostIds.value = likesResult.getOrNull()?.map { it.postId }?.toSet() ?: emptySet()
                        Log.d(TAG, "Fetched ${_userLikedPostIds.value.size} liked posts for current user $currentLoggedInUserId")
                    } else {
                        Log.e(TAG, "Error fetching current user's liked posts: ${likesResult.exceptionOrNull()?.message}")
                    }
                } else {
                    _userLikedPostIds.value = emptySet()
                    Log.d(TAG, "Current logged in user is null, liked posts set to empty.")
                }

                // fetch the post author's profile
                val authorResult = userRepository.getUserProfile(userId)
                if (authorResult.isSuccess) {
                    _postAuthor.value = authorResult.getOrNull()
                } else {
                    Log.e(
                        TAG,
                        "Error fetching post author profile for $userId: ${authorResult.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred while loading data."
                Log.e(TAG, "Unexpected error in fetchAllUserDataForScreen: ${e.message}", e)
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
                Log.d(TAG, "fetchAllUserDataForScreen finished. isLoading set to false, isRefreshing set to false.")
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

        _isLoadingAction.value = true
        _errorMessage.value = null

        val previousLikedState = _userLikedPostIds.value
        _userLikedPostIds.value = if (isCurrentlyLiked) {
            _userLikedPostIds.value.minus(postId)
        } else {
            _userLikedPostIds.value.plus(postId)
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
                    fetchAllUserDataForScreen()     // refresh all data for consistency
                }.onFailure { e ->
                    _userLikedPostIds.value = previousLikedState
                    _errorMessage.value = e.message ?: "Greška pri lajkanju/otlajkavanju objave."
                    Log.e(TAG, "Error liking/unliking post $postId: ${e.message}", e)
                }
            } catch (e: Exception) {
                _userLikedPostIds.value = previousLikedState
                _errorMessage.value = e.message ?: "Došlo je do neočekivane greške pri lajkanju."
                Log.e(TAG, "Unexpected error in toggleLike", e)
            } finally {
                _isLoadingAction.value = false
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
