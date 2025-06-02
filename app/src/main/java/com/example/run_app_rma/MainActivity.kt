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
        authRepository = AuthRepository(applicationContext)
        userRepository = UserRepository(FirebaseFirestore.getInstance())
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()

        locationPermissionRequest.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)

        setContent {
            Run_app_RMATheme {
                var isLoggedIn by remember { mutableStateOf(authRepository.isLoggedIn()) }
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

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (isLoggedIn) "main" else "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(authRepository = authRepository) {
                                isLoggedIn = true
                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        }
                        composable("main") {
                            // navController NIJE proslijeđen MainScreenWithTabs
                            MainScreenWithTabs(
                                runViewModel = runViewModel,
                                userRepository = userRepository,
                                firebaseAuth = firebaseAuth,
                                authRepository = authRepository,
                                onLogout = {
                                    isLoggedIn = false
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
                                        // NEMA postavljanja zastavice u SavedStateHandle
                                        // Prazan lambda ili uklonite 'onProfileUpdated' ako ga EditProfileScreen dopušta
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