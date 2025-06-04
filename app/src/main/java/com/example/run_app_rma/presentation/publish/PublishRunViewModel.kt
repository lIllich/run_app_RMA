package com.example.run_app_rma.presentation.publish

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.run_app_rma.data.dao.RunDao
import com.example.run_app_rma.data.firestore.model.RunPost
import com.example.run_app_rma.data.firestore.repository.RunPostRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.example.run_app_rma.domain.model.RunEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.launch
import java.util.Date
import java.text.DecimalFormat

class PublishRunViewModel(
    private val runDao: RunDao,
    private val runPostRepository: RunPostRepository,
    private val userRepository: UserRepository, // Needed to update user's total runs/distance
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _localRuns = mutableStateListOf<RunEntity>()
    val localRuns: List<RunEntity> = _localRuns

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _successMessage = mutableStateOf<String?>(null)
    val successMessage: State<String?> = _successMessage

    private val _selectedRun = mutableStateOf<RunEntity?>(null)
    val selectedRun: State<RunEntity?> = _selectedRun

    private val _caption = mutableStateOf("")
    val caption: State<String> = _caption

    private val decimalFormat = DecimalFormat("#.##") // For formatting distance/pace

    init {
        loadLocalRuns()
    }

    fun loadLocalRuns() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val runs = runDao.getAllRuns()
                _localRuns.clear()
                _localRuns.addAll(runs)
            } catch (e: Exception) {
                _errorMessage.value = "Greška pri učitavanju lokalnih trčanja: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectRun(run: RunEntity) {
        _selectedRun.value = run
        _caption.value = "" // Clear caption when a new run is selected
        clearMessages() // Clear any previous messages
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
                // Fetch location data for the selected run
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
                    timestamp = Date() // Firestore will set ServerTimestamp, but good to provide a local Date
                )

                val result = runPostRepository.createRunPost(runPost)
                if (result.isSuccess) {
                    _successMessage.value = "Trčanje uspješno objavljeno!"
                    // Optionally, update the local run to mark it as published
                    // You would need to add an 'isPublished: Boolean' field to RunEntity
                    // and then update the RunEntity in the Room database.
                    // For now, we'll just reload the list.
                    loadLocalRuns() // Reload runs to reflect changes (if any, like marking as published)
                    _selectedRun.value = null // Clear selected run after publishing
                    _caption.value = "" // Clear caption
                    updateUserProfileStats(currentUserId, runToPublish.distance, 1) // Update user stats
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Greška pri objavi trčanja."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Došlo je do greške: ${e.message}"
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
                println("Error fetching user profile for stats update: ${e.message}")
            }
        }
    }


    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
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
