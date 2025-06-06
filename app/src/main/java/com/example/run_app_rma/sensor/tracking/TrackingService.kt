package com.example.run_app_rma.sensor.tracking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.run_app_rma.MainActivity // Import your MainActivity
import com.example.run_app_rma.R // Import your R.java for resources (e.g., app_name, ic_launcher_foreground)
import com.example.run_app_rma.sensor.tracking.LocationService // Import your existing LocationService
import com.example.run_app_rma.data.db.AppDatabase // Import your AppDatabase
import com.example.run_app_rma.domain.model.LocationDataEntity
import com.example.run_app_rma.domain.model.SensorType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TrackingService : Service() {

    private lateinit var locationService: LocationService
    private lateinit var appDatabase: AppDatabase
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO) // Scope za coroutine-e unutar servisa

    private var currentRunId: Long? = null // Za spremanje ID-a trčanja

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "run_tracking_channel"
        const val NOTIFICATION_ID = 1 // Mora biti jedinstven za Foreground Service
        const val ACTION_START_OR_RESUME_SERVICE = "ACTION_START_OR_RESUME_SERVICE"
        const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE" // Ako želite pauzirati praćenje
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val EXTRA_RUN_ID = "EXTRA_RUN_ID"
    }

    override fun onCreate() {
        super.onCreate()
        locationService = LocationService(applicationContext)
        appDatabase = AppDatabase.getInstance(applicationContext) // Inicijalizirajte bazu podataka
        createNotificationChannel() // Kreirajte kanal za obavijesti
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    currentRunId = it.getLongExtra(EXTRA_RUN_ID, -1L).takeIf { id -> id != -1L }
                    startForegroundServiceWithNotification()
                    startLocationTracking()
                }
                ACTION_PAUSE_SERVICE -> {
                    // Implementirajte logiku za pauziranje ako je potrebno
                    // npr. zaustavite ažuriranja lokacije, ali servis ostaje aktivan
                    stopLocationTracking()
                }
                ACTION_STOP_SERVICE -> {
                    stopTrackingService()
                }
            }
        }
        return START_NOT_STICKY // Servis se neće automatski ponovno pokrenuti ako ga sustav ubije
    }

    private fun startForegroundServiceWithNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false) // Obavijest ostaje dok je servis aktivan
            .setOngoing(true) // Obavijest se ne može pomaknuti
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Koristite ikonu vaše aplikacije
            .setContentTitle("Praćenje trčanja")
            .setContentText("Vaše trčanje se prati u pozadini...")
            .setContentIntent(getMainActivityPendingIntent()) // Klik na obavijest otvara MainActivity

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun startLocationTracking() {
        locationService.startLocationUpdates { location ->
            // Ovdje dobivate ažuriranja lokacije
            // Spremajte lokaciju u vašu bazu podataka
            currentRunId?.let { runId ->
                serviceScope.launch {
                    val locationDao = appDatabase.locationDao()
                    val newLocationData = LocationDataEntity(
                        runId = runId,
                        lat = location.latitude,
                        lon = location.longitude,
                        alt = location.altitude,
                        timestamp = location.time,
                        speed = location.speed,
                        sensorType = SensorType.GPS
                    )
                    locationDao.insertLocationData(newLocationData)
                    // Možete ovdje emitirati ove lokacije putem Flow-a ili LiveData-e
                    // kako bi se UI ažurirao u stvarnom vremenu ako je aplikacija u prvom planu.
                    // Npr. _locationFlow.emit(newLocationData)
                }
            }
        }
    }

    private fun stopLocationTracking() {
        locationService.stopLocationUpdates()
    }

    private fun stopTrackingService() {
        stopLocationTracking() // Zaustavite ažuriranja lokacije
        stopForeground(STOP_FOREGROUND_REMOVE) // Uklonite obavijest
        stopSelf() // Zaustavite servis
    }

    private fun getMainActivityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Run Tracking Service Channel",
            NotificationManager.IMPORTANCE_LOW // Koristite LOW da ne bi bilo previše nametljivo
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Ovaj servis neće biti direktno vezan za aktivnosti
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // Obustavite coroutine scope
    }
}