package com.example.run_app_rma.sensor.tracking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.run_app_rma.R
import com.example.run_app_rma.data.dao.SensorDao
import com.example.run_app_rma.data.db.AppDatabase
import com.example.run_app_rma.domain.model.SensorDataEntity
import com.example.run_app_rma.domain.model.SensorType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SensorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null
    private var initialStepCount: Int = 0
    private var currentSteps: Int = 0
    private var runId: Long = -1L

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorDao: SensorDao

    companion object {
        const val ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE"
        const val ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE"
        const val EXTRA_RUN_ID = "EXTRA_RUN_ID"
        private const val CHANNEL_ID = "SensorServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        sensorDao = AppDatabase.getInstance(applicationContext).sensorDao()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOREGROUND_SERVICE -> {
                runId = intent.getLongExtra(EXTRA_RUN_ID, -1L)
                if (runId != -1L) {
                    Log.d("SensorService", "Starting foreground service for Run ID: $runId")
                    startForegroundService()
                    startListening()
                } else {
                    Log.e("SensorService", "Run ID not provided. Stopping service.")
                    stopSelf()
                }
            }
            ACTION_STOP_FOREGROUND_SERVICE -> {
                Log.d("SensorService", "Stopping foreground service.")
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Run Tracking")
            .setContentText("Tracking your steps in the background...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1, notification)
    }

    private fun startListening() {
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d("SensorService", "Step counter listener registered.")
        } ?: run {
            Log.e("SensorService", "Step Counter sensor not available!")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()
            if (initialStepCount == 0) {
                initialStepCount = totalSteps
                Log.d("SensorService", "Initial step count: $initialStepCount")
            }
            currentSteps = totalSteps - initialStepCount
            Log.d("SensorService", "Steps since start: $currentSteps")

            if (runId != -1L) {
                val sensorData = SensorDataEntity(
                    runId = runId,
                    timestamp = System.currentTimeMillis(),
                    sensorType = SensorType.PEDOMETER,
                    steps = currentSteps
                )
                serviceScope.launch {
                    sensorDao.insertSensorData(sensorData)
                    Log.d("SensorService", "Saved step data for Run ID $runId: $currentSteps steps")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
        Log.d("SensorService", "Sensor listener unregistered and service destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sensor Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
