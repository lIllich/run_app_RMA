package com.example.run_app_rma.data.dao

import androidx.room.Dao
import androidx.room.Insert
import com.example.run_app_rma.domain.model.SensorDataEntity

@Dao
interface SensorDao {
    @Insert suspend fun insertSensorData(data: SensorDataEntity)
}