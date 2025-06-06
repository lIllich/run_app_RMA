package com.example.run_app_rma.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager // Import for PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat // Import for ContextCompat
import com.example.run_app_rma.MainActivity // Import your MainActivity
import com.example.run_app_rma.R // Import your R file for resources (e.g., app icon)
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.random.Random // Import Random for unique notification IDs

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"
    private val userRepository = UserRepository(FirebaseFirestore.getInstance())
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val serviceScope = CoroutineScope(SupervisorJob()) // Use SupervisorJob for the scope

    // Notification Channel ID (unique for your app)
    private val CHANNEL_ID = "run_app_notifications"
    private val CHANNEL_NAME = "Run App Notifications"
    private val CHANNEL_DESCRIPTION = "Notifications for Run App activities"

    /**
     * Called when a new FCM registration token is generated or updated.
     * This happens in scenarios like:
     * - The app is installed on a new device.
     * - The user uninstalls/reinstalls the app.
     * - The user clears app data.
     * - The token expires.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // Send the token to your app server (Firestore in this case)
        // You should associate this token with the currently logged-in user.
        sendRegistrationTokenToFirestore(token)
    }

    /**
     * Called when an FCM message is received.
     * Messages are received here regardless of whether the app is in the foreground, background, or killed,
     * but how they are handled depends on the message type (notification vs. data) and app state.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            // Handle data messages. These messages are typically processed here.
            // Example: update UI, perform background tasks.
            // You'll parse the data to determine notification type and display accordingly.

            val notificationType = remoteMessage.data["type"]
            val postId = remoteMessage.data["postId"]
            val userId = remoteMessage.data["userId"]
            val followerId = remoteMessage.data["followerId"] // For new_follower type

            // Determine title and body from the data payload
            val title = remoteMessage.notification?.title ?: "Run App Notification"
            val body = remoteMessage.notification?.body ?: "You have a new activity."

            // Manually display notification if the app is in the foreground
            // If the app is in the background/killed, FCM typically handles this.
            if (isAppInForeground()) { // You might need a more robust check for foreground state
                // Build and display the notification
                sendNotification(title, body, notificationType, postId, userId, followerId)
            } else {
                // If in background/killed, FCM system tray handles it, but we can still
                // log for debugging or custom handling if default notification is not enough.
                Log.d(TAG, "App is in background/killed. FCM will display notification.")
            }
        }

        // Check if message contains a notification payload.
        // If a notification payload is present, FCM generally handles displaying it
        // when the app is in the background/killed. If the app is in the foreground,
        // onMessageReceived is called, and you'll need to manually display it
        // if you want it to appear. We already handle this through the data payload
        // processing above for consistency.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }
    }

    /**
     * Sends the FCM registration token to Firestore, associating it with the current user.
     * This ensures that Cloud Functions can retrieve the correct token to send notifications.
     */
    private fun sendRegistrationTokenToFirestore(token: String) {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId == null) {
            Log.w(TAG, "User not logged in, cannot save FCM token to Firestore.")
            return
        }

        serviceScope.launch {
            val updates = mapOf("fcmToken" to token)
            val result = userRepository.updateUserProfile(currentUserId, updates)
            result.onSuccess {
                Log.d(TAG, "FCM token successfully saved to Firestore for user $currentUserId.")
            }.onFailure { e ->
                Log.e(TAG, "Failed to save FCM token to Firestore for user $currentUserId: ${e.message}", e)
            }
        }
    }

    /**
     * Creates and displays a notification.
     * @param title The title of the notification.
     * @param body The body text of the notification.
     * @param type The type of notification (e.g., "like", "comment", "new_follower", "comment_deleted").
     * @param postId The ID of the post related to the notification (optional).
     * @param userId The ID of the user related to the notification (e.g., follower ID, liker ID, commenter ID) (optional).
     * @param followerId The ID of the follower (redundant with userId for follow, but kept for clarity).
     */
    private fun sendNotification(
        title: String,
        body: String,
        type: String?,
        postId: String?,
        userId: String?,
        followerId: String? // Can be redundant with userId for new_follower
    ) {
        // Create an Intent for the MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // Clear activity stack
            // Pass data as extras to the intent
            putExtra("type", type)
            Log.d(TAG, "sendNotification: Putting extra 'type': $type")
            if (postId != null) {
                putExtra("postId", postId)
                Log.d(TAG, "sendNotification: Putting extra 'postId': $postId")
            }
            // Use 'userId' for navigation, as MainActivity expects this
            // For 'new_follower', the followerId is the relevant userId
            // For 'comment_deleted', 'like', 'comment', no direct userId is needed for navigation itself
            // but the Cloud Function might pass one if it wants to identify the source.
            // Ensure consistency between Cloud Function 'data' keys and Android 'putExtra' keys.
            if (userId != null) {
                putExtra("userId", userId) // Used for new_follower navigation
                Log.d(TAG, "sendNotification: Putting extra 'userId': $userId")
            }
            // Log all extras just to be super sure
            Log.d(TAG, "sendNotification: Intent extras: ${extras?.keySet()?.joinToString(", ")}")
        }

        // Create a PendingIntent to be triggered when the user taps the notification
        val pendingIntent = PendingIntent.getActivity(
            this,
            Random.nextInt(), // Use a unique request code for each notification
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE // FLAG_IMMUTABLE is required for Android 12+
        )

        // Create a Notification Channel (required for Android 8.0 and above)
        createNotificationChannel()

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Set your app's small icon
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true) // Automatically dismiss the notification when tapped
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)) // Default notification sound
            .setContentIntent(pendingIntent) // Set the intent to launch when notification is tapped
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Set priority for heads-up notification

        // Show the notification with permission check for Android 13+
        with(NotificationManagerCompat.from(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this@MyFirebaseMessagingService, // Use 'this@MyFirebaseMessagingService' for context
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notify(Random.nextInt(), notificationBuilder.build()) // Use a unique ID for each notification
                    Log.d(TAG, "Notification displayed successfully (API 33+).")
                } else {
                    Log.w(TAG, "Notification permission denied. Cannot display notification (API 33+).")
                    // You might want to queue the notification or inform the user
                    // that notifications are disabled.
                }
            } else {
                // For APIs < 33, POST_NOTIFICATIONS permission is not required at runtime
                // (it's granted at install time).
                notify(Random.nextInt(), notificationBuilder.build())
                Log.d(TAG, "Notification displayed successfully (API < 33).")
            }
        }
    }

    /**
     * Creates a NotificationChannel for Android 8.0 (Oreo) and above.
     */
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // Importance level
            ).apply {
                description = CHANNEL_DESCRIPTION
                // Enable vibration and sound by default for this channel
                enableVibration(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Placeholder for checking if the app is in the foreground.
     * A more robust solution might involve tracking activity lifecycle or using a broadcast receiver.
     */
    private fun isAppInForeground(): Boolean {
        // This is a simplified check. For a production app, you might
        // implement a more sophisticated way to track foreground status.
        // For debugging, we can assume it's foreground if onMessageReceived is called
        // for notification payloads that FCM *would* display in background.
        // Or you can return true to always show notification if message has data payload
        // and you want to manage it yourself.
        return true // For testing, always assume foreground to see manual notifications
    }
}
