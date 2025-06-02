package com.example.run_app_rma.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.example.run_app_rma.data.firestore.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_LOGGED_IN = "user_logged_in"
        private const val KEY_CURRENT_USER_UID = "current_user_uid"
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
                    profileImageUrl = null
                )
                usersCollection.document(it.uid).set(newUser).await()
                sharedPreferences.edit().putBoolean(KEY_USER_LOGGED_IN, true).apply()
                sharedPreferences.edit().putString(KEY_CURRENT_USER_UID, it.uid).apply()
                Result.success(Unit)
            } ?: Result.failure(Exception("User creation failed."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
        sharedPreferences.edit().clear().apply() // Briše sve podatke o prijavi
        // Također, ako koristiš ViewModel, resetiraj relevantna stanja u ViewModelima
    }
}