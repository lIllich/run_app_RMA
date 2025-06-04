package com.example.run_app_rma.data.firestore.repository

import com.example.run_app_rma.data.firestore.model.Comment
import com.example.run_app_rma.data.firestore.model.Like
import com.example.run_app_rma.data.firestore.model.RunPost
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class RunPostRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val postsCollection = firestore.collection("posts")
    private val likesCollection = firestore.collection("likes")
    private val commentsCollection = firestore.collection("comments")

    suspend fun createRunPost(runPost: RunPost): Result<String> {
        return try {
            val docRef = postsCollection.add(runPost).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRunPost(postId: String): Result<RunPost> {
        return try {
            val document = postsCollection.document(postId).get().await()
            val post = document.toObject(RunPost::class.java)

            if(post != null) {
                Result.success(post)
            } else {
                Result.failure(NoSuchElementException("Run post not found for ID: $postId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRunPostForUser(userId: String): Result<List<RunPost>> {
        return try {
            val posts = postsCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(RunPost::class.java)
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllOtherUsersRunPosts(currentUserId: String): Result<List<RunPost>> {
        return try {
            val posts = postsCollection
                .whereNotEqualTo("userId", currentUserId) // Exclude current user's posts
                .orderBy("timestamp", Query.Direction.ASCENDING) // <--- CHANGED TO ASCENDING TO MATCH FIRESTORE'S INDEX REQUIREMENT
                .get()
                .await()
                .toObjects(RunPost::class.java)
            Result.success(posts)
        } catch (e: Exception) {
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
            Result.success(likes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommentsByUser(userId: String): Result<List<Comment>> {
        return try {
            val comments = commentsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(Comment::class.java)
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return try {
            likesCollection.add(Like(postId = postId, userId = userId)).await()
            postsCollection.document(postId).update("likesCount", FieldValue.increment(1)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        return try {
            likesCollection
                .whereEqualTo("postId", postId)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .forEach { it.reference.delete().await() }

            postsCollection.document(postId).update("likesCount", FieldValue.increment(-1)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasUserLikedPost(postId: String, userId: String): Result<Boolean> {
        return try {
            val snapshot = likesCollection
                .whereEqualTo("postId", postId)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()
            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addComment(comment: Comment): Result<Unit> {
        return try {
            commentsCollection.add(comment).await()
            postsCollection.document(comment.postId).update("commentsCount", FieldValue.increment(1)).await()
            Result.success(Unit)
        } catch (e: Exception) {
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
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
