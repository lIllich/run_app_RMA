package com.example.run_app_rma.presentation.feed

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.run_app_rma.data.firestore.model.RunPost
import com.example.run_app_rma.data.firestore.model.User
import com.example.run_app_rma.data.firestore.repository.FollowRepository
import com.example.run_app_rma.data.firestore.repository.RunPostRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.example.run_app_rma.widget.FeedWidgetProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedViewModel(
    private val application: Application,
    private val runPostRepository: RunPostRepository,
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
    private val followRepository: FollowRepository
) : ViewModel() {

    private val _allPosts = mutableStateListOf<RunPost>()
    val allPosts: List<RunPost> = _allPosts

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingAction = MutableStateFlow(false)
    val isLoadingAction: StateFlow<Boolean> = _isLoadingAction.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _userProfiles = mutableStateOf(emptyMap<String, User>())
    val userProfiles: State<Map<String, User>> = _userProfiles

    private val _userLikedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val userLikedPostIds: StateFlow<Set<String>> = _userLikedPostIds.asStateFlow()

    private val TAG = "FeedViewModel"

    init {
        loadFeedPosts()
    }

    fun loadFeedPosts() {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId == null) {
            _errorMessage.value = "Korisnik nije prijavljen."
            Log.d(TAG, "loadFeedPosts called but currentUserId is null. User not authenticated.")
            _isInitialLoading.value = false
            _isRefreshing.value = false
            return
        }

        Log.d(TAG, "loadFeedPosts called for userId: $currentUserId")

        // only set initial loading if it's the very first load, otherwise rely on isRefreshing
        if (!_isInitialLoading.value) {
            _isRefreshing.value = true
        }

        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val followingIdsResult = followRepository.getFollowingIds(currentUserId)
                Log.d(TAG, "Fetched following IDs result: $followingIdsResult")

                if (followingIdsResult.isSuccess) {
                    val followingIds = followingIdsResult.getOrNull() ?: emptyList()
                    Log.d(TAG, "User is following: $followingIds")

                    if (followingIds.isEmpty()) {
                        _allPosts.clear()
                        _userProfiles.value = emptyMap()
                        _userLikedPostIds.value = emptySet()
                        _errorMessage.value = "Ne pratite nijednog korisnika. Pratite nekoga da vidite objave."
                        _isInitialLoading.value = false
                        _isRefreshing.value = false
                        return@launch
                    }

                    val followedUsersPostsResult = runPostRepository.getRunPostsByUsers(followingIds)
                    Log.d(TAG, "Fetched followed users posts result: $followedUsersPostsResult")

                    val userLikesResult = runPostRepository.getLikesByUser(currentUserId)
                    Log.d(TAG, "Fetched user likes result: $userLikesResult")


                    if (followedUsersPostsResult.isSuccess && userLikesResult.isSuccess) {
                        val allFetchedPosts = followedUsersPostsResult.getOrNull() ?: emptyList()
                        val userLikedPostIds = userLikesResult.getOrNull()?.map { it.postId }?.toSet() ?: emptySet()

                        Log.d(TAG, "Total posts fetched: ${allFetchedPosts.size}")
                        Log.d(TAG, "User liked post IDs: $userLikedPostIds")

                        _allPosts.clear()
                        _userLikedPostIds.value = userLikedPostIds

                        _allPosts.addAll(allFetchedPosts.sortedByDescending { it.timestamp })

                        Log.d(TAG, "All posts count: ${_allPosts.size}")

                        FeedWidgetProvider.sendUpdateBroadcast(application)

                        val postsToFetchUserProfilesFor = _allPosts.map { it.userId }.distinct()
                        fetchUserProfilesForPosts(postsToFetchUserProfilesFor)

                    } else {
                        val errorMsg = "Greška pri učitavanju objava: " +
                                (followedUsersPostsResult.exceptionOrNull()?.message ?: "") +
                                (userLikesResult.exceptionOrNull()?.message ?: "")
                        _errorMessage.value = errorMsg
                        Log.e(TAG, "Error loading feed posts: $errorMsg",
                            followedUsersPostsResult.exceptionOrNull() ?: userLikesResult.exceptionOrNull())
                    }
                } else {
                    _errorMessage.value = followingIdsResult.exceptionOrNull()?.message ?: "Greška pri učitavanju korisnika koje pratite."
                    Log.e(TAG, "Error fetching following IDs: ${followingIdsResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Došlo je do greške: ${e.message}"
                Log.e(TAG, "Unexpected error in loadFeedPosts: ${e.message}", e)
            } finally {
                _isInitialLoading.value = false
                _isRefreshing.value = false         // reset refreshing state after completion/error
                Log.d(TAG, "loadFeedPosts finished. isInitialLoading set to false, isRefreshing set to false.")
            }
        }
    }

    private fun fetchUserProfilesForPosts(userIds: List<String>) {
        viewModelScope.launch {
            val profiles = mutableMapOf<String, User>()
            userIds.forEach { userId ->
                val result = userRepository.getUserProfile(userId)
                result.onSuccess { user ->
                    profiles[userId] = user
                    Log.d(TAG, "Fetched profile for user: ${user.displayName} ($userId)")
                }.onFailure { e ->
                    Log.e(TAG, "Error fetching profile for user $userId: ${e.message}", e)
                }
            }
            _userProfiles.value = profiles
            Log.d(TAG, "User profiles map updated. Total profiles: ${_userProfiles.value.size}")
        }
    }

    fun toggleLike(postId: String, isCurrentlyLiked: Boolean) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId == null) {
            _errorMessage.value = "Morate biti prijavljeni za lajkanje."
            Log.w(TAG, "toggleLike called but currentUserId is null.")
            return
        }

        val previousLikedState = _userLikedPostIds.value
        _userLikedPostIds.value = if (isCurrentlyLiked) {
            _userLikedPostIds.value.minus(postId)
        } else {
            _userLikedPostIds.value.plus(postId)
        }
        Log.d(TAG, "Optimistically updated userLikedPostIds in FeedViewModel: ${_userLikedPostIds.value}")

        viewModelScope.launch {
            val result = if (isCurrentlyLiked) {
                runPostRepository.unlikePost(postId, currentUserId)
            } else {
                runPostRepository.likePost(postId, currentUserId)
            }

            result.onSuccess {
                Log.d(TAG, "Like/Unlike successful for post $postId. Refreshing data.")
                loadFeedPosts()
            }.onFailure { e ->
                _userLikedPostIds.value = previousLikedState
                _errorMessage.value = "Greška pri lajkanju objave: ${e.message}"
                Log.e(TAG, "Error liking/unliking post $postId: ${e.message}", e)
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
        Log.d(TAG, "Messages cleared.")
    }

    class Factory(
        private val application: Application,
        private val runPostRepository: RunPostRepository,
        private val userRepository: UserRepository,
        private val firebaseAuth: FirebaseAuth,
        private val followRepository: FollowRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FeedViewModel(application, runPostRepository, userRepository, firebaseAuth, followRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
