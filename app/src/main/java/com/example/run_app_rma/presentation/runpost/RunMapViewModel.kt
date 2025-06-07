package com.example.run_app_rma.presentation.runpost

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.run_app_rma.data.firestore.model.LocationPoint
import com.example.run_app_rma.data.firestore.model.RunPost
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class RunMapViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()

    fun getRunPost(postId: String): Flow<RunPost?> = callbackFlow {
        val currentUser = firebaseAuth.currentUser
        Log.d("RunMapViewModel", "Current Firebase User: ${currentUser?.uid ?: "Not authenticated"}")

        if (currentUser == null) {
            Log.w("RunMapViewModel", "User is not authenticated. Cannot fetch run post.")
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val docRef = firestore.collection("posts").document(postId)

        val listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("RunMapViewModel", "Firestore error fetching post: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                Log.d("RunMapViewModel", "Document snapshot exists for postId: $postId")
                try {
                    val post = snapshot.toObject(RunPost::class.java)

                    if (post != null && post.pathPoints.isEmpty()) {
                        val rawData = snapshot.data
                        val polylineCoordsList = rawData?.get("polylineCoords")

                        val parsedPathPoints = mutableListOf<LocationPoint>()

                        if (polylineCoordsList is List<*>) {
                            for (item in polylineCoordsList) {
                                if (item is List<*>) {
                                    val lat = (item.getOrNull(0) as? Number)?.toDouble()
                                    val lon = (item.getOrNull(1) as? Number)?.toDouble()
                                    if (lat != null && lon != null) {
                                        parsedPathPoints.add(LocationPoint(latitude = lat, longitude = lon, elevation = 0.0))
                                    }
                                }
                            }
                        }

                        if (parsedPathPoints.isNotEmpty()) {
                            Log.d("RunMapViewModel", "Parsed ${parsedPathPoints.size} points from polylineCoords")
                            trySend(post.copy(pathPoints = parsedPathPoints))
                        } else {
                            Log.d("RunMapViewModel", "Post has no path data (neither pathPoints nor polylineCoords)")
                            trySend(post)
                        }
                    } else {
                        Log.d("RunMapViewModel", "Post has pathPoints data directly or is null")
                        trySend(post)
                    }

                } catch (e: Exception) {
                    Log.e("RunMapViewModel", "Error processing RunPost snapshot: ${e.message}", e)
                    close(e)
                }
            } else {
                Log.d("RunMapViewModel", "Document for postId: $postId does not exist or is not accessible.")
                trySend(null)
            }
        }

        awaitClose { listenerRegistration.remove() }
    }
}