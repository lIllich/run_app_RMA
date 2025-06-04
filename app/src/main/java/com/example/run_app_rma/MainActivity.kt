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
import com.example.run_app_rma.data.firestore.repository.FollowRepository
import com.example.run_app_rma.data.firestore.repository.RunPostRepository
import com.example.run_app_rma.presentation.feed.FeedViewModel
import com.example.run_app_rma.presentation.follow.FollowViewModel
import com.example.run_app_rma.presentation.profile.EditProfileScreen
import com.example.run_app_rma.presentation.profile.EditProfileViewModel
import com.example.run_app_rma.presentation.profile.ProfileScreen
import com.example.run_app_rma.presentation.profile.UserPostsScreen
import com.example.run_app_rma.presentation.profile.UserPostsViewModel
import com.example.run_app_rma.presentation.profile.UserListScreen
import com.example.run_app_rma.presentation.profile.UserListViewModel
import com.example.run_app_rma.presentation.profile.OtherUserProfileScreen
import com.example.run_app_rma.presentation.profile.OtherUserProfileViewModel
import com.example.run_app_rma.presentation.publish.PublishRunViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage


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
                android.Manifest.permission.READ_MEDIA_IMAGES
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
                                onUserClick = { clickedUserId -> // Pass the onUserClick lambda here
                                    navController.navigate("other_user_profile_screen/$clickedUserId")
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
                                    onUserClick = { clickedUserId -> // Pass onUserClick to UserPostsScreen
                                        navController.navigate("other_user_profile_screen/$clickedUserId")
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
                                    onUserClick = { clickedUserId -> // Pass onUserClick to OtherUserProfileScreen
                                        navController.navigate("other_user_profile_screen/$clickedUserId")
                                    }
                                )
                            } else {
                                Toast.makeText(this@MainActivity, "User ID missing for other user profile.", Toast.LENGTH_SHORT).show()
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
