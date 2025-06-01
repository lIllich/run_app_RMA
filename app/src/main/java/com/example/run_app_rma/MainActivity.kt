package com.example.run_app_rma

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.run_app_rma.data.db.AppDatabase
import com.example.run_app_rma.data.remote.AuthRepository
import com.example.run_app_rma.sensor.tracking.LocationService
import com.example.run_app_rma.sensor.tracking.SensorService
import com.example.run_app_rma.ui.theme.Run_app_RMATheme
import com.example.run_app_rma.presentation.login.LoginScreen
import com.example.run_app_rma.presentation.track.RunViewModel

class MainActivity : ComponentActivity() {

    private lateinit var locationService: LocationService
    private lateinit var sensorService: SensorService
    private lateinit var appDatabase: AppDatabase

    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Location permission required!", Toast.LENGTH_LONG).show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        locationService = LocationService(this)
        sensorService = SensorService(this)
        appDatabase = AppDatabase.getInstance(applicationContext)

        locationPermissionRequest.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)

        setContent {
            Run_app_RMATheme {
                val authRepository = remember { AuthRepository() }
                var isLoggedIn by remember { mutableStateOf(authRepository.isLoggedIn()) }

                val runViewModel: RunViewModel = viewModel(
                    factory = object: androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            if(modelClass.isAssignableFrom(RunViewModel::class.java)) {
                                @Suppress("UNCHECKED_CAST")
                                return RunViewModel(
                                    appDatabase.runDao(),
                                    appDatabase.locationDao(),
                                    appDatabase.sensorDao(),
                                    locationService,
                                    sensorService
                                ) as T
                            }
                            throw IllegalArgumentException("Unknown ViewModel class")
                        }
                    }
                )

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (isLoggedIn) {
                        TrackScreen(
                            modifier = Modifier.padding(innerPadding),
                            runViewModel = runViewModel,
                            onLogout = {
                                authRepository.logout()
                                isLoggedIn = false
                            }
                        )
                    } else {
                        LoginScreen(authRepository = authRepository) {
                            isLoggedIn = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackScreen(
    modifier: Modifier = Modifier,
    runViewModel: RunViewModel,
    onLogout: () -> Unit
) {
    val isTracking by runViewModel.isTracking.collectAsState()
    val locationText by runViewModel.liveLocationData
    val accelerometerText by runViewModel.liveAccelerometerData
    val gyroscopeText by runViewModel.liveGyroscopeData

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = locationText)
        Text(text = accelerometerText)
        Text(text = gyroscopeText)

        Spacer(modifier = Modifier.height(24.dp))

        if(!isTracking) {
            Button(
                onClick = { runViewModel.startRun() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start run")
            }
        } else {
            Button(
                onClick = { runViewModel.stopRun() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Stop run")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Run_app_RMATheme {
        Text("Hello Android!")
    }
}
