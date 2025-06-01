package com.example.run_app_rma.data.firestore.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    @DocumentId val id: String = "",    // firebase auth user id
    val displayName: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val totalDistanceRun: Float = 0f,   // m
    val totalRuns: Int = 0,
    val lastRunTimestamp: Long? = null,
    @ServerTimestamp val createdAt: Date? = null    // firestore timestamp
)
