package com.example.run_app_rma.domain.model

enum class ChallengeType {
    TOTAL_RUNS,
    LONGEST_RUN,
    TOTAL_ELEVATION_GAIN,
    FASTEST_RUN
}

data class Challenge(
    val id: String,
    val name: String,
    val description: (Float) -> String,
    val type: ChallengeType,
    val levels: List<Float>,
    val finalRewardTitle: String
)

object ChallengesRepository {
    val challenges = listOf(
        Challenge(
            id = "total_runs",
            name = "Consistent Runner",
            description = { goal -> "Complete a total of ${goal.toInt()} runs (min. 2km each)." },
            type = ChallengeType.TOTAL_RUNS,
            levels = listOf(1f, 5f, 10f, 20f, 30f, 40f, 50f, 60f, 80f, 100f),
            finalRewardTitle = "Hundred Club Runner"
        ),
        Challenge(
            id = "longest_run",
            name = "Distance Explorer",
            description = { goal -> "Complete a single run of %.1f km.".format(goal / 1000f) },
            type = ChallengeType.LONGEST_RUN,
            levels = listOf(5000f, 10000f, 15000f, 21097.5f, 30000f, 42195f, 60000f), // in meters
            finalRewardTitle = "Ultra Marathoner"
        ),
        Challenge(
            id = "total_elevation",
            name = "Mountain Goat",
            description = { goal -> "Accumulate ${goal.toInt()}m of elevation gain." },
            type = ChallengeType.TOTAL_ELEVATION_GAIN,
            levels = listOf(100f, 250f, 500f, 1000f, 2000f), // in meters
            finalRewardTitle = "Peak Conqueror"
        ),
        Challenge(
            id = "fastest_5k",
            name = "Speed Demon",
            description = { goal -> "Run 5km in under ${goal.toInt()} minutes." },
            type = ChallengeType.FASTEST_RUN,
            levels = listOf(30f, 28f, 25f, 22f, 20f), // in minutes
            finalRewardTitle = "Velocity Master"
        )
    )
} 