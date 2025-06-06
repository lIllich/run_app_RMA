package com.example.run_app_rma.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.run_app_rma.domain.model.SensorDataEntity

@Dao
interface SensorDao {
    @Insert suspend fun insertSensorData(data: SensorDataEntity)

    // returns max step count recorded for that run
    @Query("SELECT MAX(steps) FROM sensor_data WHERE runId = :runId")
    suspend fun getStepCountForRun(runId: Long): Int?
}