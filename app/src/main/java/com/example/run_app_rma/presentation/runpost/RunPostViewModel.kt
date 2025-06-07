package com.example.run_app_rma.presentation.runpost

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.run_app_rma.data.firestore.model.Comment
import com.example.run_app_rma.data.firestore.model.RunPost
import com.example.run_app_rma.data.firestore.model.User
import com.example.run_app_rma.data.firestore.repository.RunPostRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions // Import FirebaseFunctions
import com.google.firebase.functions.functions // Import the extension function
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await // Import await
import kotlinx.coroutines.launch
import java.util.Date

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

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingAction = MutableStateFlow(false)
    val isLoadingAction: StateFlow<Boolean> = _isLoadingAction.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _userLikedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val userLikedPostIds: StateFlow<Set<String>> = _userLikedPostIds.asStateFlow()

    private val _likedUsers = MutableStateFlow<List<User>>(emptyList())
    val likedUsers: StateFlow<List<User>> = _likedUsers.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _commentUsers = MutableStateFlow<Map<String, User>>(emptyMap())
    val commentUsers: StateFlow<Map<String, User>> = _commentUsers.asStateFlow()

    private val _commentInput = MutableStateFlow("")
    val commentInput: StateFlow<String> = _commentInput.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    // Initialize Firebase Functions instance
    private val functions = FirebaseFunctions.getInstance()


    private val TAG = "RunPostViewModel"

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _currentUserId.value = auth.currentUser?.uid
            Log.d(TAG, "Auth state changed. Current User ID: ${_currentUserId.value}")
            savedStateHandle.get<String>("postId")?.let { postId ->
                if (_currentUserId.value != null && _runPost.value != null) {
                    fetchUpdatedLikeAndCommentData(postId)
                }
            }
        }

        savedStateHandle.get<String>("postId")?.let { postId ->
            Log.d(TAG, "Initializing with postId: $postId")
            fetchRunPostAndRelatedData(postId)
        }
    }

    fun fetchRunPostAndRelatedData(postId: String) {
        if (!_isInitialLoading.value) {
            _isRefreshing.value = true
        }

        _errorMessage.value = null
        Log.d(TAG, "Fetching data for post ID: $postId")

        viewModelScope.launch {
            try {
                val postResult = runPostRepository.getRunPost(postId)
                if (postResult.isSuccess) {
                    val fetchedPost = postResult.getOrNull()
                    _runPost.value = fetchedPost
                    Log.d(TAG, "Fetched RunPost: $fetchedPost")

                    fetchedPost?.let { post ->
                        val userResult = userRepository.getUserProfile(post.userId)
                        _postUser.value = userResult.getOrNull()
                        Log.d(TAG, "Fetched Post User: ${_postUser.value?.displayName ?: "N/A"}")

                        val likesResult = runPostRepository.getLikesForPost(postId)
                        if (likesResult.isSuccess) {
                            val likedUserIds = likesResult.getOrNull()?.map { it.userId } ?: emptyList()
                            Log.d(TAG, "Liked User IDs: $likedUserIds")

                            val usersWhoLikedResult = runPostRepository.getUsersByIds(likedUserIds)
                            if (usersWhoLikedResult.isSuccess) {
                                _likedUsers.value = usersWhoLikedResult.getOrNull() ?: emptyList()
                                Log.d(TAG, "Fetched Liked Users: ${_likedUsers.value.map { it.displayName }}")
                            } else {
                                _errorMessage.value = usersWhoLikedResult.exceptionOrNull()?.message ?: "Failed to load users who liked."
                                Log.e(TAG, "Error fetching liked users: ${_errorMessage.value}")
                            }

                            val currentUserId = firebaseAuth.currentUser?.uid
                            if (currentUserId != null) {
                                _userLikedPostIds.value = if (likedUserIds.contains(currentUserId)) setOf(postId) else emptySet()
                                Log.d(TAG, "Current user ($currentUserId) liked status (after initial fetch): ${_userLikedPostIds.value.contains(postId)}")
                            }
                        } else {
                            _errorMessage.value = likesResult.exceptionOrNull()?.message ?: "Failed to load likes."
                            Log.e(TAG, "Error fetching likes: ${_errorMessage.value}")
                        }

                        val commentsResult = runPostRepository.getCommentsForPost(postId)
                        if (commentsResult.isSuccess) {
                            val fetchedComments = commentsResult.getOrNull() ?: emptyList()
                            _comments.value = fetchedComments
                            Log.d(TAG, "Fetched Comments (${fetchedComments.size}): $fetchedComments")

                            val commentUserIds = fetchedComments.map { it.userId }.distinct()
                            Log.d(TAG, "Comment User IDs: $commentUserIds")

                            val usersWhoCommentedResult = runPostRepository.getUsersByIds(commentUserIds)
                            if (usersWhoCommentedResult.isSuccess) {
                                _commentUsers.value = usersWhoCommentedResult.getOrNull()?.associateBy { it.id } ?: emptyMap()
                                Log.d(TAG, "Fetched Comment Users: ${_commentUsers.value.map { it.value.displayName }}")
                            } else {
                                _errorMessage.value = usersWhoCommentedResult.exceptionOrNull()?.message ?: "Failed to load comment users."
                                Log.e(TAG, "Error fetching comment users: ${_errorMessage.value}")
                            }
                        } else {
                            _errorMessage.value = commentsResult.exceptionOrNull()?.message ?: "Failed to load comments."
                            Log.e(TAG, "Error fetching comments: ${_errorMessage.value}")
                        }

                    }
                } else {
                    _errorMessage.value = postResult.exceptionOrNull()?.message ?: "Run post not found."
                    Log.e(TAG, "Error fetching run post: ${_errorMessage.value}")
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
                Log.e(TAG, "Unexpected error in fetchRunPostAndRelatedData", e)
            } finally {
                _isInitialLoading.value = false
                _isRefreshing.value = false
                Log.d(TAG, "Finished fetching data. isInitialLoading: ${_isInitialLoading.value}, isRefreshing: ${_isRefreshing.value}")
            }
        }
    }

    fun toggleLike(postId: String, isCurrentlyLiked: Boolean) {
        val currentUserId = firebaseAuth.currentUser?.uid ?: run {
            _errorMessage.value = "You must be logged in to like posts"
            Log.w(TAG, "User not logged in, cannot toggle like.")
            return
        }
        Log.d(TAG, "toggleLike: BEFORE optimistic update. userLikedPostIds.value: ${_userLikedPostIds.value}, isCurrentlyLiked (from UI): $isCurrentlyLiked")

        _isLoadingAction.value = true
        _errorMessage.value = null

        val previousLikedState = _userLikedPostIds.value
        _userLikedPostIds.value = if (isCurrentlyLiked) {
            _userLikedPostIds.value.minus(postId)
        } else {
            _userLikedPostIds.value.plus(postId)
        }
        Log.d(TAG, "toggleLike: AFTER optimistic update. userLikedPostIds.value: ${_userLikedPostIds.value}. UI icon should now reflect this.")

        viewModelScope.launch {
            try {
                val result = if (isCurrentlyLiked) {
                    runPostRepository.unlikePost(postId, currentUserId)
                } else {
                    runPostRepository.likePost(postId, currentUserId)
                }

                result.onSuccess {
                    Log.d(TAG, "Like/Unlike successful for post $postId in DB. Re-fetching data for confirmation.")
                    fetchUpdatedLikeAndCommentData(postId)
                }.onFailure { e ->
                    _userLikedPostIds.value = previousLikedState
                    _errorMessage.value = e.message ?: "Greška pri lajkanju/otlajkavanju objave."
                    Log.e(TAG, "Error (ViewModel) liking/unliking post $postId: ${e.message}", e)
                }
            } catch (e: Exception) {
                _userLikedPostIds.value = previousLikedState
                _errorMessage.value = e.message ?: "Došlo je do neočekivane greške pri lajkanju."
                Log.e(TAG, "Unexpected error (ViewModel) in toggleLike", e)
            } finally {
                _isLoadingAction.value = false
            }
        }
    }

    fun onCommentInputChanged(newInput: String) {
        _commentInput.value = newInput
        Log.d(TAG, "Comment input changed: $newInput")
    }

    fun addComment() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: run {
            _errorMessage.value = "You must be logged in to comment."
            Log.w(TAG, "User not logged in, cannot add comment.")
            return
        }
        val postId = _runPost.value?.id ?: run {
            _errorMessage.value = "Cannot add comment, post ID is missing."
            Log.w(TAG, "Post ID missing, cannot add comment.")
            return
        }
        val commentText = _commentInput.value.trim()
        if (commentText.isBlank()) {
            _errorMessage.value = "Comment cannot be empty."
            Log.w(TAG, "Comment text is blank.")
            return
        }

        Log.d(TAG, "Attempting to add comment for post $postId by user $currentUserId: $commentText")

        _isLoadingAction.value = true
        viewModelScope.launch {
            try {
                val newComment = Comment(
                    postId = postId,
                    userId = currentUserId,
                    text = commentText,
                    timestamp = Date()
                )
                runPostRepository.addComment(newComment).onSuccess {
                    _commentInput.value = ""
                    Log.d(TAG, "Comment added successfully. Comment input cleared.")
                    fetchUpdatedLikeAndCommentData(postId)
                }.onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to add comment."
                    Log.e(TAG, "Error adding comment: ${e.message}", e)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred while adding comment."
                Log.e(TAG, "Unexpected error in addComment", e)
            } finally {
                _isLoadingAction.value = false
            }
        }
    }

    // UPDATED: Now calls a Callable Cloud Function
    fun deleteComment(commentId: String) {
        val currentUserId = firebaseAuth.currentUser?.uid ?: run {
            _errorMessage.value = "You must be logged in to delete comments."
            Log.w(TAG, "User not logged in, cannot delete comment.")
            return
        }
        val postId = _runPost.value?.id ?: run {
            _errorMessage.value = "Cannot delete comment, post ID is missing."
            Log.w(TAG, "Post ID missing, cannot delete comment.")
            return
        }

        Log.d(TAG, "Attempting to call deleteCommentCallable for comment $commentId for post $postId by user $currentUserId")

        _isLoadingAction.value = true
        _errorMessage.value = null // Clear any existing error messages

        viewModelScope.launch {
            try {
                val data = hashMapOf(
                    "commentId" to commentId,
                    "postId" to postId
                )
                // Call the Callable Cloud Function
                val result = functions
                    .getHttpsCallable("deleteCommentCallable")
                    .call(data)
                    .await()

                // Callable functions return data in 'result.data'
                val response = result.data as? Map<String, Any>
                val success = response?.get("success") as? Boolean ?: false
                val notificationSent = response?.get("notificationSent") as? Boolean
                val reason = response?.get("reason") as? String
                val errorDetails = response?.get("error") // Can be an object if passed

                if (success) {
                    Log.d(TAG, "Comment deletion callable returned success. Notification sent: $notificationSent. Reason: $reason")
                    fetchUpdatedLikeAndCommentData(postId) // Refresh UI after successful deletion
                } else {
                    val msg = "Failed to delete comment via callable: ${reason ?: "Unknown reason"}"
                    _errorMessage.value = msg
                    Log.e(TAG, msg)
                    if (errorDetails != null) {
                        Log.e(TAG, "Callable error details: $errorDetails")
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error calling delete comment function: ${e.message}"
                Log.e(TAG, "Error calling deleteCommentCallable", e)
            } finally {
                _isLoadingAction.value = false
                Log.d(TAG, "deleteComment operation finished via callable. isLoadingAction: ${_isLoadingAction.value}")
            }
        }
    }

    fun deletePost(postId: String, onSuccessAction: () -> Unit) {
        val currentUserId = firebaseAuth.currentUser?.uid ?: run {
            _errorMessage.value = "You must be logged in to delete posts."
            Log.w(TAG, "User not logged in, cannot delete post.")
            return
        }
        val postToDelete = _runPost.value
        if (postToDelete == null || postToDelete.id != postId) {
            _errorMessage.value = "Post not found or ID mismatch."
            Log.w(TAG, "Post not found or ID mismatch for deletion: $postId")
            return
        }
        if (currentUserId != postToDelete.userId) {
            _errorMessage.value = "You don't have permission to delete this post."
            Log.w(TAG, "User $currentUserId attempted to delete post $postId without permission (not owner).")
            return
        }

        Log.d(TAG, "Attempting to delete post $postId by user $currentUserId")
        _isLoadingAction.value = true
        viewModelScope.launch {
            try {
                runPostRepository.deleteRunPost(postId).onSuccess {
                    Log.d(TAG, "Post $postId deleted successfully from RunPostRepository.")
                    _runPost.value = null
                    _errorMessage.value = "Objava je uspješno obrisana."
                    onSuccessAction()
                }.onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to delete post."
                    Log.e(TAG, "Error deleting post $postId: ${e.message}", e)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred while deleting post."
                Log.e(TAG, "Unexpected error in deletePost", e)
            } finally {
                _isLoadingAction.value = false
            }
        }
    }

    private fun fetchUpdatedLikeAndCommentData(postId: String) {
        viewModelScope.launch {
            runPostRepository.getRunPost(postId).onSuccess { updatedPost ->
                _runPost.value = updatedPost
                Log.d(TAG, "Updated RunPost after action: $updatedPost")
            }.onFailure { e ->
                _errorMessage.value = e.message ?: "Failed to refresh post data."
                Log.e(TAG, "Error refreshing post data after action: ${e.message}", e)
            }

            runPostRepository.getLikesForPost(postId).onSuccess { likes ->
                val likedUserIds = likes.map { it.userId }
                Log.d(TAG, "fetchUpdatedLikeAndCommentData: Liked user IDs from DB for post $postId: $likedUserIds")

                val currentUserId = firebaseAuth.currentUser?.uid
                if (currentUserId != null) {
                    val newLikedState = likedUserIds.contains(currentUserId)
                    Log.d(TAG, "fetchUpdatedLikeAndCommentData: Current user ($currentUserId) like status from DB: $newLikedState")
                    _userLikedPostIds.value = if (newLikedState) setOf(postId) else emptySet()
                    Log.d(TAG, "fetchUpdatedLikeAndCommentData: _userLikedPostIds.value after DB sync: ${_userLikedPostIds.value}")
                } else {
                    _userLikedPostIds.value = emptySet()
                    Log.d(TAG, "fetchUpdatedLikeAndCommentData: Current user not logged in, _userLikedPostIds set to empty.")
                }

                runPostRepository.getUsersByIds(likedUserIds).onSuccess { users ->
                    _likedUsers.value = users
                }.onFailure { e ->
                    Log.e(TAG, "Error updating liked users after action: ${e.message}")
                }
            }.onFailure { e ->
                Log.e(TAG, "Error refreshing likes after action: ${e.message}")
            }

            runPostRepository.getCommentsForPost(postId).onSuccess { comments ->
                _comments.value = comments
                val commentUserIds = comments.map { it.userId }.distinct()
                runPostRepository.getUsersByIds(commentUserIds).onSuccess { users ->
                    _commentUsers.value = users.associateBy { it.id }
                }.onFailure { e ->
                    Log.e(TAG, "Error updating comment users after action: ${e.message}")
                }
            }.onFailure { e ->
                Log.e(TAG, "Error refreshing comments after action: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        firebaseAuth.removeAuthStateListener {  }
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
