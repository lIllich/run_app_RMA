package com.example.run_app_rma.presentation.publish

import android.location.Location
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.run_app_rma.data.dao.LocationDao
import com.example.run_app_rma.data.dao.RunDao
import com.example.run_app_rma.data.firestore.model.RunPost
import com.example.run_app_rma.data.firestore.repository.RunPostRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.example.run_app_rma.domain.model.LocationDataEntity
import com.example.run_app_rma.domain.model.RunEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RunDetailsViewModel(
    private val runId: Long,
    private val runDao: RunDao,
    private val locationDao: LocationDao,
    private val runPostRepository: RunPostRepository,
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _runDetails = MutableStateFlow<RunEntity?>(null)
    val runDetails: StateFlow<RunEntity?> = _runDetails

    private val _locationData = MutableStateFlow<List<LocationDataEntity>>(emptyList())
    val locationData: StateFlow<List<LocationDataEntity>> = _locationData

    private val _caption = mutableStateOf("")
    val caption: State<String> = _caption

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage: MutableState<String?> = mutableStateOf(null)
    val errorMessage: State<String?> = _errorMessage

    private val _successMessage: MutableState<String?> = mutableStateOf(null)
    val successMessage: State<String?> = _successMessage

    init {
        loadRunDetails()
//        loadLocationData()
    }

    private fun recalculateRun(run: RunEntity, locationData: List<LocationDataEntity>): RunEntity {
        val sorted = locationData.sortedBy { it.timestamp }
        println("here 1")

        if (sorted.isEmpty()) {
            println("here x")
            // If there's no location data, set distance and pace to 0 and end time to start time.
            return run.copy(distance = 0f, avgPace = 0f, endTime = run.startTime)
        }

        var totalDistance = 0.0
        for (i in 0 until sorted.size - 1) {
            val start = sorted[i]
            val end = sorted[i + 1]

            totalDistance += calculateHaversineDistance(
                lat1 = start.lat,
                lon1 = start.lon,
                lat2 = end.lat,
                lon2 = end.lon
            )
        }

        val durationMillis = sorted.last().timestamp - sorted.first().timestamp
        println(sorted.last().timestamp)
        println(sorted.first().timestamp)
        println(durationMillis)
        val durationMinutes = durationMillis / (1000f * 60f)
        val distanceKm = totalDistance / 1000.0
        val avgPace = if (distanceKm > 0.0) durationMinutes / distanceKm else 0.0

        return run.copy(
            distance = totalDistance.toFloat(),
            avgPace = avgPace.toFloat(),
            startTime = sorted.first().timestamp,
            endTime = sorted.last().timestamp
        )
    }

    private fun loadRunDetails() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val run = runDao.getRunById(runId)
                val locationData = locationDao.getLocationDataForRun(runId)
                _locationData.value = locationData

                if (run != null) {
                    val updatedRun = recalculateRun(run, locationData)
                    runDao.update(updatedRun) // Save the recalculated run to Room DB
                    _runDetails.value = updatedRun // Update the UI StateFlow
                } else {
                    _errorMessage.value = "Trčanje s ID-jem $runId nije pronađeno."
                }

            } catch (e: Exception) {
                _errorMessage.value = "Greška pri dohvatu detalja trčanja: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

//    private fun loadLocationData() {
//        viewModelScope.launch {
//            _isLoading.value = true
//            _errorMessage.value = null
//            try {
//                _locationData.value = locationDao.getLocationDataForRun(runId)
//            } catch (e: Exception) {
//                _errorMessage.value = "Greška pri dohvatu lokacijskih podataka: ${e.message}"
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }

    fun onCaptionChanged(newCaption: String) {
        _caption.value = newCaption
    }

    fun publishRun() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            try {
                val currentUserId = firebaseAuth.currentUser?.uid
                if (currentUserId == null) {
                    _errorMessage.value = "Korisnik nije prijavljen."
                    return@launch
                }

                val run = _runDetails.value
                if (run == null) {
                    _errorMessage.value = "Nije pronađeno trčanje za objavu."
                    return@launch
                }

                val polylineCoordinates = _locationData.value.map { GeoPoint(it.lat, it.lon) }

                val runPost = RunPost(
                    userId = currentUserId,
                    localRunId = run.id,
                    startTime = run.startTime,
                    endTime = run.endTime ?: 0L,
                    distance = run.distance ?: 0f,
                    avgPace = run.avgPace ?: 0f,
                    polylineCoords = polylineCoordinates,
                    caption = _caption.value,
                    likesCount = 0,
                    commentsCount = 0
                )
                val result = runPostRepository.createRunPost(runPost)

                result.onSuccess { postId ->
                    // Update user's total runs and distance after successful publish
                    updateUserStats(currentUserId, run.distance ?: 0f)
                    _successMessage.value = "Trčanje uspješno objavljeno! Post ID: $postId"
                }.onFailure { e ->
                    _errorMessage.value = "Greška pri objavi trčanja: ${e.message}"
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Greška pri objavi trčanja: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun updateUserStats(userId: String, distance: Float) {
        viewModelScope.launch {
            val userResult = userRepository.getUserProfile(userId)
            userResult.onSuccess { user ->
                val currentTotalDistance = user.totalDistanceRun ?: 0f
                val currentTotalRuns = user.totalRuns ?: 0
                val updates = mapOf(
                    "totalDistanceRun" to (currentTotalDistance + distance),
                    "totalRuns" to (currentTotalRuns + 1),
                    "lastRunTimestamp" to System.currentTimeMillis()
                )
                userRepository.updateUserProfile(userId, updates)
            }.onFailure { e ->
                println("Error fetching user profile for stats update: ${e.message}")
                _errorMessage.value = "Greška pri ažuriranju korisničkih statistika: ${e.message}"
            }
        }
    }

    fun getAverageSpeed(run: RunEntity): String {
        val durationMillis = run.endTime?.let { it - run.startTime }
        val distanceMeters = run.distance

        return if (durationMillis != null && distanceMeters != null && durationMillis > 0) {
            val durationHours = durationMillis / (1000.0 * 60 * 60)
            val distanceKm = distanceMeters / 1000.0
            val speedKmH = distanceKm / durationHours
            DecimalFormat("#.##").format(speedKmH) + " km/h"
        } else {
            "N/A"
        }
    }

    fun getDurationFormatted(run: RunEntity): String {
        val durationMillis = run.endTime?.let { it - run.startTime }
        return if (durationMillis != null) {
            val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            "N/A"
        }
    }

    fun getElevationGain(locationData: List<LocationDataEntity>): String {
        if (locationData.size < 2) return "N/A"

        var elevationGain = 0.0
        for (i in 1 until locationData.size) {
            val prevAlt = locationData[i - 1].alt
            val currentAlt = locationData[i].alt
            if (currentAlt > prevAlt) {
                elevationGain += (currentAlt - prevAlt)
            }
        }
        return "${DecimalFormat("#.##").format(elevationGain)} m"
    }

//    fun getRouteLength(locationData: List<LocationDataEntity>): String {
//        if (locationData.size < 2) return "N/A"
//
//        var totalDistance = 0.0
//        for (i in 1 until locationData.size) {
//            val loc1 = locationData[i - 1]
//            val loc2 = locationData[i]
//            totalDistance += calculateHaversineDistance(loc1.lat, loc1.lon, loc2.lat, loc2.lon)
//        }
//        return "${DecimalFormat("#.##").format(totalDistance / 1000)} km"
//    }

    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371e3 // metres
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c    // distance in meters
    }

    fun getKilometerSplits(locationData: List<LocationDataEntity>): List<Pair<String, String>> {
        if (locationData.size < 2) return emptyList()

        val splits = mutableListOf<Pair<String, String>>()
        var currentKmDistance = 0.0 // meters
        var currentSplitStartTime = locationData.first().timestamp

        for (i in 1 until locationData.size) {
            val prevLoc = locationData[i - 1]
            val currLoc = locationData[i]

            val distanceSegment = calculateHaversineDistance(prevLoc.lat, prevLoc.lon, currLoc.lat, currLoc.lon)
            currentKmDistance += distanceSegment

            if (currentKmDistance >= 1000.0) {
                val kmDurationMillis = currLoc.timestamp - currentSplitStartTime
                val paceMillisPerKm = kmDurationMillis / (currentKmDistance / 1000.0)
                val paceMinutes = TimeUnit.MILLISECONDS.toMinutes(paceMillisPerKm.toLong())
                val paceSeconds = TimeUnit.MILLISECONDS.toSeconds(paceMillisPerKm.toLong()) % 60

                val hours = TimeUnit.MILLISECONDS.toHours(kmDurationMillis)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(kmDurationMillis) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(kmDurationMillis) % 60

                val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                val paceFormatted = String.format("%02d:%02d min/km", paceMinutes, paceSeconds)

                splits.add(Pair(timeFormatted, paceFormatted))

                currentKmDistance %= 1000.0 // remaining distance from last km
                currentSplitStartTime = currLoc.timestamp
            }
        }
        return splits
    }

    class Factory(
        private val runId: Long,
        private val runDao: RunDao,
        private val locationDao: LocationDao,
        private val runPostRepository: RunPostRepository?,
        private val userRepository: UserRepository?,
        private val firebaseAuth: FirebaseAuth?
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RunDetailsViewModel::class.java)) {
                // Ensure all nullable parameters are non-null before constructing the ViewModel
                if (runPostRepository != null && userRepository != null && firebaseAuth != null) {
                    @Suppress("UNCHECKED_CAST")
                    return RunDetailsViewModel(
                        runId,
                        runDao,
                        locationDao,
                        runPostRepository,
                        userRepository,
                        firebaseAuth
                    ) as T
                } else {
                    // Throw an informative exception if any required dependency is missing
                    throw IllegalArgumentException("Missing required repositories or FirebaseAuth for RunDetailsViewModel. Check if runPostRepository, userRepository, or firebaseAuth are null.")
                }
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}