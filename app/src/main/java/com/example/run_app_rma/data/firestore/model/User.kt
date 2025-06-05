package com.example.run_app_rma.data.firestore.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    // Removed @DocumentId annotation
    val id: String = "",    // firebase document id for the user, explicitly stored as a field
    val displayName: String = "",
    val lowercaseDisplayName: String = "", // Add this new field
    val email: String = "",
    val profileImageUrl: String? = null,
    val totalDistanceRun: Float = 0f,
    val totalRuns: Int = 0,
    val age: Int? = null,
    val lastRunTimestamp: Long? = null,
    @ServerTimestamp val createdAt: Date? = null
)
