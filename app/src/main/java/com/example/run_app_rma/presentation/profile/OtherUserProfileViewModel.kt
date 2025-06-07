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
import com.example.run_app_rma.data.firestore.repository.FollowRepository
import com.example.run_app_rma.data.firestore.repository.RunPostRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OtherUserProfileViewModel(
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
    private val runPostRepository: RunPostRepository,
    private val followRepository: FollowRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _viewedUser = MutableStateFlow<User?>(null)
    val viewedUser: StateFlow<User?> = _viewedUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _followingCount = MutableStateFlow(0)
    val followingCount: StateFlow<Int> = _followingCount.asStateFlow()

    private val _followersCount = MutableStateFlow(0)
    val followersCount: StateFlow<Int> = _followersCount.asStateFlow()

    private val _viewedUserPosts = MutableStateFlow<List<RunPost>>(emptyList())
    val viewedUserPosts: StateFlow<List<RunPost>> = _viewedUserPosts.asStateFlow()

    private val _userLikedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val userLikedPostIds: StateFlow<Set<String>> = _userLikedPostIds.asStateFlow()

    private val _isFollowingViewedUser = MutableStateFlow(false)
    val isFollowingViewedUser: StateFlow<Boolean> = _isFollowingViewedUser.asStateFlow()

    private val _isTogglingFollow = MutableStateFlow(false)
    val isTogglingFollow: StateFlow<Boolean> = _isTogglingFollow.asStateFlow()

    private val TAG = "OtherUserProfileVM"

    private var currentViewedUserId: String? = null

    val currentLoggedInUserId: String?
        get() = firebaseAuth.currentUser?.uid


    init {
        savedStateHandle.get<String>("userId")?.let { userId ->
            currentViewedUserId = userId
            fetchUserProfileAndData(userId)
            fetchUserLikedPosts(currentLoggedInUserId)
            fetchFollowingStatus(userId)
        }
    }

    fun fetchUserProfileAndData(userId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                // fetch user profile
                val userResult = userRepository.getUserProfile(userId)
                if (userResult.isSuccess) {
                    _viewedUser.value = userResult.getOrNull()
                } else {
                    _errorMessage.value = userResult.exceptionOrNull()?.message ?: "Failed to load user profile."
                    _viewedUser.value = null
                }

                // fetch following count
                val followingCountResult = followRepository.getFollowingCount(userId)
                if (followingCountResult.isSuccess) {
                    _followingCount.value = followingCountResult.getOrNull() ?: 0
                } else {
                    Log.e(TAG, "Error fetching following count for $userId: ${followingCountResult.exceptionOrNull()?.message}")
                    _followingCount.value = 0
                }

                // fetch followers count
                val followersCountResult = followRepository.getFollowersCount(userId)
                if (followersCountResult.isSuccess) {
                    _followersCount.value = followersCountResult.getOrNull() ?: 0
                } else {
                    Log.e(TAG, "Error fetching followers count for $userId: ${followersCountResult.exceptionOrNull()?.message}")
                    _followersCount.value = 0
                }

                // fetch user's posts
                val postsResult = runPostRepository.getRunPostsByUsers(listOf(userId))
                if (postsResult.isSuccess) {
                    _viewedUserPosts.value = postsResult.getOrNull()?.sortedByDescending { it.timestamp } ?: emptyList()
                } else {
                    Log.e(TAG, "Error fetching posts for user $userId: ${postsResult.exceptionOrNull()?.message}")
                    _viewedUserPosts.value = emptyList()
                }

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred while fetching user data."
                Log.e(TAG, "Unexpected error in fetchUserProfileAndData: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchUserLikedPosts(currentLoggedInUserId: String?) {
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

    private fun fetchFollowingStatus(viewedUserId: String) {
        val currentId = currentLoggedInUserId
        if (currentId == null) {
            _isFollowingViewedUser.value = false
            Log.w(TAG, "fetchFollowingStatus: currentId is null, cannot check following status.")
            return
        }
        if (currentId == viewedUserId) {    // cannot follow yourself
            _isFollowingViewedUser.value = false
            Log.d(TAG, "fetchFollowingStatus: currentId and viewedUserId are same, cannot follow self.")
            return
        }
        // explicit validation for blank IDs
        if (currentId.isBlank() || viewedUserId.isBlank()) {
            Log.e(TAG,
                "fetchFollowingStatus: Attempted to check following status with blank ID: currentId='$currentId'," +
                        "viewedUserId='$viewedUserId'")
            _isFollowingViewedUser.value = false
            return
        }

        viewModelScope.launch {
            val result = followRepository.isFollowing(currentId, viewedUserId)
            result.onSuccess { isFollowing ->
                _isFollowingViewedUser.value = isFollowing
                Log.d(TAG, "fetchFollowingStatus: isFollowing for $currentId -> $viewedUserId is $isFollowing")
            }.onFailure { e ->
                Log.e(TAG, "Error fetching following status for viewed user: ${e.message}")
                _isFollowingViewedUser.value = false
            }
        }
    }

    fun toggleFollowViewedUser() {
        val currentId = currentLoggedInUserId ?: run {
            _errorMessage.value = "Morate biti prijavljeni za praćenje korisnika."
            Log.w(TAG, "toggleFollowViewedUser: currentId is null, user not logged in.")
            return
        }
        val targetUserId = currentViewedUserId ?: run {
            _errorMessage.value = "Nije moguće pronaći korisnika za praćenje."
            Log.w(TAG, "toggleFollowViewedUser: targetUserId is null.")
            return
        }
        if (currentId == targetUserId) {
            _errorMessage.value = "Ne možete pratiti sami sebe."
            Log.w(TAG, "toggleFollowViewedUser: currentId and targetUserId are same.")
            return
        }
        // explicit validation for blank IDs
        if (currentId.isBlank() || targetUserId.isBlank()) {
            _errorMessage.value = "Korisnički ID-jevi ne mogu biti prazni."
            Log.e(TAG, "toggleFollowViewedUser: Attempted to toggle follow with blank ID: currentId='$currentId', targetUserId='$targetUserId'")
            return
        }

        viewModelScope.launch {
            _isTogglingFollow.value = true
            _errorMessage.value = null
            Log.d(TAG, "toggleFollowViewedUser: Toggling follow status for $targetUserId by $currentId.")

            val isCurrentlyFollowing = _isFollowingViewedUser.value
            val result = if (isCurrentlyFollowing) {
                Log.d(TAG, "toggleFollowViewedUser: Calling unfollowUser for $currentId -> $targetUserId")
                followRepository.unfollowUser(currentId, targetUserId)
            } else {
                Log.d(TAG, "toggleFollowViewedUser: Calling followUser for $currentId -> $targetUserId")
                followRepository.followUser(currentId, targetUserId)
            }

            result.onSuccess {
                _isFollowingViewedUser.value = !isCurrentlyFollowing
                Log.d(TAG, "toggleFollowViewedUser: Follow/Unfollow successful. New status: ${_isFollowingViewedUser.value}")
                // re-fetch follower/following counts for the viewed user to reflect the change
                currentViewedUserId?.let { id ->
                    followRepository.getFollowersCount(id).onSuccess { count ->
                        _followersCount.value = count
                        Log.d(TAG, "toggleFollowViewedUser: Updated followers count for $id to $count")
                    }.onFailure { e ->
                        Log.e(TAG, "toggleFollowViewedUser: Error updating followers count: ${e.message}")
                    }
                    followRepository.getFollowingCount(id).onSuccess { count ->
                        _followingCount.value = count
                        Log.d(TAG, "toggleFollowViewedUser: Updated following count for $id to $count")
                    }.onFailure { e ->
                        Log.e(TAG, "toggleFollowViewedUser: Error updating following count: ${e.message}")
                    }
                }
            }.onFailure { e ->
                _errorMessage.value = "Greška pri praćenju/otpraćivanju: ${e.message}"
                Log.e(TAG, "toggleFollowViewedUser: Error toggling follow for $targetUserId: ${e.message}", e)
            }
            _isTogglingFollow.value = false // Stop loading
            Log.d(TAG, "toggleFollowViewedUser: Finished toggling follow status.")
        }
    }

    fun toggleLike(postId: String, isCurrentlyLiked: Boolean) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId == null) {
            _errorMessage.value = "Morate biti prijavljeni za lajkanje."
            Log.w(TAG, "toggleLike called but currentUserId is null.")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = if (isCurrentlyLiked) {
                runPostRepository.unlikePost(postId, currentUserId)
            } else {
                runPostRepository.likePost(postId, currentUserId)
            }

            result.onSuccess {
                Log.d(TAG, "Like/Unlike successful for post $postId. Re-fetching posts and likes.")
                currentViewedUserId?.let { fetchUserProfileAndData(it) }    // re-fetch posts for the viewed user
                fetchUserLikedPosts(currentUserId)                          // re-fetch current user's likes
            }.onFailure { e ->
                _errorMessage.value = "Greška pri lajkanju objave: ${e.message}"
                Log.e(TAG, "Error liking/unliking post $postId: ${e.message}", e)
            }
            _isLoading.value = false
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
                OtherUserProfileViewModel(
                    userRepository = UserRepository(firestore),
                    firebaseAuth = firebaseAuth,
                    runPostRepository = RunPostRepository(firestore),
                    followRepository = FollowRepository(firestore),
                    savedStateHandle = savedStateHandle
                )
            }
        }
    }
}
