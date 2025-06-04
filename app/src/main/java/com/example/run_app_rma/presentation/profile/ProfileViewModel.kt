package com.example.run_app_rma.presentation.profile

import android.util.Log // Import Log for debugging
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.run_app_rma.data.firestore.model.User
import com.example.run_app_rma.data.firestore.repository.FollowRepository // Import FollowRepository
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
    private val runPostRepository: RunPostRepository,
    private val followRepository: FollowRepository // Add FollowRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _followingCount = MutableStateFlow(0)
    val followingCount: StateFlow<Int> = _followingCount.asStateFlow()

    private val _followersCount = MutableStateFlow(0)
    val followersCount: StateFlow<Int> = _followersCount.asStateFlow()

    // New: State for post count
    private val _postCount = MutableStateFlow(0)
    val postCount: StateFlow<Int> = _postCount.asStateFlow()


    private val TAG = "ProfileViewModel"

    init {
        fetchUserProfileAndCounts() // Call new combined fetch function
    }

    /**
     * Fetches the current user's profile and their following/followers counts.
     * This function is called on init and after profile updates.
     */
    fun fetchUserProfileAndCounts() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _errorMessage.value = "User not logged in."
            _currentUser.value = null
            _followingCount.value = 0
            _followersCount.value = 0
            _postCount.value = 0 // Reset post count as well
            Log.d(TAG, "fetchUserProfileAndCounts called but currentUserId is null. User not authenticated.")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                // Fetch user profile
                val userResult = userRepository.getUserProfile(userId)
                if (userResult.isSuccess) {
                    _currentUser.value = userResult.getOrNull()
                } else {
                    _errorMessage.value = userResult.exceptionOrNull()?.message ?: "Failed to load profile."
                    _currentUser.value = null
                }

                // Fetch following count
                val followingCountResult = followRepository.getFollowingCount(userId)
                if (followingCountResult.isSuccess) {
                    _followingCount.value = followingCountResult.getOrNull() ?: 0
                } else {
                    Log.e(TAG, "Error fetching following count: ${followingCountResult.exceptionOrNull()?.message}")
                    _followingCount.value = 0
                }

                // Fetch followers count
                val followersCountResult = followRepository.getFollowersCount(userId)
                if (followersCountResult.isSuccess) {
                    _followersCount.value = followersCountResult.getOrNull() ?: 0
                } else {
                    Log.e(TAG, "Error fetching followers count: ${followersCountResult.exceptionOrNull()?.message}")
                    _followersCount.value = 0
                }

                // New: Fetch post count
                val userPostsResult = runPostRepository.getRunPostForUser(userId)
                if (userPostsResult.isSuccess) {
                    _postCount.value = userPostsResult.getOrNull()?.size ?: 0
                } else {
                    Log.e(TAG, "Error fetching user posts count: ${userPostsResult.exceptionOrNull()?.message}")
                    _postCount.value = 0
                }

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred while fetching profile and counts."
                Log.e(TAG, "Unexpected error in fetchUserProfileAndCounts: ${e.message}", e)
            } finally {
                _isLoading.value = false
                Log.d(TAG, "fetchUserProfileAndCounts finished. isLoading set to false.")
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _currentUser.value = null
        _errorMessage.value = null
        _followingCount.value = 0
        _followersCount.value = 0
        _postCount.value = 0 // Clear post count on logout
    }

    fun clearMessages() {
        _errorMessage.value = null
    }

    /**
     * Factory for creating ProfileViewModel instances.
     * Required for injecting dependencies into the ViewModel.
     */
    class Factory(
        private val userRepository: UserRepository,
        private val firebaseAuth: FirebaseAuth,
        private val authRepository: AuthRepository,
        private val runPostRepository: RunPostRepository,
        private val followRepository: FollowRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(userRepository, firebaseAuth, authRepository, runPostRepository, followRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
