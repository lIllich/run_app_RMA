package com.example.run_app_rma.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenge_progress")
data class UserChallengeProgress(
    @PrimaryKey val challengeId: String,
    val currentLevel: Int = 0,
    // For cumulative challenges, this is the total progress (e.g., total distance).
    // For "best of" challenges, this is the best value achieved (e.g., longest run, fastest 5k).
    val value: Float = 0f,
    val unlockedTitle: String? = null
) 