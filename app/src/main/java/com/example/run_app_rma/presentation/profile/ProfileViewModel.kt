package com.example.run_app_rma.presentation.profile

import android.util.Log // Import Log for debugging
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.run_app_rma.data.firestore.model.RunPost
import com.example.run_app_rma.data.firestore.model.User
import com.example.run_app_rma.data.firestore.repository.RunPostRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.example.run_app_rma.data.remote.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val runPostRepository: RunPostRepository // Add RunPostRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // New: State for user's own posts
    private val _userPosts = MutableStateFlow<List<RunPost>>(emptyList())
    val userPosts: StateFlow<List<RunPost>> = _userPosts.asStateFlow()

    // New: State for user profiles associated with the displayed posts (even if it's just the current user)
    // This is a map where key is userId and value is User object
    private val _userProfilesForPosts = MutableStateFlow<Map<String, User>>(emptyMap())
    val userProfilesForPosts: StateFlow<Map<String, User>> = _userProfilesForPosts.asStateFlow()

    // New: State for posts liked by the current user (needed for RunPostCard to show like status)
    private val _userLikedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val userLikedPostIds: StateFlow<Set<String>> = _userLikedPostIds.asStateFlow()

    private val TAG = "ProfileViewModel"

    init {
        // Initial fetch when ViewModel is created
        fetchUserProfileAndPosts()
    }

    /**
     * Fetches the current user's profile, their posts, and their liked posts.
     * This combined function is called on init and after profile updates.
     */
    fun fetchUserProfileAndPosts() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _errorMessage.value = "User not logged in."
            _currentUser.value = null
            _userPosts.value = emptyList()
            _userLikedPostIds.value = emptySet()
            _userProfilesForPosts.value = emptyMap()
            Log.d(TAG, "fetchUserProfileAndPosts called but currentUserId is null. User not authenticated.")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                // Fetch user profile
                val userResult = userRepository.getUserProfile(userId)
                if (userResult.isSuccess) {
                    val user = userResult.getOrNull()
                    _currentUser.value = user
                    if (user != null) {
                        // Add current user's profile to the map for posts
                        _userProfilesForPosts.value = _userProfilesForPosts.value.toMutableMap().apply {
                            put(userId, user)
                        }
                    }
                    // Fetch user's posts after profile is loaded
                    fetchUserPosts(userId)
                    // Fetch user's liked posts
                    fetchUserLikedPosts(userId)
                } else {
                    _errorMessage.value = userResult.exceptionOrNull()?.message ?: "Failed to load profile."
                    _currentUser.value = null
                    _userPosts.value = emptyList()
                    _userLikedPostIds.value = emptySet()
                    _userProfilesForPosts.value = emptyMap()
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred while fetching profile and posts."
                Log.e(TAG, "Unexpected error in fetchUserProfileAndPosts: ${e.message}", e)
            } finally { // 'finally' block is correctly placed here
                _isLoading.value = false
                Log.d(TAG, "fetchUserProfileAndPosts finished. isLoading set to false.")
            }
        }
    }

    /**
     * Fetches all run posts created by a specific user.
     * @param userId The ID of the user whose posts to fetch.
     */
    private fun fetchUserPosts(userId: String) {
        viewModelScope.launch {
            val result = runPostRepository.getRunPostForUser(userId)
            if (result.isSuccess) {
                // Sort posts by timestamp in descending order for display
                _userPosts.value = result.getOrNull()?.sortedByDescending { it.timestamp } ?: emptyList()
                Log.d(TAG, "Fetched ${_userPosts.value.size} posts for user $userId")
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to load user posts."
                _userPosts.value = emptyList()
                Log.e(TAG, "Error fetching user posts: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Fetches the IDs of posts liked by the current user.
     * @param userId The ID of the current user.
     */
    private fun fetchUserLikedPosts(userId: String) {
        viewModelScope.launch {
            val result = runPostRepository.getLikesByUser(userId)
            if (result.isSuccess) {
                _userLikedPostIds.value = result.getOrNull()?.map { it.postId }?.toSet() ?: emptySet()
                Log.d(TAG, "Fetched ${_userLikedPostIds.value.size} liked posts for user $userId")
            } else {
                Log.e(TAG, "Error fetching user liked posts: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Toggles the like status of a post.
     * @param postId The ID of the post to like/unlike.
     * @param isCurrentlyLiked Boolean indicating if the post is currently liked by the user.
     */
    fun toggleLike(postId: String, isCurrentlyLiked: Boolean) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId == null) {
            _errorMessage.value = "Morate biti prijavljeni za lajkanje."
            Log.w(TAG, "toggleLike called but currentUserId is null.")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true // Set loading state for like operation
            val result = if (isCurrentlyLiked) {
                runPostRepository.unlikePost(postId, currentUserId)
            } else {
                runPostRepository.likePost(postId, currentUserId)
            }

            result.onSuccess {
                Log.d(TAG, "Like/Unlike successful for post $postId. Refreshing posts and likes.")
                // Refresh posts and likes to reflect the change
                fetchUserPosts(currentUserId) // Re-fetch user's posts to update likesCount
                fetchUserLikedPosts(currentUserId) // Re-fetch user's liked posts to update liked status
            }.onFailure { e ->
                _errorMessage.value = "Greška pri lajkanju objave: ${e.message}"
                Log.e(TAG, "Error liking/unliking post $postId: ${e.message}", e)
            }
            _isLoading.value = false // Reset loading state on completion

        }
    }

    /**
     * Clears any error or success messages.
     */
    fun clearMessages() {
        _errorMessage.value = null
        // _successMessage.value = null // Uncomment if you add a success message to ProfileViewModel
    }

    /**
     * Factory for creating ProfileViewModel instances.
     * Required for injecting dependencies into the ViewModel.
     */
    class Factory(
        private val userRepository: UserRepository,
        private val firebaseAuth: FirebaseAuth,
        private val authRepository: AuthRepository,
        private val runPostRepository: RunPostRepository // Add RunPostRepository to factory
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(userRepository, firebaseAuth, authRepository, runPostRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
