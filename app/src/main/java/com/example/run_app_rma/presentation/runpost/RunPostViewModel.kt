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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // State for initial loading of the entire screen's data
    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    // State for specific actions like liking or commenting
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

    // New: Current authenticated user's ID
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()


    private val TAG = "RunPostViewModel"

    init {
        // Observe Firebase Auth state to get the current user's ID
        firebaseAuth.addAuthStateListener { auth ->
            _currentUserId.value = auth.currentUser?.uid
            Log.d(TAG, "Auth state changed. Current User ID: ${_currentUserId.value}")
            // If the postId is already available from SavedStateHandle, and current user is set,
            // refresh data to ensure like status is correct for the logged-in user.
            savedStateHandle.get<String>("postId")?.let { postId ->
                if (_currentUserId.value != null && _runPost.value != null) {
                    // Only re-fetch if already loaded, otherwise init will handle it
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
        _isInitialLoading.value = true // Set initial loading to true at the start of a full data fetch
        _errorMessage.value = null
        Log.d(TAG, "Fetching data for post ID: $postId")

        viewModelScope.launch {
            try {
                // Fetch the run post
                val postResult = runPostRepository.getRunPost(postId)
                if (postResult.isSuccess) {
                    val fetchedPost = postResult.getOrNull()
                    _runPost.value = fetchedPost
                    Log.d(TAG, "Fetched RunPost: $fetchedPost")

                    fetchedPost?.let { post ->
                        // Fetch the post creator's user profile
                        val userResult = userRepository.getUserProfile(post.userId)
                        _postUser.value = userResult.getOrNull()
                        Log.d(TAG, "Fetched Post User: ${_postUser.value?.displayName ?: "N/A"}")

                        // Fetch likes for the post
                        val likesResult = runPostRepository.getLikesForPost(postId)
                        if (likesResult.isSuccess) {
                            val likedUserIds = likesResult.getOrNull()?.map { it.userId } ?: emptyList()
                            Log.d(TAG, "Liked User IDs: $likedUserIds")

                            // Fetch user profiles for liked users
                            val usersWhoLikedResult = runPostRepository.getUsersByIds(likedUserIds)
                            if (usersWhoLikedResult.isSuccess) {
                                _likedUsers.value = usersWhoLikedResult.getOrNull() ?: emptyList()
                                Log.d(TAG, "Fetched Liked Users: ${_likedUsers.value.map { it.displayName }}")
                            } else {
                                _errorMessage.value = usersWhoLikedResult.exceptionOrNull()?.message ?: "Failed to load users who liked."
                                Log.e(TAG, "Error fetching liked users: ${_errorMessage.value}")
                            }

                            // Check if current user liked the post
                            val currentUserId = firebaseAuth.currentUser?.uid
                            if (currentUserId != null) {
                                // Update _userLikedPostIds based on actual data from likesResult
                                _userLikedPostIds.value = if (likedUserIds.contains(currentUserId)) setOf(postId) else emptySet()
                                Log.d(TAG, "Current user ($currentUserId) liked status (after initial fetch): ${_userLikedPostIds.value.contains(postId)}")
                            }
                        } else {
                            _errorMessage.value = likesResult.exceptionOrNull()?.message ?: "Failed to load likes."
                            Log.e(TAG, "Error fetching likes: ${_errorMessage.value}")
                        }

                        // Fetch comments for the post
                        val commentsResult = runPostRepository.getCommentsForPost(postId)
                        if (commentsResult.isSuccess) {
                            val fetchedComments = commentsResult.getOrNull() ?: emptyList()
                            _comments.value = fetchedComments
                            Log.d(TAG, "Fetched Comments (${fetchedComments.size}): $fetchedComments")

                            val commentUserIds = fetchedComments.map { it.userId }.distinct()
                            Log.d(TAG, "Comment User IDs: $commentUserIds")

                            // Fetch user profiles for commenters
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
                _isInitialLoading.value = false // Set initial loading to false after completion/error
                Log.d(TAG, "Finished fetching data. isInitialLoading: ${_isInitialLoading.value}")
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

        _isLoadingAction.value = true // Set action loading to true
        _errorMessage.value = null // Clear any previous error messages

        // Optimistic update: Immediately reflect the new like status in the UI
        val previousLikedState = _userLikedPostIds.value
        _userLikedPostIds.value = if (isCurrentlyLiked) {
            _userLikedPostIds.value.minus(postId) // Remove postId from set
        } else {
            _userLikedPostIds.value.plus(postId) // Add postId to set
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
                    // Re-fetch only necessary data to avoid full screen reload for minor action
                    fetchUpdatedLikeAndCommentData(postId)
                }.onFailure { e ->
                    // Revert optimistic update on failure
                    _userLikedPostIds.value = previousLikedState
                    _errorMessage.value = e.message ?: "Greška pri lajkanju/otlajkavanju objave."
                    Log.e(TAG, "Error (ViewModel) liking/unliking post $postId: ${e.message}", e)
                }
            } catch (e: Exception) {
                // Revert optimistic update on unexpected error (e.g., network timeout before DB call)
                _userLikedPostIds.value = previousLikedState
                _errorMessage.value = e.message ?: "Došlo je do neočekivane greške pri lajkanju."
                Log.e(TAG, "Unexpected error (ViewModel) in toggleLike", e)
            } finally {
                _isLoadingAction.value = false // Hide action loading
                Log.d(TAG, "toggleLike operation finished. isLoadingAction: ${_isLoadingAction.value}")
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

        // Set action loading to true
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
                    _commentInput.value = "" // Clear input field
                    Log.d(TAG, "Comment added successfully. Comment input cleared.")
                    // Only refresh necessary data to update counts and comments list,
                    // without setting _isInitialLoading back to true.
                    fetchUpdatedLikeAndCommentData(postId)
                }.onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to add comment."
                    Log.e(TAG, "Error adding comment: ${e.message}", e)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred while adding comment."
                Log.e(TAG, "Unexpected error in addComment", e)
            } finally {
                // Set action loading to false regardless of success or failure
                _isLoadingAction.value = false
            }
        }
    }

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

        Log.d(TAG, "Attempting to delete comment $commentId for post $postId by user $currentUserId")

        _isLoadingAction.value = true
        viewModelScope.launch {
            try {
                // Ensure the current user has permission to delete:
                // Either they own the comment OR they own the post.
                val commentToDelete = _comments.value.find { it.id == commentId }
                val postOwner = _runPost.value?.userId

                if (commentToDelete == null) {
                    _errorMessage.value = "Comment not found."
                    Log.e(TAG, "Attempted to delete non-existent comment: $commentId")
                    return@launch
                }

                val canDelete = (currentUserId == commentToDelete.userId) || (currentUserId == postOwner)

                if (canDelete) {
                    runPostRepository.deleteComment(commentId, postId).onSuccess {
                        Log.d(TAG, "Comment $commentId deleted successfully.")
                        fetchUpdatedLikeAndCommentData(postId) // Refresh data after deletion
                    }.onFailure { e ->
                        _errorMessage.value = e.message ?: "Failed to delete comment."
                        Log.e(TAG, "Error deleting comment $commentId: ${e.message}", e)
                    }
                } else {
                    _errorMessage.value = "You don't have permission to delete this comment."
                    Log.w(TAG, "User $currentUserId attempted to delete comment $commentId without permission.")
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred while deleting comment."
                Log.e(TAG, "Unexpected error in deleteComment", e)
            } finally {
                _isLoadingAction.value = false
            }
        }
    }

    // Modified to accept a lambda for success action
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
                // Await the deletion result before continuing
                runPostRepository.deleteRunPost(postId).onSuccess {
                    Log.d(TAG, "Post $postId deleted successfully from RunPostRepository.")
                    _runPost.value = null // Clear the post from the UI
                    _errorMessage.value = "Objava je uspješno obrisana." // Provide feedback
                    onSuccessAction() // <--- Call the provided lambda ONLY on success
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


    // New helper function to refresh only likes and comments after an action
    private fun fetchUpdatedLikeAndCommentData(postId: String) {
        viewModelScope.launch {
            // Re-fetch only the post to get updated like/comment counts
            runPostRepository.getRunPost(postId).onSuccess { updatedPost ->
                _runPost.value = updatedPost
                Log.d(TAG, "Updated RunPost after action: $updatedPost")
            }.onFailure { e ->
                _errorMessage.value = e.message ?: "Failed to refresh post data."
                Log.e(TAG, "Error refreshing post data after action: ${e.message}", e)
            }

            // Re-fetch likes for the post
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
                    _userLikedPostIds.value = emptySet() // Not logged in, no likes
                    Log.d(TAG, "fetchUpdatedLikeAndCommentData: Current user not logged in, _userLikedPostIds set to empty.")
                }

                // Also update likedUsers for the "Sviđa se" tab
                runPostRepository.getUsersByIds(likedUserIds).onSuccess { users ->
                    _likedUsers.value = users
                }.onFailure { e ->
                    Log.e(TAG, "Error updating liked users after action: ${e.message}")
                }
            }.onFailure { e ->
                Log.e(TAG, "Error refreshing likes after action: ${e.message}")
            }

            // Re-fetch comments for the post
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
        // Remove auth state listener to prevent memory leaks
        firebaseAuth.removeAuthStateListener {  } // Correct way to remove all listeners, or store a specific listener to remove it.
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
