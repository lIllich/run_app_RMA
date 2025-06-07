package com.example.run_app_rma.presentation.track

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.run_app_rma.data.dao.LocationDao
import com.example.run_app_rma.data.dao.RunDao
import com.example.run_app_rma.data.dao.SensorDao
import com.example.run_app_rma.domain.model.LocationDataEntity
import com.example.run_app_rma.domain.model.RunEntity
import com.example.run_app_rma.domain.model.SensorDataEntity
import com.example.run_app_rma.domain.model.SensorType
import com.example.run_app_rma.sensor.tracking.LocationService
import com.example.run_app_rma.sensor.tracking.SensorService
import com.example.run_app_rma.services.ShortcutManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class RunViewModel(
    application: Application,
    private val runDao: RunDao,
    private val locationDao: LocationDao,
    private val sensorDao: SensorDao,
    private val locationService: LocationService
) : AndroidViewModel(application) {

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking

    private val _currentRunId = MutableStateFlow<Long?>(null)
    val currentRunId: StateFlow<Long?> = _currentRunId

    private var currentRunStartTime: Long = 0L
    private var currentRunLocations = mutableListOf<Location>()

    val liveLocationData = mutableStateOf("Lat: N/A, Lng: N/A")
    val liveSensorData = mutableStateOf("Steps: N/A")

    init {
        locationService.startLocationUpdates { location ->
            liveLocationData.value = "Lat: ${location.latitude}, Lng: ${location.longitude}"

            if (_isTracking.value && _currentRunId.value != null) {
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
        // Sensor tracking is handled by the SensorService
        ShortcutManager.updateShortcuts(application.applicationContext, false)
    }

    fun startRun() {
        if (_isTracking.value) return

        _isTracking.value = true
        ShortcutManager.updateShortcuts(getApplication(), true)
        currentRunStartTime = System.currentTimeMillis()
        currentRunLocations.clear()

        viewModelScope.launch {
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
            }
            getApplication<Application>().startForegroundService(sensorServiceIntent)
            Log.d("RunViewModel", "SensorService started.")
        }
    }

    fun stopRun() {
        if (!_isTracking.value || _currentRunId.value == null) return

        val endTime = System.currentTimeMillis()
        _isTracking.value = false
        ShortcutManager.updateShortcuts(getApplication(), false)

        viewModelScope.launch {
            val runId = _currentRunId.value!!

            // Stop SensorService
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

            _currentRunId.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationService.stopLocationUpdates()
    }

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

    class Factory(
        private val application: Application,
        private val runDao: RunDao,
        private val locationDao: LocationDao,
        private val sensorDao: SensorDao,
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
                    locationService
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
