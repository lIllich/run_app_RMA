package com.example.run_app_rma.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.run_app_rma.data.dao.LocationDao
import com.example.run_app_rma.data.dao.RunDao
import com.example.run_app_rma.data.dao.SensorDao
import com.example.run_app_rma.data.dao.ChallengeDao
import com.example.run_app_rma.domain.model.LocationDataEntity
import com.example.run_app_rma.domain.model.RunEntity
import com.example.run_app_rma.domain.model.SensorDataEntity
import com.example.run_app_rma.domain.model.UserChallengeProgress

@Database(
    entities = [RunEntity::class, SensorDataEntity::class, LocationDataEntity::class, UserChallengeProgress::class],
    version = 4
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun runDao(): RunDao
    abstract fun sensorDao(): SensorDao
    abstract fun locationDao(): LocationDao
    abstract fun challengeDao(): ChallengeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "run_app_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}