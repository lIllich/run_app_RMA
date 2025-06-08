package com.example.run_app_rma.presentation.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.run_app_rma.data.dao.ChallengeDao
import com.example.run_app_rma.data.firestore.model.User
import com.example.run_app_rma.data.firestore.repository.FollowRepository
import com.example.run_app_rma.data.firestore.repository.RunPostRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.example.run_app_rma.data.remote.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val runPostRepository: RunPostRepository,
    private val followRepository: FollowRepository,
    private val challengeDao: ChallengeDao
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

    private val _postCount = MutableStateFlow(0)
    val postCount: StateFlow<Int> = _postCount.asStateFlow()

    private val _unlockedTitles = MutableStateFlow<List<String>>(emptyList())
    val unlockedTitles: StateFlow<List<String>> = _unlockedTitles.asStateFlow()

    private val TAG = "ProfileViewModel"

    init {
        fetchUserProfileAndCounts()
        observeUnlockedTitles()
    }

    private fun observeUnlockedTitles() {
        challengeDao.getUnlockedTitles()
            .onEach { titles ->
                _unlockedTitles.value = titles
            }
            .launchIn(viewModelScope)
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
            _postCount.value = 0
            Log.d(TAG, "fetchUserProfileAndCounts called but currentUserId is null. User not authenticated.")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                // fetch user profile
                val userResult = userRepository.getUserProfile(userId)
                if (userResult.isSuccess) {
                    _currentUser.value = userResult.getOrNull()
                } else {
                    _errorMessage.value = userResult.exceptionOrNull()?.message ?: "Failed to load profile."
                    _currentUser.value = null
                }

                // fetch following count
                val followingCountResult = followRepository.getFollowingCount(userId)
                if (followingCountResult.isSuccess) {
                    _followingCount.value = followingCountResult.getOrNull() ?: 0
                } else {
                    Log.e(TAG, "Error fetching following count: ${followingCountResult.exceptionOrNull()?.message}")
                    _followingCount.value = 0
                }

                // fetch followers count
                val followersCountResult = followRepository.getFollowersCount(userId)
                if (followersCountResult.isSuccess) {
                    _followersCount.value = followersCountResult.getOrNull() ?: 0
                } else {
                    Log.e(TAG, "Error fetching followers count: ${followersCountResult.exceptionOrNull()?.message}")
                    _followersCount.value = 0
                }

                // fetch post count
                val userPostsResult = runPostRepository.getRunPostsByUsers(listOf(userId))
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

    /**
     * Triggers the recalculation and saving of totalDistanceRun for the current user.
     * This function should only be called by authorized users/logic.
     */
    fun recalculateDistances() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _errorMessage.value = "User not logged in. Cannot recalculate distances."
            Log.w(TAG, "recalculateDistances called but currentUserId is null.")
            return
        }

        // only allow specific user to run this for now
        if (userId != "2MKIn3Un7HevvAAROb4z3cWaxFy2") {
            _errorMessage.value = "You do not have permission to perform this action."
            Log.w(TAG, "Unauthorized user $userId attempted to recalculate distances.")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            Log.d(TAG, "Attempting to recalculate distances for user: $userId")
            val result = userRepository.recalculateAndSaveTotalDistanceRun(userId)
            result.onSuccess {
                Log.d(TAG, "Total distance recalculated successfully for user $userId.")
                _errorMessage.value = "Uspješno preračunata ukupna udaljenost."
                fetchUserProfileAndCounts() // refresh the UI to show the new value
            }.onFailure { e ->
                _errorMessage.value = "Greška pri preračunavanju udaljenosti: ${e.message}"
                Log.e(TAG, "Failed to recalculate totalDistanceRun for user $userId: ${e.message}", e)
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        authRepository.logout()
        _currentUser.value = null
        _errorMessage.value = null
        _followingCount.value = 0
        _followersCount.value = 0
        _postCount.value = 0
        _unlockedTitles.value = emptyList()
    }

    fun clearMessages() {
        _errorMessage.value = null
    }

    class Factory(
        private val userRepository: UserRepository,
        private val firebaseAuth: FirebaseAuth,
        private val authRepository: AuthRepository,
        private val runPostRepository: RunPostRepository,
        private val followRepository: FollowRepository,
        private val challengeDao: ChallengeDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(
                    userRepository,
                    firebaseAuth,
                    authRepository,
                    runPostRepository,
                    followRepository,
                    challengeDao
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
