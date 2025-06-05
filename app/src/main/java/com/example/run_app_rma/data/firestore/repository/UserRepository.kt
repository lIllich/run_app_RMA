package com.example.run_app_rma.data.firestore.repository

import android.util.Log
import com.example.run_app_rma.data.firestore.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val usersCollection = firestore.collection("users")
    private val postsCollection = firestore.collection("posts") // Need reference to posts collection

    private val TAG = "UserRepository"

    suspend fun createUserProfile(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(userId: String): Result<User> {
        return try {
            val document = usersCollection.document(userId).get().await()
            val user = document.toObject(User::class.java)

            if(user != null) {
                Result.success(user)
            } else {
                Result.failure(NoSuchElementException("User profile not found for ID: $userId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(userId: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            usersCollection.document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val lowerCaseQuery = query.lowercase() // Convert query to lowercase
            val users = usersCollection
                .orderBy("lowercaseDisplayName") // Order and search by the new lowercase field
                .startAt(lowerCaseQuery) //
                .endAt(lowerCaseQuery + '\uf8ff') //
                .get()
                .await()
                .toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recalculates the total distance run for a specific user based on their posted runs
     * and updates the 'totalDistanceRun' field in their user profile in Firestore.
     * This function is intended for data correction or specific updates, not for every-day use.
     * @param userId The ID of the user whose total distance run needs to be recalculated.
     * @return Result.success(Unit) if successful, Result.failure(Exception) otherwise.
     */
    suspend fun recalculateAndSaveTotalDistanceRun(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Starting recalculation for user: $userId")

            // Fetch all run posts for this user
            val runPostsSnapshot = postsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            var totalDistance = 0f
            var totalRuns = 0

            // Sum the distances and count the runs from posts
            for (document in runPostsSnapshot.documents) {
                val distance = document.get("distance", Float::class.java) ?: 0f
                totalDistance += distance
                totalRuns++
                Log.d(TAG, "  - Found post: ${document.id}, distance: $distance")
            }

            Log.d(TAG, "Calculated totalDistance: $totalDistance km, totalRuns: $totalRuns for user $userId")

            // Update the user's profile with the new totalDistanceRun
            val updates = mapOf(
                "totalDistanceRun" to totalDistance,
                "totalRuns" to totalRuns
            )
            usersCollection.document(userId).update(updates).await()

            Log.d(TAG, "Successfully updated totalDistanceRun for user $userId to $totalDistance km.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error recalculating totalDistanceRun for user $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches all user IDs from the 'users' collection.
     * Useful for iterating through all users to perform a batch operation like recalculation.
     */
    suspend fun getAllUserIds(): Result<List<String>> {
        return try {
            val snapshot = usersCollection.get().await()
            val userIds = snapshot.documents.map { it.id }
            Log.d(TAG, "Fetched ${userIds.size} user IDs.")
            Result.success(userIds)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all user IDs", e)
            Result.failure(e)
        }
    }
}
