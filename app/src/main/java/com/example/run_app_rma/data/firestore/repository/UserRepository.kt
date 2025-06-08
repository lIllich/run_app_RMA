package com.example.run_app_rma.data.firestore.repository

import android.util.Log
import com.example.run_app_rma.data.firestore.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Locale

class UserRepository(private val firestore: FirebaseFirestore) { // Added 'private val firestore: FirebaseFirestore'

    private val usersCollection = firestore.collection("users")
    private val postsCollection = firestore.collection("posts") // Assuming 'posts' collection is also managed here

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

    /**
     * Updates the weekly goals for a specific user in Firestore.
     *
     * @param userId The ID of the user whose goals are being updated.
     * @param steps The target number of steps for the week. Null if not set.
     * @param duration The target duration of running in milliseconds for the week. Null if not set.
     * @param distance The target distance of running in meters for the week. Null if not set.
     */
    suspend fun updateWeeklyGoals(
        userId: String,
        steps: Int?,
        duration: Long?,
        distance: Float?
    ): Result<Unit> {
        return try {
            val userRef = usersCollection.document(userId)
            val updates = hashMapOf<String, Any?>(
                "weeklyGoalSteps" to steps,
                "weeklyGoalDuration" to duration,
                "weeklyGoalDistance" to distance,
                "weeklyGoalsSetTimestamp" to System.currentTimeMillis() // Record when goals were set
            )
            userRef.update(updates).await()
            Log.d(TAG, "Successfully updated weekly goals for user $userId.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating weekly goals for user $userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Recalculates the total distance run and total runs for a user based on their posts
     * and updates the user's profile in Firestore.
     *
     * @param userId The ID of the user for whom to recalculate data.
     */
    suspend fun recalculateAndSaveTotalDistanceRun(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Recalculating totalDistanceRun for user: $userId")

            // fetch all run posts for the given user
            val runPostsSnapshot = postsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            var totalDistance = 0f // in meters
            var totalRuns = 0

            // iterate through the run posts to calculate total distances and count the runs from posts
            for (document in runPostsSnapshot.documents) {
                val distance = document.get("distance", Float::class.java) ?: 0f
                totalDistance += distance
                totalRuns++
                Log.d(TAG, "  - Found post: ${document.id}, distance: $distance")
            }

            Log.d(TAG, "Calculated totalDistance: $totalDistance m, totalRuns: $totalRuns for user $userId")

            // update the user's profile with the new totalDistanceRun
            val updates = mapOf(
                "totalDistanceRun" to totalDistance,
                "totalRuns" to totalRuns
            )
            usersCollection.document(userId).update(updates).await()

            Log.d(TAG, "Successfully updated totalDistanceRun for user $userId to ${totalDistance}m.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error recalculating totalDistanceRun for user $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves all user IDs from the "users" collection.
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

    /**
     * Searches for users whose display name or lowercase display name starts with the given query.
     * This is a basic prefix search. For more advanced searching, consider using a dedicated
     * search service like Algolia or Elasticsearch.
     *
     * @param query The search string.
     * @return A list of matching User objects.
     */
    suspend fun searchUsers(query: String): Result<List<User>> {
        if (query.isBlank()) {
            return Result.success(emptyList()) // Return empty list for blank queries
        }
        return try {
            val lowercaseQuery = query.lowercase(Locale.getDefault()) // Use Locale for consistency

            // Search by lowercaseDisplayName starting with the query
            val snapshot = usersCollection
                .orderBy("lowercaseDisplayName") // Order by the lowercase field for efficient querying
                .startAt(lowercaseQuery)
                .endAt(lowercaseQuery + '\uf8ff') // Unicode character to ensure "starts with" behavior
                .limit(50) // Limit results to prevent excessive reads
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { document ->
                document.toObject(User::class.java) // Convert each document to a User object
            }
            Log.d(TAG, "searchUsers: Found ${users.size} users for query '$query'.")
            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users for query '$query': ${e.message}", e)
            Result.failure(e)
        }
    }
}
