package com.example.run_app_rma.presentation.publish

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.run_app_rma.data.dao.RunDao
import com.example.run_app_rma.data.firestore.repository.RunPostRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.example.run_app_rma.domain.model.RunEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class PublishRunViewModel(
    private val runDao: RunDao,
    private val runPostRepository: RunPostRepository,
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    val localRuns = mutableStateListOf<RunEntity>()
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
//    private val _errorMessage: MutableState<String?> = mutableStateOf(null)
//    val errorMessage: State<String?> = _errorMessage
//    private val _successMessage: MutableState<String?> = mutableStateOf(null)
//    val successMessage: State<String?> = _successMessage

    // Event flow za slanje poruka na UI (npr. za Toast)
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadLocalRuns()
    }

    fun loadLocalRuns() {
        viewModelScope.launch {
            _isLoading.value = true
//            _errorMessage.value = null
//            _successMessage.value = null
            try {
                val runs = runDao.getAllRuns()
                localRuns.clear()
                localRuns.addAll(runs)
            } catch (e: Exception) {
//                _errorMessage.value = "Greška pri dohvatu lokalnih trčanja: ${e.message}"
                _eventFlow.emit(UiEvent.ShowToast("Greška pri dohvatu lokalnih trčanja: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteRun(runId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
//            _errorMessage.value = null
//            _successMessage.value = null
            try {
                runDao.deleteRunById(runId)
                localRuns.removeIf { it.id == runId }
//                _successMessage.value = "Trčanje uspješno izbrisano!"
                _eventFlow.emit(UiEvent.ShowToast("Trčanje uspješno izbrisano!"))
            } catch (e: Exception) {
//                _errorMessage.value = "Greška pri brisanju trčanja: ${e.message}"
                _eventFlow.emit(UiEvent.ShowToast("Greška pri brisanju trčanja: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    sealed class UiEvent {
        data class ShowToast(val message: String) : UiEvent()
    }

    class Factory(
        private val runDao: RunDao,
        private val runPostRepository: RunPostRepository,
        private val userRepository: UserRepository,
        private val firebaseAuth: FirebaseAuth
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PublishRunViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PublishRunViewModel(runDao, runPostRepository, userRepository, firebaseAuth) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}