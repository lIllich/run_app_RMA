package com.example.run_app_rma.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.run_app_rma.data.firestore.model.User
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    fun fetchCurrentUserProfile() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val userId = firebaseAuth.currentUser?.uid
                if (userId != null) {
                    val result = userRepository.getUserProfile(userId)
                    if (result.isSuccess) {
                        _currentUser.value = result.getOrNull()
                    } else {
                        _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to load profile."
                    }
                } else {
                    _errorMessage.value = "User not logged in."
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An error occurred while fetching profile."
            } finally {
                _isLoading.value = false
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
        viewModelScope.launch {
            try {
                var imageUrl: String? = currentUser.value?.profileImageUrl

                if (profileImageUri != null) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        val imageRef = firebaseStorage.reference
                            .child("profile_images/${user.uid}/${UUID.randomUUID()}.jpg")

                        val uploadTask = imageRef.putFile(profileImageUri).await()
                        imageUrl = uploadTask.storage.downloadUrl.await().toString()
                    } else {
                        _errorMessage.value = "User not authenticated for image upload."
                        _isLoading.value = false
                        return@launch
                    }
                } else if (currentUser.value?.profileImageUrl != null && (profileImageUri == null && imageUrl?.isEmpty() != false)) {
                    // If URI is null and current profile has an image, and it's not explicitly keeping it, set to empty.
                    // This handles cases where user wants to remove the image.
                    imageUrl = ""
                }


                val updatedUserMap = mutableMapOf<String, Any>()
                val currentDisplayName = _currentUser.value?.displayName
                val currentAge = _currentUser.value?.age
                val currentProfileImageUrl = _currentUser.value?.profileImageUrl

                var hasChanges = false

                if (displayName != currentDisplayName) {
                    updatedUserMap["displayName"] = displayName
                    hasChanges = true
                }
                if (age != currentAge) {
                    updatedUserMap["age"] = age ?: 0
                    hasChanges = true
                }
                // Check if imageUrl has changed, including setting to null/empty
                if (imageUrl != currentProfileImageUrl) {
                    updatedUserMap["profileImageUrl"] = imageUrl ?: ""
                    hasChanges = true
                }

                if (hasChanges) {
                    val result = userRepository.updateUserProfile(userId, updatedUserMap)

                    if (result.isSuccess) {
                        _successMessage.value = "Profil uspješno ažuriran!"
                        fetchCurrentUserProfile() // Fetch updated profile to reflect changes immediately
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