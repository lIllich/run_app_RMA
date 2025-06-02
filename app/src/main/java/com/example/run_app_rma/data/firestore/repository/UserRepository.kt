package com.example.run_app_rma.data.firestore.repository

import com.example.run_app_rma.data.firestore.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val usersCollection = firestore.collection("users")

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

    // ISPRAVKA OVDJE: Promijeni tip 'updates' na Map<String, Any?>
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
            val users = usersCollection
                .orderBy("displayName")
                .startAt(query)
                .endAt(query + '\uf8ff')
                .get()
                .await()
                .toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}