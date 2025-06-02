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
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _errorMessage.value = "Korisnik nije prijavljen."
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val result = userRepository.getUserProfile(userId)
            _isLoading.value = false
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Greška pri dohvatu profila."
            }
        }
    }

    fun updateUserProfile(userId: String, displayName: String, age: Int?, profileImageUri: Uri?) {
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null

        viewModelScope.launch {
            try {
                var newProfileImageUrl: String? = currentUser.value?.profileImageUrl

                if (profileImageUri != null) {
                    // Upload image to Firebase Storage
                    val storageRef = firebaseStorage.reference.child("profile_images/${userId}.jpg")
                    val uploadTask = storageRef.putFile(profileImageUri).await()
                    newProfileImageUrl = storageRef.downloadUrl.await().toString()
                }

                // Promjena ovdje: Koristi MutableMap<String, Any?> za podršku null vrijednostima
                val updates = mutableMapOf<String, Any?>()

                if (displayName != currentUser.value?.displayName) {
                    updates["displayName"] = displayName
                }
                // age je Int?, pa može biti null. Bitno je da mapa to podržava.
                if (age != currentUser.value?.age) {
                    updates["age"] = age
                }
                // newProfileImageUrl je String?, pa može biti null.
                if (newProfileImageUrl != currentUser.value?.profileImageUrl) {
                    updates["profileImageUrl"] = newProfileImageUrl
                }

                if (updates.isNotEmpty()) {
                    val result = userRepository.updateUserProfile(userId, updates as Map<String, Any>) // Firestore update method requires Map<String, Any> for non-null values. But if value is nullable, we cast to Any?
                    // NOTE: The updateUserProfile in UserRepository should ideally accept Map<String, Any?>
                    // If it still expects Map<String, Any>, you might need to handle nulls there
                    // or define a specific map for non-nullable updates.
                    // For now, let's assume `updateUserProfile` can handle null if the Firestore API allows it.
                    // If the problem persists, we might need to adjust UserRepository as well.

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
                _errorMessage.value = e.message ?: "Došlo je do greške."
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