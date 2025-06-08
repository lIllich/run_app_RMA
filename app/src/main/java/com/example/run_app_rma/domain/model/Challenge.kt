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
            name = "Ustrajni Trkač", // Translated from "Consistent Runner"
            description = { goal -> "Dovršite ukupno ${goal.toInt()} trčanja (min. 2 km svako)." }, // Translated
            type = ChallengeType.TOTAL_RUNS,
            levels = listOf(1f, 5f, 10f, 20f, 30f, 40f, 50f, 60f, 80f, 100f),
            finalRewardTitle = "Trkač Stotog Kluba" // Translated from "Hundred Club Runner"
        ),
        Challenge(
            id = "longest_run",
            name = "Istraživač Udaljenosti", // Translated from "Distance Explorer"
            description = { goal -> "Dovršite jedno trčanje od %.1f km.".format(goal / 1000f) }, // Translated
            type = ChallengeType.LONGEST_RUN,
            levels = listOf(5000f, 10000f, 15000f, 21097.5f, 30000f, 42195f, 60000f), // in meters
            finalRewardTitle = "Ultra Maratonac" // Translated from "Ultra Marathoner"
        ),
        Challenge(
            id = "total_elevation",
            name = "Planinska Koza", // Translated from "Mountain Goat"
            description = { goal -> "Skupite ${goal.toInt()}m uspona." }, // Translated
            type = ChallengeType.TOTAL_ELEVATION_GAIN,
            levels = listOf(100f, 250f, 500f, 1000f, 2000f),
            finalRewardTitle = "Majstor Uspona" // Translated from "Master of Ascent"
        ),
        Challenge(
            id = "fastest_run",
            name = "Brzi Sprint", // Translated from "Speed Demon" (or similar, assuming "Fastest Run" implies speed)
            description = { goal -> "Pretrčite 5 km u ${goal.toInt()} minuta ili brže." }, // Translated, assuming fastest 5k in minutes
            type = ChallengeType.FASTEST_RUN,
            levels = listOf(30f, 25f, 22f, 20f, 18f), // in minutes (e.g., 30 min, 25 min)
            finalRewardTitle = "Munjeviti Trkač" // Translated from "Lightning Runner"
        )
    )
}