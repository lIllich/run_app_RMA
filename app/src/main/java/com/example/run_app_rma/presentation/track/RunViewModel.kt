package com.example.run_app_rma.presentation.track

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.run_app_rma.data.dao.ChallengeDao
import com.example.run_app_rma.data.dao.LocationDao
import com.example.run_app_rma.data.dao.RunDao
import com.example.run_app_rma.data.dao.SensorDao
import com.example.run_app_rma.domain.challenges.ChallengeUpdater
import com.example.run_app_rma.domain.model.LocationDataEntity
import com.example.run_app_rma.domain.model.RunEntity
import com.example.run_app_rma.domain.model.SensorType
import com.example.run_app_rma.sensor.tracking.LocationService
import com.example.run_app_rma.sensor.tracking.SensorService
import com.example.run_app_rma.services.ShortcutManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RunViewModel(
    application: Application,
    private val runDao: RunDao,
    private val locationDao: LocationDao,
    private val sensorDao: SensorDao,
    private val locationService: LocationService,
    private val challengeUpdater: ChallengeUpdater
) : AndroidViewModel(application) {

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking

    private val _currentRunId = MutableStateFlow<Long?>(null)
    val currentRunId: StateFlow<Long?> = _currentRunId

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime

    private val _distanceMeters = MutableStateFlow(0f)
    val distanceMeters: StateFlow<Float> = _distanceMeters

    private val _livePace = MutableStateFlow(0f)
    val livePace: StateFlow<Float> = _livePace

    private val _runFinished = MutableStateFlow(false)
    val runFinished: StateFlow<Boolean> = _runFinished

    private var flashJob: Job? = null

    val liveLocationData = mutableStateOf("Lat: N/A\nLng: N/A")
    private val liveSensorData = mutableStateOf("Steps: N/A")

    private var currentRunStartTime: Long = 0L
    private var currentRunLocations = mutableListOf<Location>()
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            distanceMeters.collect { distance ->
                GpsDistanceRepository.update(distance)
            }
        }

        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                locationService.startLocationUpdates { location ->
                    liveLocationData.value = "Lat: ${location.latitude}\nLng: ${location.longitude}"

                    if (_isTracking.value && _currentRunId.value != null) {
                        currentRunLocations.add(location)

                        if (currentRunLocations.size >= 2) {
                            val last = currentRunLocations[currentRunLocations.size - 2]
                            val current = currentRunLocations.last()
                            val added = last.distanceTo(current)
                            _distanceMeters.value += added
                        }

                        viewModelScope.launch {
                            val locationEntity = LocationDataEntity(
                                runId = _currentRunId.value!!,
                                timestamp = System.currentTimeMillis(),
                                sensorType = SensorType.GPS,
                                lat = location.latitude,
                                lon = location.longitude,
                                alt = location.altitude,
                                speed = location.speed
                            )
                            locationDao.insertLocationData(locationEntity)
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.e("Location", "Location permission not granted: ${e.message}")
            }
        } else {
            Log.w("Location", "Location permission not granted")
        }
        // sensor tracking is handled by the SensorService
        ShortcutManager.updateShortcuts(application.applicationContext, false)
    }

    fun startRun() {
        if (_isTracking.value) return

        _isTracking.value = true
        ShortcutManager.updateShortcuts(getApplication(), true)
        currentRunStartTime = System.currentTimeMillis()
        currentRunLocations.clear()

        viewModelScope.launch {
            startElapsedTimeUpdater()

            val newRun = RunEntity(
                startTime = currentRunStartTime,
                endTime = null,
                distance = null,
                avgPace = null,
                steps = 0
            )
            val runId = runDao.insert(newRun)
            _currentRunId.value = runId
            Log.d("RunViewModel", "Run started with ID: $runId")

            val sensorServiceIntent = Intent(getApplication(), SensorService::class.java).apply {
                action = SensorService.ACTION_START_FOREGROUND_SERVICE
                putExtra(SensorService.EXTRA_RUN_ID, runId)
                putExtra(SensorService.EXTRA_RUN_START_TIME, currentRunStartTime)
                putExtra(SensorService.EXTRA_INITIAL_DISTANCE, 0f)
            }
            getApplication<Application>().startForegroundService(sensorServiceIntent)
            Log.d("RunViewModel", "SensorService started.")
        }
    }

    private fun startElapsedTimeUpdater() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isTracking.value) {
                val elapsed = System.currentTimeMillis() - currentRunStartTime
                _elapsedTime.value = elapsed

                if (elapsed >= 5000 && _distanceMeters.value > 0f) {
                    val minutes = elapsed / 1000f / 60f
                    val km = _distanceMeters.value / 1000f
                    _livePace.value = minutes / km
                }

                delay(1000)
            }
        }
    }

    fun stopRun() {
        if (!_isTracking.value || _currentRunId.value == null) return

        timerJob?.cancel()

        val endTime = System.currentTimeMillis()
        _isTracking.value = false
        ShortcutManager.updateShortcuts(getApplication(), false)

        viewModelScope.launch {
            val runId = _currentRunId.value!!

            // stop SensorService
            val sensorServiceIntent = Intent(getApplication(), SensorService::class.java).apply {
                action = SensorService.ACTION_STOP_FOREGROUND_SERVICE
            }
            getApplication<Application>().stopService(sensorServiceIntent)
            Log.d("RunViewModel", "SensorService stopped.")

            val totalStepsForRun = sensorDao.getStepCountForRun(runId) ?: 0
            liveSensorData.value = "Steps: $totalStepsForRun"

            var totalDistance = 0f
            if (currentRunLocations.size >= 2) {
                for (i in 0 until currentRunLocations.size - 1) {
                    totalDistance += currentRunLocations[i].distanceTo(currentRunLocations[i + 1])
                }
            }

            val durationMillis = endTime - currentRunStartTime
            val durationMinutes = durationMillis / (1000f * 60f)
            val distanceKm = totalDistance / 1000f
            val avgPace = if (distanceKm > 0) durationMinutes / distanceKm else 0f

            val updatedRun = RunEntity(
                id = runId,
                startTime = currentRunStartTime,
                endTime = endTime,
                distance = totalDistance,
                avgPace = avgPace,
                steps = totalStepsForRun
            )
            runDao.update(updatedRun)
            Log.d("RunViewModel", "Run with ID $runId updated. Final steps: $totalStepsForRun")

            // update challenges
            challengeUpdater.updateChallengesAfterRun(updatedRun)

            // trigger UI flashing and reset (from main)
            _runFinished.value = true

            // launch flashing coroutine to keep final data visible (flashing) for 5 seconds, then reset
            flashJob?.cancel()
            flashJob = viewModelScope.launch {
                delay(5000)
                resetRunData()
            }

            _currentRunId.value = null
        }
    }

    // reset all tracking data to initial state
    private fun resetRunData() {
        _elapsedTime.value = 0L
        _distanceMeters.value = 0f
        _livePace.value = 0f
        GpsDistanceRepository.reset()
        liveSensorData.value = "Steps: 0"
        currentRunLocations.clear()
        currentRunStartTime = 0L
        _runFinished.value = false
    }

    override fun onCleared() {
        super.onCleared()
        locationService.stopLocationUpdates()
    }

    class Factory(
        private val application: Application,
        private val runDao: RunDao,
        private val locationDao: LocationDao,
        private val sensorDao: SensorDao,
        private val challengeDao: ChallengeDao,
        private val locationService: LocationService
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RunViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RunViewModel(
                    application,
                    runDao,
                    locationDao,
                    sensorDao,
                    locationService,
                    ChallengeUpdater(challengeDao, runDao, locationDao)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

object GpsDistanceRepository {
    private val _distance = MutableStateFlow(0f)
    val distance: StateFlow<Float> = _distance

    fun update(newValue: Float) {
        _distance.value = newValue
        Log.d("DistanceManager", "Updating distance: $newValue")
    }

    fun reset() {
        _distance.value = 0f
    }
}
