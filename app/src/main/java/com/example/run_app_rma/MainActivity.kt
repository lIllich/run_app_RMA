package com.example.run_app_rma

import android.content.Intent // Import Intent
import android.os.Build // Import Build for SDK version checks
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
import com.example.run_app_rma.presentation.search.SearchUserViewModel // Renamed from FollowViewModel
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
import android.util.Log // Import Log for debugging


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

    private val TAG = "MainActivity" // Tag for logging

    // Launcher for general permissions (location, camera, storage)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        val readExternalStorageGranted = permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        val writeExternalStorageGranted = permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
        val readMediaImagesGranted = permissions[android.Manifest.permission.READ_MEDIA_IMAGES] ?: false

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
    }

    // New: Launcher for POST_NOTIFICATIONS permission (for Android 13+)
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
        sensorService = SensorService(this)
        appDatabase = AppDatabase.getInstance(applicationContext)
        authRepository = AuthRepository(applicationContext)
        userRepository = UserRepository(FirebaseFirestore.getInstance())
        followRepository = FollowRepository(FirebaseFirestore.getInstance())
        runPostRepository = RunPostRepository(FirebaseFirestore.getInstance())
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()

        // Request general permissions at startup
        requestPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.CAMERA,
                // These storage permissions are deprecated in newer Android versions
                // but still needed for compatibility with older APIs or until a full
                // migration to MediaStore API for all file access.
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                // Specific for Android 13+ for image access
                android.Manifest.permission.READ_MEDIA_IMAGES
            )
        )

        // New: Request POST_NOTIFICATIONS permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        }

        setContent {
            Run_app_RMATheme {
                val navController = rememberNavController()

                val isLoggedIn by remember { mutableStateOf(authRepository.isLoggedIn()) }

                // New: Observe the current intent for notification handling
                val currentIntent = rememberUpdatedState(intent)

                // New: LaunchedEffect to handle initial intent when component is created
                // and subsequent new intents when activity is already running (e.g., singleTop launch mode)
                LaunchedEffect(currentIntent.value) {
                    currentIntent.value?.let { incomingIntent ->
                        handleNotificationIntent(navController, incomingIntent)
                        // It's crucial to clear the intent's data after handling
                        // to prevent it from being re-processed on subsequent recreations
                        // or other scenarios that might re-deliver the same intent.
                        // However, directly modifying 'intent' is not allowed here as it's
                        // an immutable property within onCreate.
                        // A common pattern is to set the intent to null or a new empty intent
                        // after processing it in a separate lifecycle method if the activity
                        // has singleTop launch mode or similar behavior.
                        // For simplicity in Compose, we ensure it's only processed once per intent value.
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
                                },
                                onPostClick = { postId -> // Pass onPostClick to navigate to RunPostScreen
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
                                    onUserClick = { clickedUserId -> // Pass onUserClick to UserPostsScreen
                                        navController.navigate("other_user_profile_screen/$clickedUserId")
                                    },
                                    onPostClick = { postId -> // Pass onPostClick to UserPostsScreen
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
                                    onUserClick = { clickedUserId -> // Pass onUserClick to OtherUserProfileScreen
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
                                    },
                                    onPostDeleted = {
                                        navController.popBackStack() // Navigate back when the post is deleted
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

    // New: Helper function to handle intents when MainActivity is launched or receives a new intent
    private fun handleNotificationIntent(navController: NavController, intent: Intent?) {
        intent?.let { incomingIntent ->
            Log.d(TAG, "handleNotificationIntent: Received intent. Action: ${incomingIntent.action}, Data: ${incomingIntent.dataString}") // ADDED LOG
            Log.d(TAG, "handleNotificationIntent: Intent extras: ${incomingIntent.extras?.keySet()?.joinToString(", ")}") // ADDED LOG

            // Check if this intent came from a notification tap
            if (incomingIntent.hasExtra("type")) {
                val notificationType = incomingIntent.getStringExtra("type")
                val postId = incomingIntent.getStringExtra("postId")
                val userId = incomingIntent.getStringExtra("userId") // For follower notifications

                Log.d(TAG, "handleNotificationIntent: Notification Type: $notificationType, Post ID: $postId, User ID: $userId") // ADDED LOG

                when (notificationType) {
                    "like", "comment" -> {
                        if (postId != null) {
                            Log.d(TAG, "Navigating to run_post_screen/$postId for $notificationType notification. Current destination: ${navController.currentDestination?.route}")
                            navController.navigate("run_post_screen/$postId") {
                                // Clear back stack to make this the root of the notification flow
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true // Ensure only one instance of the destination
                            }
                        } else {
                            Toast.makeText(this, "Notification error: Post ID missing.", Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "Notification error: Post ID missing for $notificationType notification.") // ADDED LOG
                        }
                    }
                    "new_follower" -> {
                        if (userId != null) {
                            Log.d(TAG, "Navigating to other_user_profile_screen/$userId for new_follower notification. Current destination: ${navController.currentDestination?.route}")
                            navController.navigate("other_user_profile_screen/$userId") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            Toast.makeText(this, "Notification error: User ID missing.", Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "Notification error: User ID missing for new_follower notification.") // ADDED LOG
                        }
                    }
                    "comment_deleted" -> { // Handle comment_deleted notification
                        if (postId != null) {
                            Log.d(TAG, "Navigating to run_post_screen/$postId for comment_deleted notification. Current destination: ${navController.currentDestination?.route}")
                            navController.navigate("run_post_screen/$postId") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            Toast.makeText(this, "Notification error: Post ID missing for deleted comment.", Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "Notification error: Post ID missing for deleted comment notification.") // ADDED LOG
                        }
                    }
                    else -> { // Log unexpected notification types
                        Log.w(TAG, "Unhandled notification type: $notificationType") // ADDED LOG
                    }
                }
            } else {
                Log.d(TAG, "handleNotificationIntent: Intent does not have 'type' extra. Not a handled notification intent.") // ADDED LOG
            }
        } ?: Log.d(TAG, "handleNotificationIntent: Received null intent.") // ADDED LOG
    }

    // New: Override onNewIntent to handle cases where Activity is already running (e.g., singleTop launch mode)
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
