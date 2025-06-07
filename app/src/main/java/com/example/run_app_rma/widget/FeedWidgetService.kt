package com.example.run_app_rma.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.run_app_rma.R
import com.example.run_app_rma.data.firestore.model.RunPost
import com.example.run_app_rma.data.firestore.repository.FollowRepository
import com.example.run_app_rma.data.firestore.repository.RunPostRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class FeedWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return FeedWidgetItemFactory(applicationContext, intent)
    }
}

class FeedWidgetItemFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {
    private val runPostRepository = RunPostRepository(FirebaseFirestore.getInstance())
    private val userRepository = UserRepository(FirebaseFirestore.getInstance())
    private val followRepository = FollowRepository(FirebaseFirestore.getInstance())
    private val firebaseAuth = FirebaseAuth.getInstance()
    private var posts = listOf<RunPost>()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    override fun onCreate() {
        // Initialize data
    }

    override fun onDataSetChanged() {
        runBlocking {
            try {
                val currentUserId = firebaseAuth.currentUser?.uid
                if (currentUserId != null) {
                    val followingIdsResult = followRepository.getFollowingIds(currentUserId)
                    if (followingIdsResult.isSuccess) {
                        val followingIds = followingIdsResult.getOrNull() ?: emptyList()
                        if (followingIds.isNotEmpty()) {
                            val allPostsFromFollowedUsers = mutableListOf<RunPost>()
                            followingIds.forEach { id ->
                                runPostRepository.getPostsForUser(id, 6).getOrNull()?.let {
                                    allPostsFromFollowedUsers.addAll(it)
                                }
                            }
                            posts = allPostsFromFollowedUsers.sortedByDescending { it.timestamp }.take(6)
                        } else {
                            posts = emptyList()
                        }
                    } else {
                        posts = emptyList()
                    }
                } else {
                    posts = emptyList()
                }
            } catch (_: Exception) {
                posts = emptyList()
            }
        }
    }

    override fun onDestroy() {
        // Clean up
    }

    override fun getCount(): Int = posts.size

    override fun getViewAt(position: Int): RemoteViews {
        val post = posts[position]
        val views = RemoteViews(context.packageName, R.layout.widget_feed_item)

        runBlocking {
            val userResult = userRepository.getUserProfile(post.userId)
            if (userResult.isSuccess) {
                val user = userResult.getOrNull()
                val durationMillis = post.endTime - post.startTime
                val durationText = if (durationMillis < TimeUnit.HOURS.toMillis(1)) {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) - TimeUnit.MINUTES.toSeconds(minutes)
                    if (minutes == 0L) {
                        "${seconds}s"
                    } else {
                        "${minutes}m ${seconds}s"
                    }
                } else {
                    val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) - TimeUnit.HOURS.toMinutes(hours)
                    String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
                }

                // Format distance to 2 decimal places
                val distanceKm = String.format(Locale.getDefault(), "%.2f", post.distance / 1000.0)
                
                val postText = "${user?.displayName ?: "User"}: ${distanceKm}km in $durationText"
                val postDate = post.timestamp?.let { dateFormat.format(it) } ?: ""

                views.setTextViewText(R.id.widget_post_text, postText)
                views.setTextViewText(R.id.widget_post_date, postDate)
            }
        }

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
} 