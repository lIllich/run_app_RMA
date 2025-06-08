package com.example.run_app_rma.data.firestore.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    val id: String = "",
    val displayName: String = "",
    val lowercaseDisplayName: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val totalDistanceRun: Float = 0f,
    val totalRuns: Int = 0,
    val age: Int? = null,
    val lastRunTimestamp: Long? = null,
    val fcmToken: String? = null,
    @ServerTimestamp val createdAt: Date? = null,
    // New fields for weekly goals
    val weeklyGoalSteps: Int? = null,
    val weeklyGoalDuration: Long? = null, // in milliseconds
    val weeklyGoalDistance: Float? = null, // in meters
    val weeklyGoalsSetTimestamp: Long? = null // Timestamp when goals were last set
)