package com.example.run_app_rma

import android.content.Intent
import android.os.Build
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
import androidx.navigation.NavController
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
import com.example.run_app_rma.presentation.runpost.RunPostScreen
import com.example.run_app_rma.presentation.runpost.RunPostViewModel
import com.example.run_app_rma.presentation.search.SearchUserViewModel
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
import android.util.Log


class MainActivity : ComponentActivity() {

    private lateinit var locationService: LocationService
    private lateinit var appDatabase: AppDatabase
    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: UserRepository
    private lateinit var followRepository: FollowRepository
    private lateinit var runPostRepository: RunPostRepository
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseStorage: FirebaseStorage

    private val TAG = "MainActivity"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        val readExternalStorageGranted = permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        val writeExternalStorageGranted = permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
        val readMediaImagesGranted = permissions[android.Manifest.permission.READ_MEDIA_IMAGES] ?: false
        val activityRecognitionGranted = permissions[android.Manifest.permission.ACTIVITY_RECOGNITION] ?: false
        val foregroundServiceGranted = permissions[android.Manifest.permission.FOREGROUND_SERVICE] ?: false

        if (fineLocationGranted && coarseLocationGranted) {
            Toast.makeText(this, "Location permissions granted.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Location permissions denied.", Toast.LENGTH_SHORT).show()
        }

        if (cameraGranted) {
            Toast.makeText(this, "Camera permission granted.", Toast.LENGTH_SHORT).show()
        }
        if (readExternalStorageGranted || writeExternalStorageGranted || readMediaImagesGranted) {
            Toast.makeText(this, "Storage permission granted.", Toast.LENGTH_SHORT).show()
        }

        if (activityRecognitionGranted) {
            Toast.makeText(this, "Activity recognition permission granted.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Activity recognition permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notification permission denied. " +
                    "You may not receive push notifications.", Toast.LENGTH_LONG).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        locationService = LocationService(this)
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
                android.Manifest.permission.ACTIVITY_RECOGNITION,
                android.Manifest.permission.FOREGROUND_SERVICE
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        }

        setContent {
            Run_app_RMATheme {
                val navController = rememberNavController()

                val isLoggedIn by remember { mutableStateOf(authRepository.isLoggedIn()) }

                // Observe the current intent for notification handling
                val currentIntent = rememberUpdatedState(intent)

                // LaunchedEffect to handle initial intent when component is created
                // and subsequent new intents when activity is already running (e.g., singleTop launch mode)
                LaunchedEffect(currentIntent.value) {
                    currentIntent.value?.let { incomingIntent ->
                        handleNotificationIntent(navController, incomingIntent)
                        // Clear the intent's data after handling to prevent re-processing.
                        // This is important for singleTop activities.
                        // Note: Only clear if you're sure you won't need to re-read the same intent later.
                        // For a notification-driven navigation, this is generally safe.
                        incomingIntent.replaceExtras(Bundle())
                        incomingIntent.data = null
                    }
                }

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
                                    application,
                                    appDatabase.runDao(),
                                    appDatabase.locationDao(),
                                    appDatabase.sensorDao(),
                                    locationService
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
                                        Toast.makeText(this@MainActivity, "Comments for post $postId", Toast.LENGTH_SHORT).show()
                                    },
                                    onPostDeleted = {
                                        navController.popBackStack()
                                    }
                                )
                            } else {
                                Toast.makeText(this@MainActivity, "Post ID missing.", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleNotificationIntent(navController: NavController, intent: Intent?) {
        intent?.let { incomingIntent ->
            Log.d(TAG, "handleNotificationIntent: Received intent. Action: ${incomingIntent.action}, Data: ${incomingIntent.dataString}")
            Log.d(TAG, "handleNotificationIntent: Intent extras: ${incomingIntent.extras?.keySet()?.joinToString(", ")}")

            if (incomingIntent.hasExtra("type")) {
                val notificationType = incomingIntent.getStringExtra("type")
                val postId = incomingIntent.getStringExtra("postId")
                val userId = incomingIntent.getStringExtra("userId")

                Log.d(TAG, "handleNotificationIntent: Notification Type: $notificationType, Post ID: $postId, User ID: $userId")

                when (notificationType) {
                    "like", "comment", "comment_deleted" -> {
                        if (postId != null) {
                            val route = "run_post_screen/$postId"
                            Log.d(TAG, "Navigating to $route for $notificationType notification.")
                            navController.navigate(route) {
                                // Keep the current screen if it's the target, otherwise navigate.
                                // This works well with singleTop launchMode.
                                launchSingleTop = true
                            }
                            Log.d(TAG, "Navigation dispatched to $route. Current destination: ${navController.currentDestination?.route}")
                        } else {
                            Toast.makeText(this, "Notification error: Post ID missing.", Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "Notification error: Post ID missing for $notificationType notification.")
                        }
                    }
                    "new_follower" -> {
                        if (userId != null) {
                            val route = "other_user_profile_screen/$userId"
                            Log.d(TAG, "Navigating to $route for new_follower notification.")
                            navController.navigate(route) {
                                // Keep the current screen if it's the target, otherwise navigate.
                                launchSingleTop = true
                            }
                            Log.d(TAG, "Navigation dispatched to $route. Current destination: ${navController.currentDestination?.route}")
                        } else {
                            Toast.makeText(this, "Notification error: User ID missing.", Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "Notification error: User ID missing for new_follower notification.")
                        }
                    }
                    else -> {
                        Log.w(TAG, "Unhandled notification type: $notificationType")
                    }
                }
            } else {
                Log.d(TAG, "handleNotificationIntent: Intent does not have 'type' extra. Not a handled notification intent.")
            }
        } ?: Log.d(TAG, "handleNotificationIntent: Received null intent.")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Set the new intent for the current activity instance.
        // This is crucial for LaunchedEffect(currentIntent.value) to react to new intents.
        setIntent(intent)
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        Run_app_RMATheme {
            Text("Hello Android!")
        }
    }
}
