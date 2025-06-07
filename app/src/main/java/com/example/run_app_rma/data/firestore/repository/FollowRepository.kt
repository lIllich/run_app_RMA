package com.example.run_app_rma.data.firestore.repository

import android.util.Log     // for debugging
import com.example.run_app_rma.data.firestore.model.Follow
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FollowRepository(firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val followsCollection = firestore.collection("follows")
    private val TAG = "FollowRepository"    // tag for logging

    suspend fun followUser(followerId: String, followingId: String): Result<Unit> {
        Log.d(TAG, "followUser called: followerId=$followerId, followingId=$followingId")
        if (followerId == followingId) {
            Log.w(TAG, "Cannot follow yourself: followerId=$followerId, followingId=$followingId")
            return Result.failure(IllegalArgumentException("Cannot follow yourself"))
        }

        // explicit validation for empty/blank IDs before proceeding
        if (followerId.isBlank() || followingId.isBlank()) {
            Log.e(TAG, "Attempted to follow with blank ID: followerId='$followerId', followingId='$followingId'")
            return Result.failure(IllegalArgumentException("Follower or following ID cannot be blank."))
        }

        return try {
            // check if already following
            val existingFollow = followsCollection
                .whereEqualTo("followerId", followerId)
                .whereEqualTo("followingId", followingId)
                .limit(1)
                .get()
                .await()

            if (existingFollow.isEmpty) {
                Log.d(TAG, "Adding new follow document: followerId=$followerId, followingId=$followingId")
                followsCollection.add(Follow(followerId = followerId, followingId = followingId)).await()
                Log.d(TAG, "Follow document added successfully.")
            } else {
                Log.d(TAG, "Already following: followerId=$followerId, followingId=$followingId. No new document added.")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in followUser: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(followerId: String, followingId: String): Result<Unit> {
        Log.d(TAG, "unfollowUser called: followerId=$followerId, followingId=$followingId")

        // explicit validation for empty/blank IDs before proceeding
        if (followerId.isBlank() || followingId.isBlank()) {
            Log.e(TAG, "Attempted to unfollow with blank ID: followerId='$followerId', followingId='$followingId'")
            return Result.failure(IllegalArgumentException("Follower or following ID cannot be blank."))
        }

        return try {
            val querySnapshot = followsCollection
                .whereEqualTo("followerId", followerId)
                .whereEqualTo("followingId", followingId)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                Log.d(TAG, "Found ${querySnapshot.size()} follow documents to delete for followerId=$followerId, followingId=$followingId")
                querySnapshot.documents
                    .forEach {
                        Log.d(TAG, "Deleting follow document: ${it.id}")
                        it.reference.delete().await()
                    }
                Log.d(TAG, "Successfully deleted follow documents.")
            } else {
                Log.d(TAG, "No follow document found for followerId=$followerId, followingId=$followingId. No action taken.")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in unfollowUser: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun isFollowing(followerId: String, followingId: String): Result<Boolean> {
        if (followerId.isBlank() || followingId.isBlank()) {
            Log.e(TAG, "Attempted to check isFollowing with blank ID: followerId='$followerId', followingId='$followingId'")
            return Result.failure(IllegalArgumentException("Follower or following ID cannot be blank."))
        }
        return try {
            val snapshot = followsCollection
                .whereEqualTo("followerId", followerId)
                .whereEqualTo("followingId", followingId)
                .limit(1)
                .get()
                .await()
            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Log.e(TAG, "Error in isFollowing: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getFollowingIds(userId: String): Result<List<String>> {
        Log.d(TAG, "getFollowingIds called for userId=$userId")
        if (userId.isBlank()) {
            Log.e(TAG, "Attempted to getFollowingIds with blank userId.")
            return Result.failure(IllegalArgumentException("User ID cannot be blank."))
        }
        return try {
            val following = followsCollection
                .whereEqualTo("followerId", userId)
                .get()
                .await()
                .toObjects(Follow::class.java)
                .map { it.followingId }
            Log.d(TAG, "Fetched following IDs for $userId: $following")
            Result.success(following)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getFollowingIds for userId=$userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getFollowerIds(userId: String): Result<List<String>> {
        Log.d(TAG, "getFollowerIds called for userId=$userId")
        if (userId.isBlank()) {
            Log.e(TAG, "Attempted to getFollowerIds with blank userId.")
            return Result.failure(IllegalArgumentException("User ID cannot be blank."))
        }
        return try {
            val followers = followsCollection
                .whereEqualTo("followingId", userId)
                .get()
                .await()
                .toObjects(Follow::class.java)
                .map { it.followerId }
            Log.d(TAG, "Fetched follower IDs for $userId: $followers")
            Result.success(followers)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getFollowerIds for userId=$userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getFollowingCount(userId: String): Result<Int> {
        if (userId.isBlank()) {
            Log.e(TAG, "Attempted to getFollowingCount with blank userId.")
            return Result.failure(IllegalArgumentException("User ID cannot be blank."))
        }
        return try {
            val count = followsCollection
                .whereEqualTo("followerId", userId)
                .get()
                .await()
                .size()
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getFollowingCount for userId=$userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getFollowersCount(userId: String): Result<Int> {
        if (userId.isBlank()) {
            Log.e(TAG, "Attempted to getFollowersCount with blank userId.")
            return Result.failure(IllegalArgumentException("User ID cannot be blank."))
        }
        return try {
            val count = followsCollection
                .whereEqualTo("followingId", userId)
                .get()
                .await()
                .size()
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getFollowersCount for userId=$userId: ${e.message}", e)
            Result.failure(e)
        }
    }
}
