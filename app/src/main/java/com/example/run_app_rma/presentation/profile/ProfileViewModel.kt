package com.example.run_app_rma.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.run_app_rma.data.firestore.model.User
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.example.run_app_rma.data.remote.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null) // This should be _currentUser
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow() // And this should be currentUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        fetchUserProfile()
    }

    fun fetchUserProfile() { // Make it public so it can be called after profile edit
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            _errorMessage.value = "User not logged in."
            _currentUser.value = null // Reset user if not logged in
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

    fun logout() { // Consistently name it 'logout'
        authRepository.logout()
        _currentUser.value = null // Clear user data in ViewModel
        _errorMessage.value = null // Clear any error messages
    }

    class Factory(
        private val userRepository: UserRepository,
        private val firebaseAuth: FirebaseAuth,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(userRepository, firebaseAuth, authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}