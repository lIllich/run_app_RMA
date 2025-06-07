package com.example.run_app_rma.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.run_app_rma.data.firestore.model.User
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class EditProfileViewModel(
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseStorage: FirebaseStorage
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        fetchCurrentUserProfile()
    }

    private fun fetchCurrentUserProfile() {
        _errorMessage.value = null
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _errorMessage.value = "User not logged in."
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val result = userRepository.getUserProfile(userId)
            _isLoading.value = false
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to load profile."
            }
        }
    }

    fun updateUserProfile(
        userId: String,
        displayName: String,
        age: Int?,
        profileImageUri: Uri?
    ) {
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null

        viewModelScope.launch {
            try {
                val currentUserId = firebaseAuth.currentUser?.uid
                if (currentUserId == null || currentUserId != userId) {
                    _errorMessage.value = "Authentication error or unauthorized action."
                    return@launch
                }

                var imageUrl: String? = currentUser.value?.profileImageUrl

                // handle image upload if a new URI is provided
                if (profileImageUri != null) {
                    val uploadResult = uploadProfileImage(userId, profileImageUri)
                    if (uploadResult.isSuccess) {
                        imageUrl = uploadResult.getOrNull()
                    } else {
                        _errorMessage.value = uploadResult.exceptionOrNull()?.message ?: "Failed to upload image."
                        _isLoading.value = false
                        return@launch
                    }
                }

                val updates = mutableMapOf<String, Any?>()
                if (displayName != currentUser.value?.displayName) {
                    updates["displayName"] = displayName
                    updates["lowercaseDisplayName"] = displayName.lowercase()
                }
                if (age != currentUser.value?.age) {
                    updates["age"] = age
                }
                if (imageUrl != currentUser.value?.profileImageUrl) {
                    updates["profileImageUrl"] = imageUrl
                }

                if (updates.isNotEmpty()) {
                    val result = userRepository.updateUserProfile(userId, updates)

                    if (result.isSuccess) {
                        _successMessage.value = "Profil uspješno ažuriran!"
                        fetchCurrentUserProfile()
                    } else {
                        _errorMessage.value = result.exceptionOrNull()?.message ?: "Ažuriranje profila neuspješno."
                    }
                } else {
                    _successMessage.value = "Nema promjena za spremanje."
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Došlo je do greške prilikom ažuriranja profila."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun uploadProfileImage(userId: String, uri: Uri): Result<String> {
        return try {
            val storageRef = firebaseStorage.reference
            val imageRef = storageRef.child("profile_images/$userId/${UUID.randomUUID()}.jpg")
            val uploadTask = imageRef.putFile(uri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    class Factory(
        private val userRepository: UserRepository,
        private val firebaseAuth: FirebaseAuth,
        private val firebaseStorage: FirebaseStorage
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EditProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return EditProfileViewModel(userRepository, firebaseAuth, firebaseStorage) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}