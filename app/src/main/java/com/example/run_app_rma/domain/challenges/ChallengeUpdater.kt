package com.example.run_app_rma.domain.challenges

import com.example.run_app_rma.data.dao.ChallengeDao
import com.example.run_app_rma.data.dao.LocationDao
import com.example.run_app_rma.data.dao.RunDao
import com.example.run_app_rma.domain.model.Challenge
import com.example.run_app_rma.domain.model.ChallengeType
import com.example.run_app_rma.domain.model.ChallengesRepository
import com.example.run_app_rma.domain.model.RunEntity
import com.example.run_app_rma.domain.model.UserChallengeProgress

class ChallengeUpdater(
    private val challengeDao: ChallengeDao,
    private val runDao: RunDao,
    private val locationDao: LocationDao
) {
    suspend fun updateChallengesAfterRun(completedRun: RunEntity) {
        if (completedRun.distance == null || completedRun.distance < 2000f) return

        val allChallenges = ChallengesRepository.challenges
        val allProgress = challengeDao.getAllProgressList().associateBy { it.challengeId }

        allChallenges.forEach { challenge ->
            val progress = allProgress[challenge.id] ?: UserChallengeProgress(challenge.id)
            val updatedProgress = when (challenge.type) {
                ChallengeType.TOTAL_RUNS -> updateTotalRuns(progress, challenge)
                ChallengeType.LONGEST_RUN -> updateLongestRun(progress, challenge, completedRun)
                ChallengeType.TOTAL_ELEVATION_GAIN -> updateTotalElevation(progress, challenge, completedRun)
                ChallengeType.FASTEST_RUN -> updateFastestRun(progress, challenge, completedRun)
            }
            val finalProgress = checkLevelUps(updatedProgress, challenge)
            challengeDao.upsertProgress(finalProgress)
        }
    }

    private suspend fun updateTotalRuns(progress: UserChallengeProgress, challenge: Challenge): UserChallengeProgress {
        val validRuns = runDao.getAllRuns().count { (it.distance ?: 0f) >= 2000f }
        return progress.copy(value = validRuns.toFloat())
    }

    private fun updateLongestRun(progress: UserChallengeProgress, challenge: Challenge, run: RunEntity): UserChallengeProgress {
        val runDistance = run.distance ?: 0f
        return if (runDistance > progress.value) {
            progress.copy(value = runDistance)
        } else {
            progress
        }
    }

    private suspend fun updateTotalElevation(progress: UserChallengeProgress, challenge: Challenge, run: RunEntity): UserChallengeProgress {
        val locations = locationDao.getLocationDataForRun(run.id)
        var elevationGain = 0.0
        for (i in 1 until locations.size) {
            val diff = locations[i].alt - locations[i - 1].alt
            if (diff > 0) {
                elevationGain += diff
            }
        }
        val allRuns = runDao.getAllRuns().filter{ it.id != run.id }
        var totalElevation = elevationGain
        // This is not efficient, but for local-only, it's a start.
        // A better approach would be to store elevation gain per run.
        for(r in allRuns){
            val locs = locationDao.getLocationDataForRun(r.id)
            for (i in 1 until locs.size) {
                val diff = locs[i].alt - locs[i - 1].alt
                if (diff > 0) {
                    totalElevation += diff
                }
            }
        }
        return progress.copy(value = totalElevation.toFloat())
    }

    private suspend fun updateFastestRun(progress: UserChallengeProgress, challenge: Challenge, run: RunEntity): UserChallengeProgress {
        val runDistance = run.distance ?: 0f
        if (runDistance < 5000f) return progress

        val durationMinutes = (run.endTime!! - run.startTime) / 60000f
        if (progress.value == 0f || durationMinutes < progress.value) {
            return progress.copy(value = durationMinutes)
        }
        return progress
    }

    private fun checkLevelUps(progress: UserChallengeProgress, challenge: Challenge): UserChallengeProgress {
        var newLevel = progress.currentLevel
        var unlockedTitle: String? = progress.unlockedTitle

        while (newLevel < challenge.levels.size) {
            val goalForNextLevel = challenge.levels[newLevel]
            val levelCompleted = if (challenge.type == ChallengeType.FASTEST_RUN) {
                progress.value in 0.1f..goalForNextLevel
            } else {
                progress.value >= goalForNextLevel
            }

            if (levelCompleted) {
                newLevel++
                if (newLevel == challenge.levels.size) {
                    unlockedTitle = challenge.finalRewardTitle
                }
            } else {
                break
            }
        }
        return progress.copy(currentLevel = newLevel, unlockedTitle = unlockedTitle)
    }
} 