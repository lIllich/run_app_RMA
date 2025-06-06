package com.example.run_app_rma.presentation.track

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider // Import ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.run_app_rma.data.dao.LocationDao
import com.example.run_app_rma.data.dao.RunDao
import com.example.run_app_rma.domain.model.LocationDataEntity
import com.example.run_app_rma.domain.model.RunEntity
import com.example.run_app_rma.domain.model.SensorType
import com.example.run_app_rma.sensor.tracking.LocationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class RunViewModel(
    private val runDao: RunDao,
    private val locationDao: LocationDao,
    private val locationService: LocationService,
    private val onStartTrackingService: (Long) -> Unit,
    private val onStopTrackingService: () -> Unit
) : ViewModel() {

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking

    private val _currentRunId = MutableStateFlow<Long?>(null)
    val currentRunId: StateFlow<Long?> = _currentRunId.asStateFlow()

    private var currentRunStartTime: Long = 0L
    private var currentRunLocations = mutableStateListOf<Location>()

    private var LocationJob: Job? = null

    val liveLocationData = mutableStateOf("Lat: N/A, Lng: N/A")

    init {
        // inicijalizacija servisa za prikaz u stvarnom vremenu
        locationService.startLocationUpdates { location ->
            liveLocationData.value = "Lat: ${location.latitude}, Lng: ${location.longitude}"

            if(_isTracking.value) {
                currentRunLocations.add(location)
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
    }

    fun startRun() {
        if(_isTracking.value) return

        _isTracking.value = true
        currentRunStartTime = System.currentTimeMillis()
        currentRunLocations.clear()

        viewModelScope.launch {
            val newRun = RunEntity(
                startTime = currentRunStartTime,
                endTime = null,
                distance = null,
                avgPace = null
            )
            val runId = runDao.insert(newRun)
            _currentRunId.value = runId
        }

        _currentRunId.value?.let { id ->
            onStartTrackingService(id)
        }
    }

    fun stopRun() {
        if(!_isTracking.value || _currentRunId.value == null) return

        val endTime = System.currentTimeMillis()
        _isTracking.value = false

        viewModelScope.launch {
            val runId = _currentRunId.value!!

            var totalDistance = 0f
            if(currentRunLocations.size >= 2) {
                for(i in 0 until currentRunLocations.size - 1) {
                    totalDistance += currentRunLocations[i].distanceTo(currentRunLocations[i + 1])
                }
            }

            val durationMillis = endTime - currentRunStartTime
            val durationMinutes = durationMillis / (1000f * 60f)
            val distanceKm = totalDistance / 1000f  // metri u kilometre
            val avgPace = if (distanceKm > 0) durationMinutes / distanceKm else 0f  // min/km

            val updatedRun = RunEntity(
                id = runId,
                startTime = currentRunStartTime,
                endTime = endTime,
                distance = totalDistance,
                avgPace = avgPace
            )
            runDao.update(updatedRun)

            onStopTrackingService()
            _currentRunId.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationService.stopLocationUpdates()
    }

    // Factory for RunViewModel
    class Factory(
        private val runDao: RunDao,
        private val locationDao: LocationDao,
        private val locationService: LocationService,
        private val onStartTrackingService: (Long) -> Unit,
        private val onStopTrackingService: () -> Unit
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RunViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RunViewModel(runDao, locationDao, locationService, onStartTrackingService, onStopTrackingService) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    // debug
    fun exportDatabase(context: Context) {
        val dbName = "run_app_database"
        val dbPath = context.getDatabasePath(dbName)
        val destDir = context.getExternalFilesDir(null)
        val destDb = File(destDir, dbName)
        val walFile = File(dbPath.absolutePath + "-wal")
        val shmFile = File(dbPath.absolutePath + "-shm")

        try {
            dbPath.copyTo(destDb, overwrite = true)
            walFile.copyTo(File(destDir, walFile.name), overwrite = true)
            shmFile.copyTo(File(destDir, shmFile.name), overwrite = true)
            Log.d("DB_EXPORT", "Exported DB to ${destDb.absolutePath}")
        } catch (e: Exception) {
            Log.e("DB_EXPORT", "Error exporting DB", e)
        }
    }
    //***
}
