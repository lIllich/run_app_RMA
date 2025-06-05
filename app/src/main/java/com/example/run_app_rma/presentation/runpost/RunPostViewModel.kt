package com.example.run_app_rma.presentation.runpost

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

class RunPostViewModel(
    private val userRepository: UserRepository,
    private val runPostRepository: RunPostRepository,
    private val firebaseAuth: FirebaseAuth,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _runPost = MutableStateFlow<RunPost?>(null)
    val runPost: StateFlow<RunPost?> = _runPost.asStateFlow()

    private val _postUser = MutableStateFlow<User?>(null)
    val postUser: StateFlow<User?> = _postUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _userLikedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val userLikedPostIds: StateFlow<Set<String>> = _userLikedPostIds.asStateFlow()

    private val _likedUsers = MutableStateFlow<List<User>>(emptyList())
    val likedUsers: StateFlow<List<User>> = _likedUsers.asStateFlow()

    private val TAG = "RunPostViewModel"

    init {
        savedStateHandle.get<String>("postId")?.let { postId ->
            fetchRunPostAndRelatedData(postId)
        }
    }

    fun fetchRunPostAndRelatedData(postId: String) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                // Fetch the run post - changed from getPostById to getRunPost
                val postResult = runPostRepository.getRunPost(postId)
                if (postResult.isSuccess) {
                    _runPost.value = postResult.getOrNull()

                    // ... rest of the method remains the same ...
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
                Log.e(TAG, "Error fetching post data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleLike(postId: String, isCurrentlyLiked: Boolean) {
        val currentUserId = firebaseAuth.currentUser?.uid ?: run {
            _errorMessage.value = "You must be logged in to like posts"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (isCurrentlyLiked) {
                    runPostRepository.unlikePost(postId, currentUserId)
                } else {
                    runPostRepository.likePost(postId, currentUserId)
                }
                // Refresh the data
                fetchRunPostAndRelatedData(postId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to toggle like"
                Log.e(TAG, "Error toggling like", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                val firebaseAuth = FirebaseAuth.getInstance()
                val firestore = FirebaseFirestore.getInstance()
                RunPostViewModel(
                    userRepository = UserRepository(firestore),
                    runPostRepository = RunPostRepository(firestore),
                    firebaseAuth = firebaseAuth,
                    savedStateHandle = savedStateHandle
                )
            }
        }
    }
}