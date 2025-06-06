package com.example.run_app_rma.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.run_app_rma.domain.model.LocationDataEntity

@Dao
interface LocationDao {
    @Insert
    suspend fun insertLocationData(data: LocationDataEntity)

    // get location data for a specific run (TODO: remove from RunDao)
    @Query("SELECT * FROM location_data WHERE runId = :runId ORDER BY timestamp ASC")
    suspend fun getLocationDataForRun(runId: Long): List<LocationDataEntity>
}