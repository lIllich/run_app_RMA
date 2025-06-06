// MainActivity.kt
package com.example.run_app_rma

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.run_app_rma.data.db.AppDatabase
import com.example.run_app_rma.data.remote.AuthRepository
import com.example.run_app_rma.sensor.tracking.LocationService
import com.example.run_app_rma.sensor.tracking.SensorService
import com.example.run_app_rma.ui.theme.Run_app_RMATheme
import com.example.run_app_rma.presentation.login.LoginScreen
import com.example.run_app_rma.presentation.track.RunViewModel
import com.example.run_app_rma.presentation.main.MainScreenWithTabs
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.example.run_app_rma.data.firestore.repository.FollowRepository
import com.example.run_app_rma.data.firestore.repository.RunPostRepository
import com.example.run_app_rma.presentation.runpost.RunPostScreen
import com.example.run_app_rma.presentation.runpost.RunPostViewModel
import com.example.run_app_rma.presentation.profile.EditProfileScreen
import com.example.run_app_rma.presentation.profile.EditProfileViewModel
import com.example.run_app_rma.presentation.profile.UserPostsScreen
import com.example.run_app_rma.presentation.profile.UserPostsViewModel
import com.example.run_app_rma.presentation.profile.UserListScreen
import com.example.run_app_rma.presentation.profile.UserListViewModel
import com.example.run_app_rma.presentation.profile.OtherUserProfileScreen
import com.example.run_app_rma.presentation.profile.OtherUserProfileViewModel
import com.example.run_app_rma.presentation.publish.RunDetailsScreen
import com.example.run_app_rma.presentation.publish.RunDetailsViewModel // Import RunDetailsViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState


class MainActivity : ComponentActivity() {

    private lateinit var locationService: LocationService
    private lateinit var sensorService: SensorService
    private lateinit var appDatabase: AppDatabase
    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: UserRepository
    private lateinit var followRepository: FollowRepository
    private lateinit var runPostRepository: RunPostRepository
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseStorage: FirebaseStorage

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        val readExternalStorageGranted = permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        val readMediaImagesGranted = permissions[android.Manifest.permission.READ_MEDIA_IMAGES] ?: false
        val activityRecognitionGranted = permissions[android.Manifest.permission.ACTIVITY_RECOGNITION] ?: false

        if (fineLocationGranted && coarseLocationGranted) {
            Toast.makeText(this, "Location permissions granted.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Location permissions denied.", Toast.LENGTH_SHORT).show()
        }
        if (cameraGranted) {
            Toast.makeText(this, "Camera permission granted.", Toast.LENGTH_SHORT).show()
        }
        if (readExternalStorageGranted || readMediaImagesGranted) {
            Toast.makeText(this, "Read storage permission granted.", Toast.LENGTH_SHORT).show()
        }
        if (activityRecognitionGranted) {
            Toast.makeText(this, "Activity recognition permission granted.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Activity recognition permission denied.", Toast.LENGTH_SHORT).show()
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
        followRepository = FollowRepository(FirebaseFirestore.getInstance())
        runPostRepository = RunPostRepository(FirebaseFirestore.getInstance())
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()

        requestPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.ACTIVITY_RECOGNITION
            )
        )

        setContent {
            Run_app_RMATheme {
                val navController = rememberNavController()

                val isLoggedIn by remember { mutableStateOf(authRepository.isLoggedIn()) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (isLoggedIn) "main_screen" else "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(authRepository = authRepository) {
                                navController.navigate("main_screen") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        }
                        composable("main_screen") {
                            val runViewModel: RunViewModel = viewModel(
                                factory = RunViewModel.Factory(
                                    appDatabase.runDao(),
                                    appDatabase.locationDao(),
                                    appDatabase.sensorDao(),
                                    locationService,
                                    sensorService
                                )
                            )

                            MainScreenWithTabs(
                                runViewModel = runViewModel,
                                userRepository = userRepository,
                                firebaseAuth = firebaseAuth,
                                authRepository = authRepository,
                                runPostRepository = runPostRepository,
                                appDatabase = appDatabase,
                                onLogout = {
                                    authRepository.logout()
                                    navController.navigate("login") {
                                        popUpTo("main_screen") { inclusive = true }
                                    }
                                },
                                onEditProfile = { userId ->
                                    navController.navigate("edit_profile/$userId")
                                },
                                onViewUserPosts = { userId ->
                                    navController.navigate("user_posts_screen/$userId")
                                },
                                onViewFollowing = { userId ->
                                    navController.navigate("user_list_screen/following/$userId")
                                },
                                onViewFollowers = { userId ->
                                    navController.navigate("user_list_screen/followers/$userId")
                                },
                                onUserClick = { clickedUserId ->
                                    navController.navigate("other_user_profile_screen/$clickedUserId")
                                },
                                onPostClick = { postId ->
                                    navController.navigate("run_post_screen/$postId")
                                },
                                onRunClick = { runId ->
                                    navController.navigate("run_details_screen/$runId")
                                }
                            )
                        }
                        composable("edit_profile/{userId}") { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId")
                            if (userId != null) {
                                val editProfileViewModel: EditProfileViewModel = viewModel(
                                    factory = EditProfileViewModel.Factory(
                                        userRepository = userRepository,
                                        firebaseAuth = firebaseAuth,
                                        firebaseStorage = firebaseStorage
                                    )
                                )
                                EditProfileScreen(
                                    userId = userId,
                                    editProfileViewModel = editProfileViewModel,
                                    onProfileUpdated = {
                                        navController.popBackStack()
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            } else {
                                Toast.makeText(this@MainActivity, "User ID missing for edit profile.", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        }
                        composable("user_posts_screen/{userId}") { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId")
                            if (userId != null) {
                                UserPostsScreen(
                                    userPostsViewModel = viewModel(
                                        factory = UserPostsViewModel.Factory
                                    ),
                                    onBack = { navController.popBackStack() },
                                    onUserClick = { clickedUserId ->
                                        navController.navigate("other_user_profile_screen/$clickedUserId")
                                    },
                                    onPostClick = { postId ->
                                        navController.navigate("run_post_screen/$postId")
                                    }
                                )
                            } else {
                                Toast.makeText(this@MainActivity, "User ID missing for posts.", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        }
                        composable("user_list_screen/{listType}/{userId}") { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId")
                            val listType = backStackEntry.arguments?.getString("listType")
                            if (userId != null && listType != null) {
                                UserListScreen(
                                    userListViewModel = viewModel(
                                        factory = UserListViewModel.Factory
                                    ),
                                    onBack = { navController.popBackStack() },
                                    onUserClick = { clickedUserId ->
                                        navController.navigate("other_user_profile_screen/$clickedUserId")
                                    }
                                )
                            } else {
                                Toast.makeText(this@MainActivity, "User ID or list type missing.", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        }
                        composable("other_user_profile_screen/{userId}") { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId")
                            if (userId != null) {
                                OtherUserProfileScreen(
                                    otherUserProfileViewModel = viewModel(
                                        factory = OtherUserProfileViewModel.Factory
                                    ),
                                    onBack = { navController.popBackStack() },
                                    onEditProfile = { currentUserId ->
                                        navController.navigate("edit_profile/$currentUserId")
                                    },
                                    onLogout = {
                                        authRepository.logout()
                                        navController.navigate("login") {
                                            popUpTo("main_screen") { inclusive = true }
                                        }
                                    },
                                    onViewUserPosts = { viewedUserId ->
                                        navController.navigate("user_posts_screen/$viewedUserId")
                                    },
                                    onViewFollowing = { viewedUserId ->
                                        navController.navigate("user_list_screen/following/$viewedUserId")
                                    },
                                    onViewFollowers = { viewedUserId ->
                                        navController.navigate("user_list_screen/followers/$viewedUserId")
                                    },
                                    onUserClick = { clickedUserId ->
                                        navController.navigate("other_user_profile_screen/$clickedUserId")
                                    }
                                )
                            } else {
                                Toast.makeText(this@MainActivity, "User ID missing for other user profile.", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        }
                        composable("run_post_screen/{postId}") { backStackEntry ->
                            val postId = backStackEntry.arguments?.getString("postId")
                            if (postId != null) {
                                RunPostScreen(
                                    runPostViewModel = viewModel(
                                        factory = RunPostViewModel.Factory
                                    ),
                                    onBack = { navController.popBackStack() },
                                    onUserClick = { clickedUserId ->
                                        navController.navigate("other_user_profile_screen/$clickedUserId")
                                    },
                                    onViewLikedUsers = { postId, listType ->
                                        navController.navigate("user_list_screen/$listType/$postId")
                                    },
                                    onViewComments = { postId ->
                                        // TODO: Implement navigation to comments screen
                                        Toast.makeText(this@MainActivity, "Comments for post $postId", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                Toast.makeText(this@MainActivity, "Post ID missing.", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        }
                        composable("run_details_screen/{runId}") { backStackEntry ->
                            val runId = backStackEntry.arguments?.getString("runId")?.toLongOrNull()
                            if (runId != null) {
                                RunDetailsScreen(
                                    runId = runId,
                                    appDatabase = appDatabase,
                                    runPostRepository = runPostRepository,
                                    userRepository = userRepository,
                                    firebaseAuth = firebaseAuth,
                                    modifier = Modifier,
                                    onViewMapClick = { id ->
                                        // navigate to the new fullscreen map screen
                                        navController.navigate("fullscreen_map_screen/$id")
                                    }
                                )
                            } else {
                                Toast.makeText(this@MainActivity, "Run ID missing.", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        }
                        composable("fullscreen_map_screen/{runId}",
                            arguments = listOf(navArgument("runId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val runId = backStackEntry.arguments?.getLong("runId")
                            if (runId != null) {
                                FullscreenMapScreen(
                                    runId = runId,
                                    appDatabase = appDatabase, // Pass the database instance
                                    runPostRepository = runPostRepository,
                                    userRepository = userRepository,
                                    firebaseAuth = firebaseAuth,
                                    onBackClick = { navController.popBackStack() } // Navigate back
                                )
                            } else {
                                Toast.makeText(this@MainActivity, "Run ID for map missing.", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenMapScreen(
    runId: Long,
    appDatabase: AppDatabase,
    runPostRepository: RunPostRepository,
    userRepository: UserRepository,
    firebaseAuth: FirebaseAuth,
    onBackClick: () -> Unit
) {
    // Re-use the RunDetailsViewModel to fetch location data for the given runId
    val runDetailsViewModel: RunDetailsViewModel = viewModel(
        factory = RunDetailsViewModel.Factory(
            runId = runId,
            runDao = appDatabase.runDao(), // Pass the necessary DAOs from appDatabase
            locationDao = appDatabase.locationDao(),
            runPostRepository = runPostRepository,
            userRepository = userRepository,
            firebaseAuth = firebaseAuth
        )
    )
    val locationData by runDetailsViewModel.locationData.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Karta Trčanja") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Natrag")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (locationData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Učitavanje podataka o lokaciji...")
            }
            return@Scaffold
        }

        val pathPoints = locationData.map { LatLng(it.lat, it.lon) }
        val cameraPositionState = rememberCameraPositionState {
            // Center the map on the first point, or a more appropriate initial zoom level
            position = CameraPosition.fromLatLngZoom(pathPoints.first(), 15f)
        }

        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), // Apply padding from Scaffold
            cameraPositionState = cameraPositionState,
            // Ensure map gestures are enabled
            uiSettings = MapUiSettings(zoomControlsEnabled = true, scrollGesturesEnabled = true, zoomGesturesEnabled = true)
        ) {
            Polyline(
                points = pathPoints,
                color = Color.Red,
                width = 8f
            )
            Marker(
                state = rememberMarkerState(position = pathPoints.first()),
                title = "Start"
            )
            if (pathPoints.size > 1) {
                Marker(
                    state = rememberMarkerState(position = pathPoints.last()),
                    title = "End"
                )
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