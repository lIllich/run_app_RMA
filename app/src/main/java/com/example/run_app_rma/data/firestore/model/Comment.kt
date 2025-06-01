package com.example.run_app_rma.data.firestore.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Comment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val text: String = "",
    @ServerTimestamp val timestamp: Date? = null
)
