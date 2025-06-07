package com.example.run_app_rma.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sensor_data",
    foreignKeys = [ForeignKey(
        entity = RunEntity::class,
        parentColumns = ["id"],
        childColumns = ["runId"],
        onDelete = ForeignKey.Companion.CASCADE
    )],
    indices = [Index("runId")]
)
data class SensorDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: Long,    // foreign key to RunEntity
    val timestamp: Long,
    val sensorType: SensorType,
    val stepCount: Float
)
