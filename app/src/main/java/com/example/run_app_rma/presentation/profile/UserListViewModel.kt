package com.example.run_app_rma.presentation.profile

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.run_app_rma.data.firestore.model.User
import com.example.run_app_rma.data.firestore.repository.FollowRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateMapOf

class UserListViewModel(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
    val firebaseAuth: FirebaseAuth,
    val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isFollowingMap = mutableStateMapOf<String, Boolean>()
    val isFollowingMap: Map<String, Boolean> = _isFollowingMap

    private val _isTogglingFollowMap = mutableStateMapOf<String, Boolean>()
    val isTogglingFollowMap: Map<String, Boolean> = _isTogglingFollowMap

    private val TAG = "UserListViewModel"

    private val currentLoggedInUserId: String?
        get() = firebaseAuth.currentUser?.uid

    init {
        val userId = savedStateHandle.get<String>("userId")
        val listType = savedStateHandle.get<String>("listType") // following or followers

        if (userId != null && listType != null) {
            loadUsers(userId, listType)
        } else {
            _errorMessage.value = "Missing user ID or list type for UserListViewModel."
            Log.e(TAG, "Missing userId or listType in SavedStateHandle.")
        }
    }

    private fun loadUsers(userId: String, listType: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val userIdsResult = when (listType) {
                    "following" -> followRepository.getFollowingIds(userId)
                    "followers" -> followRepository.getFollowerIds(userId)
                    else -> Result.failure(IllegalArgumentException("Invalid list type: $listType"))
                }

                if (userIdsResult.isSuccess) {
                    val ids = userIdsResult.getOrNull() ?: emptyList()
                    Log.d(TAG, "Fetched ${ids.size} IDs for $listType for user $userId.")

                    val userList = mutableListOf<User>()
                    ids.forEach { id ->
                        val userResult = userRepository.getUserProfile(id)
                        userResult.onSuccess { user ->
                            userList.add(user)
                        }.onFailure { e ->
                            Log.e(TAG, "Failed to fetch user profile for ID $id: ${e.message}")
                        }
                    }
                    // sort by display name
                    _users.value = userList.sortedBy { it.displayName.lowercase() }

                    // fetch following status relative to the current user
                    updateFollowingStatusForUsers(userList.map { it.id })

                } else {
                    _errorMessage.value = userIdsResult.exceptionOrNull()?.message ?: "Failed to load user IDs."
                    Log.e(TAG, "Error fetching user IDs: ${userIdsResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred while loading users."
                Log.e(TAG, "Unexpected error in loadUsers: ${e.message}", e)
            } finally {
                _isLoading.value = false
                Log.d(TAG, "loadUsers finished. isLoading set to false.")
            }
        }
    }

    /**
     * Updates the following status for a list of target users relative to the current logged-in user.
     * This is crucial for correctly displaying the "Prati" or "Pratim" button state.
     */
    private fun updateFollowingStatusForUsers(userIds: List<String>) {
        currentLoggedInUserId?.let { currentId ->
            viewModelScope.launch {
                userIds.forEach { targetUserId ->
                    val result = followRepository.isFollowing(currentId, targetUserId)
                    result.onSuccess { isFollowing ->
                        _isFollowingMap[targetUserId] = isFollowing
                    }.onFailure { e ->
                        Log.e(TAG, "Error checking following status for $targetUserId: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Toggles the follow status for a given target user.
     * Updates the UI state and interacts with the repository.
     */
    fun toggleFollow(targetUserId: String) {
        val currentId = currentLoggedInUserId ?: run {
            _errorMessage.value = "User not logged in."
            return
        }

        viewModelScope.launch {
            _isTogglingFollowMap[targetUserId] = true   // set loading for this specific user
            _errorMessage.value = null                  // clear previous error messages

            val isCurrentlyFollowing = _isFollowingMap[targetUserId] ?: false
            val result = if (isCurrentlyFollowing) {
                followRepository.unfollowUser(currentId, targetUserId)
            } else {
                followRepository.followUser(currentId, targetUserId)
            }

            result.onSuccess {
                _isFollowingMap[targetUserId] = !isCurrentlyFollowing
            }.onFailure { e ->
                _errorMessage.value = "Failed to toggle follow status: ${e.message}"
                Log.e(TAG, "Error toggling follow for $targetUserId: ${e.message}", e)
            }

            _isTogglingFollowMap[targetUserId] = false  // reset loading

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
                UserListViewModel(
                    followRepository = FollowRepository(firestore),
                    userRepository = UserRepository(firestore),
                    firebaseAuth = firebaseAuth,
                    savedStateHandle = savedStateHandle
                )
            }
        }
    }
}
