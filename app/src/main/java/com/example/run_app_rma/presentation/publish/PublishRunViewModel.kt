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
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // New: State for pull-to-refresh
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _successMessage = mutableStateOf<String?>(null)
    val successMessage: State<String?> = _successMessage

    private val _selectedRun = mutableStateOf<RunEntity?>(null)
    val selectedRun: State<RunEntity?> = _selectedRun

    private val _caption = mutableStateOf("")
    val caption: State<String> = _caption

    private val decimalFormat = DecimalFormat("#.##")
    
    // Event flow za slanje poruka na UI (npr. za Toast)
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()


    init {
        loadLocalRuns()
    }

    fun loadLocalRuns() {
        viewModelScope.launch {
            if (!_isLoading.value) {
                _isRefreshing.value = true
            }

            _errorMessage.value = null
            _successMessage.value = null

            try {
                val runs = runDao.getAllRuns()
                localRuns.clear()
                localRuns.addAll(runs)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowToast("Greška pri dohvatu lokalnih trčanja: ${e.message}"))
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    fun deleteRun(runId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                runDao.deleteRunById(runId)
                localRuns.removeIf { it.id == runId }
                _eventFlow.emit(UiEvent.ShowToast("Trčanje uspješno izbrisano!"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowToast("Greška pri brisanju trčanja: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectRun(run: RunEntity) {
        _selectedRun.value = run
        _caption.value = ""
        clearMessages()
    }

    fun onCaptionChanged(newCaption: String) {
        _caption.value = newCaption
    }

    fun publishSelectedRun() {
        val runToPublish = _selectedRun.value
        val currentUserId = firebaseAuth.currentUser?.uid

        if (runToPublish == null) {
            _errorMessage.value = "Nema odabranog trčanja za objavu."
            return
        }
        if (currentUserId == null) {
            _errorMessage.value = "Korisnik nije prijavljen."
            return
        }
        if (runToPublish.endTime == null || runToPublish.distance == null || runToPublish.avgPace == null) {
            _errorMessage.value = "Odabrano trčanje nije završeno ili mu nedostaju podaci."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null

        viewModelScope.launch {
            try {
                val locationData = runDao.getLocationDataForRun(runToPublish.id)
                val polylineCoords = locationData.map { GeoPoint(it.lat, it.lon) }

                val runPost = RunPost(
                    userId = currentUserId,
                    localRunId = runToPublish.id,
                    startTime = runToPublish.startTime,
                    endTime = runToPublish.endTime,
                    distance = runToPublish.distance,
                    avgPace = runToPublish.avgPace,
                    polylineCoords = polylineCoords,
                    caption = _caption.value,
                    likesCount = 0,
                    commentsCount = 0,
                    timestamp = Date()
                )

                val result = runPostRepository.createRunPost(runPost)
                if (result.isSuccess) {
                    _successMessage.value = "Trčanje uspješno objavljeno!"
                    loadLocalRuns()
                    _selectedRun.value = null
                    _caption.value = ""
                    updateUserProfileStats(currentUserId, runToPublish.distance, 1)
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Greška pri objavi trčanja."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Greška pri objavi trčanja: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateUserProfileStats(userId: String, distance: Float, runsCount: Int) {
        viewModelScope.launch {
            val userResult = userRepository.getUserProfile(userId)
            userResult.onSuccess { user ->
                val currentTotalDistance = user.totalDistanceRun
                val currentTotalRuns = user.totalRuns
                val updates = mapOf(
                    "totalDistanceRun" to (currentTotalDistance + distance),
                    "totalRuns" to (currentTotalRuns + runsCount),
                    "lastRunTimestamp" to System.currentTimeMillis()
                )
                userRepository.updateUserProfile(userId, updates)
            }.onFailure { e ->
                println("Error updating user profile: ${e.message}")
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
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