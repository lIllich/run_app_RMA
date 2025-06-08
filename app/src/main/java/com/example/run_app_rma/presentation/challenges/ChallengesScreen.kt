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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(viewModel: ChallengeViewModel) {
    val uiState by viewModel.challengeUiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Challenges") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(uiState) { state ->
                ChallengeCard(state = state)
            }
        }
    }
}

@Composable
fun ChallengeCard(state: ChallengeUiState) {
    val challenge = state.challenge
    val progress = state.progress

    val isCompleted = progress.currentLevel >= challenge.levels.size
    val nextGoal = if (!isCompleted) challenge.levels[progress.currentLevel] else challenge.levels.last()
    val currentProgress = progress.value

    val progressPercentage = if (isCompleted) {
        1f
    } else if (challenge.type == ChallengeType.FASTEST_RUN) {
        if (currentProgress == 0f) 0f else (nextGoal / currentProgress).coerceIn(0f, 1f)
    } else {
        (currentProgress / nextGoal).coerceIn(0f, 1f)
    }

    val animatedProgress by animateFloatAsState(targetValue = progressPercentage, label = "progressAnimation")

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
                Text("Completed! Title unlocked: ${challenge.finalRewardTitle}", color = MaterialTheme.colorScheme.primary)
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