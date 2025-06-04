package com.example.run_app_rma.presentation.follow

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.run_app_rma.data.firestore.model.User
import com.example.run_app_rma.data.firestore.repository.FollowRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class FollowViewModel(
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _users = mutableStateListOf<User>()
    val users: List<User> = _users

    private val _isFollowingMap = mutableStateMapOf<String, Boolean>()
    val isFollowingMap: Map<String, Boolean> = _isFollowingMap

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    init {
        observeSearchQuery()
        fetchFollowingStatusForCurrentUser()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300L) // Debounce for 300ms
                .distinctUntilChanged()
                .filter { it.isNotBlank() } // Only search if query is not blank
                .collect { query ->
                    searchUsers(query)
                }
        }
    }

    private fun fetchFollowingStatusForCurrentUser() {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                val result = followRepository.getFollowingIds(userId)
                result.onSuccess { followingIds ->
                    _isFollowingMap.clear()
                    followingIds.forEach { followingId ->
                        _isFollowingMap[followingId] = true
                    }
                }.onFailure { e ->
                    _errorMessage.value = "Failed to fetch following status: ${e.message}"
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _users.clear()
        }
    }

    private fun searchUsers(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = userRepository.searchUsers(query)
            result.onSuccess { fetchedUsers ->
                // Filter out the current user from the search results
                _users.clear()
                _users.addAll(fetchedUsers.filter { it.id != currentUserId })
                // After fetching users, update their following status
                updateFollowingStatusForUsers(fetchedUsers.map { it.id })
            }.onFailure { e ->
                _errorMessage.value = "Error searching users: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    private fun updateFollowingStatusForUsers(userIds: List<String>) {
        currentUserId?.let { currentId ->
            viewModelScope.launch {
                userIds.forEach { targetUserId ->
                    val result = followRepository.isFollowing(currentId, targetUserId)
                    result.onSuccess { isFollowing ->
                        _isFollowingMap[targetUserId] = isFollowing
                    }.onFailure { e ->
                        // Log error but don't block UI
                        println("Error checking following status for $targetUserId: ${e.message}")
                    }
                }
            }
        }
    }


    fun toggleFollow(targetUserId: String) {
        val currentLoggedInUserId = currentUserId ?: run {
            _errorMessage.value = "User not logged in."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val isCurrentlyFollowing = _isFollowingMap[targetUserId] ?: false

            if (isCurrentlyFollowing) {
                val result = followRepository.unfollowUser(currentLoggedInUserId, targetUserId)
                result.onSuccess {
                    _isFollowingMap[targetUserId] = false
                }.onFailure { e ->
                    _errorMessage.value = "Failed to unfollow user: ${e.message}"
                }
            } else {
                val result = followRepository.followUser(currentLoggedInUserId, targetUserId)
                result.onSuccess {
                    _isFollowingMap[targetUserId] = true
                }.onFailure { e ->
                    _errorMessage.value = "Failed to follow user: ${e.message}"
                }
            }
            _isLoading.value = false
        }
    }

    class Factory(
        private val userRepository: UserRepository,
        private val followRepository: FollowRepository,
        private val firebaseAuth: FirebaseAuth
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FollowViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FollowViewModel(userRepository, followRepository, firebaseAuth) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}