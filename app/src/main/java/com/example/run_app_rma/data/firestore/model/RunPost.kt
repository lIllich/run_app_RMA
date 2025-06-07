package com.example.run_app_rma.data.firestore.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class RunPost(
    @DocumentId val id: String = "",    // firebase document id for the post
    val userId: String = "",
    val localRunId: Long? = null,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val distance: Float = 0f,   // m
    val avgPace: Float = 0f,    // min/km
    val pathPoints: List<LocationPoint> = emptyList(),  // custom class -> includes elevation
    val caption: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    @ServerTimestamp val timestamp: Date? = null    // firestore timestamp
)

data class LocationPoint(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val elevation: Double = 0.0     // in meters
)