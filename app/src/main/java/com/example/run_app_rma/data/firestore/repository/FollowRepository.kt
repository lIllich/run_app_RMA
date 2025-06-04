package com.example.run_app_rma.data.firestore.repository

import com.example.run_app_rma.data.firestore.model.Follow
import com.example.run_app_rma.data.firestore.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.tasks.await

class FollowRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val followsCollection = firestore.collection("follows")

    suspend fun followUser(followerId: String, followingId: String): Result<Unit> {
        if(followerId == followingId) return Result.failure(IllegalArgumentException("Cannot follow yourself"))

        return try {
            // check if already following
            val existingFollow = followsCollection
                .whereEqualTo("followerId", followerId)
                .whereEqualTo("followingId", followingId)
                .limit(1)
                .get()
                .await()

            if(existingFollow.isEmpty) {
                followsCollection.add(Follow(followerId = followerId, followingId = followingId)).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(followerId: String, followingId: String): Result<Unit> {
        return try {
            followsCollection
                .whereEqualTo("followerId", followerId)
                .whereEqualTo("followingId", followingId)
                .get()
                .await()
                .documents
                .forEach { it.reference.delete().await() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFollowing(followerId: String, followingId: String): Result<Boolean> {
        return try {
            val snapshot = followsCollection
                .whereEqualTo("followerId", followerId)
                .whereEqualTo("followingId", followingId)
                .limit(1)
                .get()
                .await()
            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowingIds(userId: String): Result<List<String>> {
        return try {
            val following = followsCollection
                .whereEqualTo("followerId", userId)
                .get()
                .await()
                .toObjects(Follow::class.java)
                .map { it.followingId }
            Result.success(following)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowerIds(userId: String): Result<List<String>> {
        return try {
            val followers = followsCollection
                .whereEqualTo("followingId", userId)
                .get()
                .await()
                .toObjects(Follow::class.java)
                .map { it.followerId }
            Result.success(followers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get the number of users a given user is following.
     */
    suspend fun getFollowingCount(userId: String): Result<Int> {
        return try {
            val count = followsCollection
                .whereEqualTo("followerId", userId)
                .get()
                .await()
                .size()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get the number of followers for a given user.
     */
    suspend fun getFollowersCount(userId: String): Result<Int> {
        return try {
            val count = followsCollection
                .whereEqualTo("followingId", userId)
                .get()
                .await()
                .size()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
