package com.example.run_app_rma.data.firestore.repository

import android.util.Log // Import Log for debugging
import com.example.run_app_rma.data.firestore.model.Comment
import com.example.run_app_rma.data.firestore.model.Like
import com.example.run_app_rma.data.firestore.model.RunPost
import com.example.run_app_rma.data.firestore.model.User // Import User model
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class RunPostRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val postsCollection = firestore.collection("posts")
    private val likesCollection = firestore.collection("likes")
    private val commentsCollection = firestore.collection("comments")
    private val usersCollection = firestore.collection("users") // Reference to users collection

    private val TAG = "RunPostRepository"

    suspend fun createRunPost(runPost: RunPost): Result<String> {
        return try {
            val docRef = postsCollection.add(runPost).await()
            Log.d(TAG, "Created RunPost with ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating RunPost", e)
            Result.failure(e)
        }
    }

    suspend fun getRunPost(postId: String): Result<RunPost> {
        return try {
            Log.d(TAG, "Fetching RunPost with ID: $postId")
            val document = postsCollection.document(postId).get().await()
            val post = document.toObject(RunPost::class.java)

            if(post != null) {
                Log.d(TAG, "Successfully fetched RunPost: ${post.id}")
                Result.success(post)
            } else {
                Log.w(TAG, "Run post not found for ID: $postId")
                Result.failure(NoSuchElementException("Run post not found for ID: $postId"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching RunPost with ID: $postId", e)
            Result.failure(e)
        }
    }

    suspend fun getRunPostForUser(userId: String): Result<List<RunPost>> {
        return try {
            Log.d(TAG, "Fetching RunPosts for User ID: $userId")
            val posts = postsCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(RunPost::class.java)
            Log.d(TAG, "Fetched ${posts.size} RunPosts for user $userId")
            Result.success(posts)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching RunPosts for user $userId", e)
            Result.failure(e)
        }
    }

    suspend fun getRunPostsByUsers(userIds: List<String>): Result<List<RunPost>> {
        if (userIds.isEmpty()) {
            Log.d(TAG, "getRunPostsByUsers called with empty userIds list.")
            return Result.success(emptyList())
        }
        return try {
            Log.d(TAG, "Fetching RunPosts for user IDs: $userIds")
            val posts = postsCollection
                .whereIn("userId", userIds)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(RunPost::class.java)
            Log.d(TAG, "Fetched ${posts.size} RunPosts for given user IDs.")
            Result.success(posts)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching RunPosts by users", e)
            Result.failure(e)
        }
    }

    // New: Get all likes for a specific post
    suspend fun getLikesForPost(postId: String): Result<List<Like>> {
        return try {
            Log.d(TAG, "Fetching Likes for Post ID: $postId")
            val likes = likesCollection
                .whereEqualTo("postId", postId)
                .get()
                .await()
                .toObjects(Like::class.java)
            Log.d(TAG, "Fetched ${likes.size} Likes for post $postId")
            Result.success(likes)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching likes for post $postId", e)
            Result.failure(e)
        }
    }

    suspend fun getLikesByUser(userId: String): Result<List<Like>> {
        return try {
            Log.d(TAG, "Fetching Likes by User ID: $userId")
            val likes = likesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(Like::class.java)
            Log.d(TAG, "Fetched ${likes.size} Likes by user $userId")
            Result.success(likes)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching likes by user $userId", e)
            Result.failure(e)
        }
    }

    suspend fun getCommentsByUser(userId: String): Result<List<Comment>> {
        return try {
            Log.d(TAG, "Fetching Comments by User ID: $userId")
            val comments = commentsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(Comment::class.java)
            Log.d(TAG, "Fetched ${comments.size} Comments by user $userId")
            Result.success(comments)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching comments by user $userId", e)
            Result.failure(e)
        }
    }

    suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Liking post $postId by user $userId")
            likesCollection.add(Like(postId = postId, userId = userId)).await()
            postsCollection.document(postId).update("likesCount", FieldValue.increment(1)).await()
            Log.d(TAG, "Post $postId liked successfully by user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error liking post $postId by user $userId", e)
            Result.failure(e)
        }
    }

    suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Unliking post $postId by user $userId")
            likesCollection
                .whereEqualTo("postId", postId)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .forEach { it.reference.delete().await() }

            postsCollection.document(postId).update("likesCount", FieldValue.increment(-1)).await()
            Log.d(TAG, "Post $postId unliked successfully by user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error unliking post $postId by user $userId", e)
            Result.failure(e)
        }
    }

    suspend fun hasUserLikedPost(postId: String, userId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Checking if user $userId liked post $postId")
            val snapshot = likesCollection
                .whereEqualTo("postId", postId)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()
            val hasLiked = !snapshot.isEmpty
            Log.d(TAG, "User $userId has liked post $postId: $hasLiked")
            Result.success(hasLiked)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if user $userId liked post $postId", e)
            Result.failure(e)
        }
    }

    suspend fun addComment(comment: Comment): Result<Unit> {
        return try {
            Log.d(TAG, "Adding comment to post ${comment.postId} by user ${comment.userId}")
            commentsCollection.add(comment).await()
            postsCollection.document(comment.postId).update("commentsCount", FieldValue.increment(1)).await()
            Log.d(TAG, "Comment added and commentsCount incremented for post ${comment.postId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding comment to post ${comment.postId}", e)
            Result.failure(e)
        }
    }

    suspend fun getCommentsForPost(postId: String): Result<List<Comment>> {
        return try {
            Log.d(TAG, "Fetching comments for post ID: $postId")
            val comments = commentsCollection
                .whereEqualTo("postId", postId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(Comment::class.java)
            Log.d(TAG, "Fetched ${comments.size} comments for post $postId")
            Result.success(comments)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching comments for post $postId", e)
            Result.failure(e)
        }
    }

    // New: Get multiple users by a list of user IDs
    suspend fun getUsersByIds(userIds: List<String>): Result<List<User>> {
        if (userIds.isEmpty()) {
            Log.d(TAG, "getUsersByIds called with empty userIds list, returning empty list.")
            return Result.success(emptyList())
        }
        // Firestore 'whereIn' clause has a limit of 10 values. If userIds is larger,
        // this query will fail or return incomplete results. You might need to batch these queries
        // if you expect more than 10 user IDs at a time.
        Log.d(TAG, "Fetching users with IDs: $userIds (Max 10 IDs for whereIn query)")
        return try {
            val snapshot = usersCollection
                .whereIn("id", userIds) // This 'id' refers to a field *within* the user document
                .get()
                .await()

            Log.d(TAG, "getUsersByIds - Raw snapshot size: ${snapshot.documents.size}")
            if (snapshot.documents.isEmpty() && userIds.isNotEmpty()) {
                Log.e(TAG, "getUsersByIds - No documents found for provided userIds. Possible data mismatch or rules issue.")
            }

            val users = snapshot.toObjects(User::class.java)
            Log.d(TAG, "Fetched ${users.size} users for given IDs: ${users.map { it.displayName }}")
            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching users by IDs: ${userIds.joinToString()}", e)
            Result.failure(e)
        }
    }
}
