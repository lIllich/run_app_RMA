package com.example.run_app_rma.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.run_app_rma.domain.model.LocationDataEntity
import com.example.run_app_rma.domain.model.RunEntity

@Dao
interface RunDao {
    @Insert suspend fun insert(run: RunEntity): Long
    @Update suspend fun update(run: RunEntity)

    @Query("SELECT * FROM runs")
    suspend fun getAllRuns(): List<RunEntity>

    // get location data for a specific run
    @Query("SELECT * FROM location_data WHERE runId = :runId ORDER BY timestamp ASC")
    suspend fun getLocationDataForRun(runId: Long): List<LocationDataEntity>

    // get a specific run by its ID
    @Query("SELECT * FROM runs WHERE id = :runId")
    suspend fun getRunById(runId: Long): RunEntity?

    @Query("DELETE FROM runs WHERE id = :runId")
    suspend fun deleteRunById(runId: Long)
}