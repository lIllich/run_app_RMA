package com.example.run_app_rma.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val distance: Float?,   // meters
    val avgPace: Float?,    // min / km
    val steps: Int?
)
