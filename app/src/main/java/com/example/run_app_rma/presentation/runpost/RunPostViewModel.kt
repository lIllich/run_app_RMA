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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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

    private val TAG = "RunPostViewModel"

    init {
        savedStateHandle.get<String>("postId")?.let { postId ->
            Log.d(TAG, "Initializing with postId: $postId")
            fetchRunPostAndRelatedData(postId)
        }
    }

    fun fetchRunPostAndRelatedData(postId: String) {
        _isLoading.value = true
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
                                _userLikedPostIds.value = if (likedUserIds.contains(currentUserId)) setOf(postId) else emptySet()
                                Log.d(TAG, "Current user ($currentUserId) liked status: ${_userLikedPostIds.value.contains(postId)}")
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
                _isLoading.value = false
                Log.d(TAG, "Finished fetching data. IsLoading: $_isLoading.value")
            }
        }
    }

    fun toggleLike(postId: String, isCurrentlyLiked: Boolean) {
        val currentUserId = firebaseAuth.currentUser?.uid ?: run {
            _errorMessage.value = "You must be logged in to like posts"
            Log.w(TAG, "User not logged in, cannot toggle like.")
            return
        }
        Log.d(TAG, "Toggling like for post $postId by user $currentUserId. Currently liked: $isCurrentlyLiked")

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = if (isCurrentlyLiked) {
                    runPostRepository.unlikePost(postId, currentUserId)
                } else {
                    runPostRepository.likePost(postId, currentUserId)
                }

                result.onSuccess {
                    Log.d(TAG, "Like/Unlike successful. Refreshing data.")
                    fetchRunPostAndRelatedData(postId) // Refresh the data to update counts and liked status
                }.onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to toggle like"
                    Log.e(TAG, "Error toggling like for post $postId: ${e.message}", e)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to toggle like"
                Log.e(TAG, "Unexpected error in toggleLike", e)
            } finally {
                // _isLoading is set to false inside fetchRunPostAndRelatedData,
                // but if an error occurs outside of the result handling, we ensure it's false.
                if (_isLoading.value) _isLoading.value = false
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

        viewModelScope.launch {
            _isLoading.value = true
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
                    fetchRunPostAndRelatedData(postId) // Refresh comments and post data
                }.onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to add comment."
                    Log.e(TAG, "Error adding comment: ${e.message}", e)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred while adding comment."
                Log.e(TAG, "Unexpected error in addComment", e)
            } finally {
                // _isLoading is set to false inside fetchRunPostAndRelatedData,
                // but if an error occurs outside of the result handling, we ensure it's false.
                if (_isLoading.value) _isLoading.value = false
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
