package com.example.run_app_rma.data.firestore.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Follow(
    @DocumentId val id: String = "",
    val followerId: String = "",
    val followingId: String = "",
    @ServerTimestamp val timestamp: Date? = null
)
