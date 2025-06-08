package com.example.run_app_rma.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.run_app_rma.domain.model.UserChallengeProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: UserChallengeProgress)

    @Query("SELECT * FROM challenge_progress")
    fun getAllProgress(): Flow<List<UserChallengeProgress>>

    @Query("SELECT * FROM challenge_progress WHERE challengeId = :challengeId")
    suspend fun getProgressForChallenge(challengeId: String): UserChallengeProgress?

    @Query("SELECT * FROM challenge_progress")
    suspend fun getAllProgressList(): List<UserChallengeProgress>

    @Query("SELECT unlockedTitle FROM challenge_progress WHERE unlockedTitle IS NOT NULL")
    fun getUnlockedTitles(): Flow<List<String>>
} 