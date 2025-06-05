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
import java.util.Date

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
            val document = postsCollection.document(postId).get().await()
            val post = document.toObject(RunPost::class.java)
            if (post != null) {
                Log.d(TAG, "Fetched RunPost: $post")
                Result.success(post)
            } else {
                Log.d(TAG, "RunPost with ID $postId not found.")
                Result.failure(NoSuchElementException("Run post not found."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching RunPost with ID $postId", e)
            Result.failure(e)
        }
    }

    // New: Get all run posts by a list of user IDs
    suspend fun getRunPostsByUsers(userIds: List<String>): Result<List<RunPost>> {
        if (userIds.isEmpty()) {
            Log.d(TAG, "getRunPostsByUsers called with empty userIds list, returning empty list.")
            return Result.success(emptyList())
        }
        return try {
            // Firestore 'whereIn' clause has a limit of 10 values.
            // If userIds is larger, this query will fail.
            // For more than 10, batch queries are needed or redesign.
            // Assuming userIds.size <= 10 for simplicity for now.
            val posts = postsCollection
                .whereIn("userId", userIds)
                .get()
                .await()
                .toObjects(RunPost::class.java)
            Log.d(TAG, "Fetched ${posts.size} posts for user IDs: $userIds")
            Result.success(posts)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching posts for user IDs $userIds", e)
            Result.failure(e)
        }
    }

    suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return try {
            // Check if the user has already liked this post
            val existingLikeQuery = likesCollection
                .whereEqualTo("postId", postId)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()

            if (existingLikeQuery.isEmpty) {
                // If no existing like, add a new one and increment count
                val like = Like(postId = postId, userId = userId, timestamp = Date())
                likesCollection.add(like).await()
                postsCollection.document(postId).update("likesCount", FieldValue.increment(1)).await()
                Log.d(TAG, "User $userId liked post $postId. Like count incremented in DB.")
                Result.success(Unit)
            } else {
                Log.d(TAG, "User $userId already liked post $postId. No action taken in DB.")
                Result.success(Unit) // Already liked, no error
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error liking post $postId by user $userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        return try {
            val querySnapshot = likesCollection
                .whereEqualTo("postId", postId)
                .whereEqualTo("userId", userId)
                .limit(1) // Limit to 1, as there should only be one like per user per post
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                // If a like document exists, delete it and decrement count
                val document = querySnapshot.documents.first() // Get the first (and only) matching document
                document.reference.delete().await()
                postsCollection.document(postId).update("likesCount", FieldValue.increment(-1)).await()
                Log.d(TAG, "User $userId unliked post $postId. Like count decremented in DB.")
            } else {
                Log.d(TAG, "User $userId had not liked post $postId. No action taken in DB (no like document found).")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error unliking post $postId by user $userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getLikesForPost(postId: String): Result<List<Like>> {
        return try {
            val likes = likesCollection
                .whereEqualTo("postId", postId)
                .get()
                .await()
                .toObjects(Like::class.java)
            Log.d(TAG, "Fetched ${likes.size} likes for post $postId")
            Result.success(likes)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching likes for post $postId", e)
            Result.failure(e)
        }
    }

    suspend fun getLikesByUser(userId: String): Result<List<Like>> {
        return try {
            val likes = likesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(Like::class.java)
            Log.d(TAG, "Fetched ${likes.size} likes for user $userId")
            Result.success(likes)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching likes for user $userId", e)
            Result.failure(e)
        }
    }

    suspend fun addComment(comment: Comment): Result<Unit> {
        return try {
            commentsCollection.add(comment).await()
            postsCollection.document(comment.postId).update("commentsCount", FieldValue.increment(1)).await()
            Log.d(TAG, "Added comment for post ${comment.postId}. Comment count incremented.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding comment", e)
            Result.failure(e)
        }
    }

    // This method is now properly defined
    suspend fun deleteComment(commentId: String, postId: String): Result<Unit> {
        return try {
            val commentDocRef = commentsCollection.document(commentId)
            commentDocRef.delete().await()
            postsCollection.document(postId).update("commentsCount", FieldValue.increment(-1)).await()
            Log.d(TAG, "Comment $commentId deleted for post $postId. Comment count decremented.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting comment $commentId for post $postId", e)
            Result.failure(e)
        }
    }

    suspend fun getCommentsForPost(postId: String): Result<List<Comment>> {
        return try {
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
            Log.e(TAG, "Error fetching users by IDs $userIds", e)
            Result.failure(e)
        }
    }
}
