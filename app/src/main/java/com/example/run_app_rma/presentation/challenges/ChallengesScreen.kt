package com.example.run_app_rma.presentation.challenges

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.run_app_rma.domain.model.ChallengeType
import com.example.run_app_rma.domain.model.UserChallengeProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(viewModel: ChallengeViewModel) {
    val uiState by viewModel.challengeUiState.collectAsState()

    // Removed Scaffold and TopAppBar
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp), // Added padding for the whole screen content
        horizontalAlignment = Alignment.CenterHorizontally // Center content horizontally
    ) {
        // Centered title text
        Text(
            text = "Izazovi",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp) // Add some vertical padding for the title
        )
        Spacer(modifier = Modifier.height(8.dp)) // Space between title and challenges list

        LazyColumn(
            modifier = Modifier.fillMaxSize(), // Fill remaining space
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState) { challengeUiState ->
                ChallengeCard(challengeUiState = challengeUiState)
            }
        }
    }
}

@Composable
fun ChallengeCard(challengeUiState: ChallengeUiState) {
    val challenge = challengeUiState.challenge
    val progress = challengeUiState.progress

    val currentProgressValue = when (challenge.type) {
        ChallengeType.TOTAL_RUNS -> progress.value
        ChallengeType.LONGEST_RUN -> progress.value
        ChallengeType.TOTAL_ELEVATION_GAIN -> progress.value
        ChallengeType.FASTEST_RUN -> progress.value
    }

    val nextGoal = challenge.levels.getOrNull(progress.currentLevel + 1) ?: 0f

    val isCompleted = progress.currentLevel >= challenge.levels.size - 1

    val animatedProgress by animateFloatAsState(
        targetValue = if (isCompleted) 1f else (currentProgressValue / nextGoal).coerceIn(0f, 1f),
        label = "challenge_progress_animation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = challenge.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (isCompleted) {
                Text("Dovršeno! Otključan naslov: ${challenge.finalRewardTitle}", color = MaterialTheme.colorScheme.primary)
            } else {
                Text(
                    text = challenge.description(nextGoal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.small)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${progress.currentLevel}/${challenge.levels.size}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                )
            }
        }
    }
}