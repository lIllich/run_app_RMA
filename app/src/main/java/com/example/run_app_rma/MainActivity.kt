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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.run_app_rma.data.db.AppDatabase
import com.example.run_app_rma.data.remote.AuthRepository
import com.example.run_app_rma.sensor.tracking.LocationService
import com.example.run_app_rma.sensor.tracking.SensorService
import com.example.run_app_rma.ui.theme.Run_app_RMATheme
import com.example.run_app_rma.presentation.login.LoginScreen
import com.example.run_app_rma.presentation.track.RunViewModel
import com.example.run_app_rma.presentation.main.MainScreenWithTabs
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.example.run_app_rma.presentation.profile.EditProfileScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage


class MainActivity : ComponentActivity() {

    private lateinit var locationService: LocationService
    private lateinit var sensorService: SensorService
    private lateinit var appDatabase: AppDatabase
    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: UserRepository
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseStorage: FirebaseStorage

    // Use a single ActivityResultLauncher for multiple permissions
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        val readExternalStorageGranted = permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        val readMediaImagesGranted = permissions[android.Manifest.permission.READ_MEDIA_IMAGES] ?: false // For Android 13+

        if (fineLocationGranted && coarseLocationGranted) {
            Toast.makeText(this, "Location permissions granted.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Location permissions denied.", Toast.LENGTH_SHORT).show()
        }

        if (cameraGranted) {
            Toast.makeText(this, "Camera permission granted.", Toast.LENGTH_SHORT).show()
        }
        // Check for relevant storage permissions based on Android version
        if (readExternalStorageGranted || readMediaImagesGranted) {
            Toast.makeText(this, "Read storage permission granted.", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        locationService = LocationService(this)
        sensorService = SensorService(this)
        appDatabase = AppDatabase.getInstance(applicationContext)
        authRepository = AuthRepository(applicationContext) // CORRECTED: Pass applicationContext
        userRepository = UserRepository(FirebaseFirestore.getInstance())
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()

        // Request all necessary permissions here
        requestPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_EXTERNAL_STORAGE, // For older Android versions
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE, // For older Android versions
                android.Manifest.permission.READ_MEDIA_IMAGES // For Android 13+
            )
        )

        setContent {
            Run_app_RMATheme {
                val navController = rememberNavController()

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

                // Determine start destination based on current login status
                val isLoggedIn by remember { mutableStateOf(authRepository.isLoggedIn()) } // Observing authRepository.isLoggedIn()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (isLoggedIn) "main" else "login", // Use isLoggedIn state
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(authRepository = authRepository) {
                                // On successful login, navigate to main and update isLoggedIn state
                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                                // No explicit isLoggedIn = true needed here as NavHost observes it.
                                // If you want immediate UI update for isLoggedIn, it needs to be MutableStateOf
                                // and updated here. However, `authRepository.isLoggedIn()` directly reflects state.
                            }
                        }
                        composable("main") {
                            // Pass all necessary dependencies to MainScreenWithTabs
                            MainScreenWithTabs(
                                runViewModel = runViewModel,
                                userRepository = userRepository,
                                firebaseAuth = firebaseAuth,
                                authRepository = authRepository, // Correctly pass the instance
                                onLogout = {
                                    authRepository.logout() // Call logout on the repository
                                    navController.navigate("login") {
                                        popUpTo("main") { inclusive = true }
                                    }
                                },
                                onEditProfile = { userId ->
                                    navController.navigate("edit_profile/$userId")
                                }
                            )
                        }
                        composable("edit_profile/{userId}") { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId")
                            if (userId != null) {
                                EditProfileScreen(
                                    userId = userId,
                                    onProfileUpdated = {
                                        // When profile is updated, navigate back.
                                        // MainScreenWithTabs will handle refreshing the profile data
                                        // when it becomes visible again.
                                        navController.popBackStack()
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            } else {
                                Toast.makeText(this@MainActivity, "User ID missing for edit profile.", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
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