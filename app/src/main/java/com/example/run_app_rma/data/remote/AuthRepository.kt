package com.example.run_app_rma.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log // Import Log for debugging
import com.example.run_app_rma.data.firestore.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging // Import FirebaseMessaging
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    // Add a property for FirebaseMessaging
    private val firebaseMessaging: FirebaseMessaging = FirebaseMessaging.getInstance()

    companion object {
        private const val KEY_USER_LOGGED_IN = "user_logged_in"
        private const val KEY_CURRENT_USER_UID = "current_user_uid"
        private const val TAG = "AuthRepository" // Tag for logging
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null && sharedPreferences.getBoolean(KEY_USER_LOGGED_IN, false)
    }

    fun getCurrentUserId(): String? {
        return sharedPreferences.getString(KEY_CURRENT_USER_UID, null)
    }

    suspend fun loginUser(email: String, password: String): Result<Unit> {
        return try {
            val userCredential = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = userCredential.user
            firebaseUser?.let {
                sharedPreferences.edit().putBoolean(KEY_USER_LOGGED_IN, true).apply()
                sharedPreferences.edit().putString(KEY_CURRENT_USER_UID, it.uid).apply()

                // --- NEW: Get and save FCM token after successful login ---
                try {
                    val token = firebaseMessaging.token.await()
                    if (token.isNotEmpty()) {
                        usersCollection.document(it.uid).update("fcmToken", token).await()
                        Log.d(TAG, "FCM token updated for user ${it.uid} after login.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get or save FCM token after login: ${e.message}", e)
                    // Continue with login success even if token update fails, it's not critical
                }
                // --- END NEW ---

                Result.success(Unit)
            } ?: Result.failure(Exception("Login failed: User not found."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerUser(email: String, password: String, displayName: String, age: Int?): Result<Unit> {
        return try {
            val userCredential = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = userCredential.user

            firebaseUser?.let {
                val newUser = User(
                    id = it.uid,
                    displayName = displayName,
                    email = it.email ?: email,
                    age = age,
                    lowercaseDisplayName = displayName.lowercase(),
                    profileImageUrl = null,
                    fcmToken = null // Temporarily null, will be updated below
                )
                usersCollection.document(it.uid).set(newUser).await()
                sharedPreferences.edit().putBoolean(KEY_USER_LOGGED_IN, true).apply()
                sharedPreferences.edit().putString(KEY_CURRENT_USER_UID, it.uid).apply()

                // --- NEW: Get and save FCM token after successful registration ---
                try {
                    val token = firebaseMessaging.token.await()
                    if (token.isNotEmpty()) {
                        // Update the user document just created with the FCM token
                        usersCollection.document(it.uid).update("fcmToken", token).await()
                        Log.d(TAG, "FCM token updated for user ${it.uid} after registration.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get or save FCM token after registration: ${e.message}", e)
                    // Continue with registration success even if token update fails
                }
                // --- END NEW ---

                Result.success(Unit)
            } ?: Result.failure(Exception("User creation failed."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
        sharedPreferences.edit().clear().apply() // Clears all login data
        // Also, if you use ViewModel, reset relevant states in ViewModels
    }
}
