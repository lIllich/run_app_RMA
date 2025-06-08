package com.example.run_app_rma.sensor.tracking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.run_app_rma.MainActivity
import com.example.run_app_rma.R
import com.example.run_app_rma.data.dao.SensorDao
import com.example.run_app_rma.data.db.AppDatabase
import com.example.run_app_rma.domain.model.SensorDataEntity
import com.example.run_app_rma.domain.model.SensorType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SensorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null
    private var initialStepCount: Int = 0
    private var currentSteps: Int = 0
    private var runId: Long = -1L

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorDao: SensorDao

    private var notificationJob: Job? = null
    private var runStartTime: Long = 0L
    private var totalDistance = 0f

    companion object {
        const val ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE"
        const val ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE"
        const val EXTRA_RUN_ID = "EXTRA_RUN_ID"
        const val EXTRA_RUN_START_TIME = "EXTRA_RUN_START_TIME"
        private const val CHANNEL_ID = "SensorServiceChannel"
        const val EXTRA_INITIAL_DISTANCE = "EXTRA_INITIAL_DISTANCE"
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        sensorDao = AppDatabase.getInstance(applicationContext).sensorDao()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            runStartTime = intent.getLongExtra(EXTRA_RUN_START_TIME, System.currentTimeMillis())
            totalDistance = intent.getFloatExtra(EXTRA_INITIAL_DISTANCE, 0f)
        }

        when (intent?.action) {
            ACTION_START_FOREGROUND_SERVICE -> {
                runId = intent.getLongExtra(EXTRA_RUN_ID, -1L)
                if (runId != -1L) {
                    Log.d("SensorService", "Starting foreground service for Run ID: $runId")
                    startForegroundService()
                    startNotificationUpdates()
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
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Trčanje aktivno")
            .setContentText("Aplikacija prati tvoju lokaciju i broj koraka")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
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

            StepCountManager.updateStepCount(currentSteps)

            if (runId != -1L) {
                val sensorData = SensorDataEntity(
                    runId = runId,
                    timestamp = System.currentTimeMillis(),
                    sensorType = SensorType.PEDOMETER,
                    stepCount = currentSteps.toFloat()
                )
                serviceScope.launch {
                    sensorDao.insertSensorData(sensorData)
                    Log.d("SensorService", "Saved step data for Run ID $runId: $currentSteps steps")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // not used
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
        notificationJob?.cancel()
        Log.d("SensorService", "Sensor listener unregistered and service destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Sensor Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun startNotificationUpdates() {
        notificationJob = serviceScope.launch {
            while (isActive) {
                val elapsedMillis = System.currentTimeMillis() - runStartTime
                val minutes = (elapsedMillis / 1000) / 60
                val seconds = (elapsedMillis / 1000) % 60
                val stepCount = currentSteps
                val distanceKm = totalDistance / 1000

                val openAppIntent = Intent(this@SensorService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val openAppPendingIntent = PendingIntent.getActivity(
                    this@SensorService,
                    0,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(this@SensorService, CHANNEL_ID)
                    .setContentTitle("Run in Progress")
                    .setContentText("⏱ ${"%02d:%02d".format(minutes, seconds)} | " +
                            "🚶 $stepCount steps | " +
                            "📏 ${"%.2f".format(distanceKm)} km")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentIntent(openAppPendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .build()


                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1, notification)

                delay(1000)     // update every second
            }
        }
    }
}

object StepCountManager {
    private val _liveStepCount = MutableStateFlow(0)
    val liveStepCount: StateFlow<Int> = _liveStepCount

    fun updateStepCount(newCount: Int) {
        _liveStepCount.value = newCount
    }

    fun reset() {
        _liveStepCount.value = 0
    }
}
