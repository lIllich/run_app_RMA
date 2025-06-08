package com.example.run_app_rma.presentation.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.run_app_rma.sensor.tracking.StepCountManager

@Composable
fun RunningScreen(
    modifier: Modifier = Modifier,
    runViewModel: RunViewModel
) {
    val isTracking by runViewModel.isTracking.collectAsState()
    val locationText by runViewModel.liveLocationData
    val stepCount by StepCountManager.liveStepCount.collectAsState()
    val elapsedTime by runViewModel.elapsedTime.collectAsState()
    val distanceMeters by runViewModel.distanceMeters.collectAsState()
    val pace by runViewModel.livePace.collectAsState()

    val elapsedTimeText = if (elapsedTime < 60_000) {
        "${(elapsedTime / 1000)} s"
    } else {
        val minutes = elapsedTime / 1000 / 60
        val seconds = (elapsedTime / 1000) % 60
        "$minutes min $seconds s"
    }
    val distanceText = if (distanceMeters < 1000) {
        "${"%.0f".format(distanceMeters)} m"
    } else {
        "${"%.2f".format(distanceMeters / 1000)} km"
    }
    val paceText = if (pace > 0f) "${"%.2f".format(pace)} min/km" else "--"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Running Tracker", style = MaterialTheme.typography.headlineSmall)

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Lokacija", style = MaterialTheme.typography.labelMedium)
                Text(locationText)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = "Distance")
                    Text(distanceText, style = MaterialTheme.typography.bodyLarge)
                    Text("Udaljenost", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Speed, contentDescription = "Pace")
                    Text(paceText, style = MaterialTheme.typography.bodyLarge)
                    Text("Tempo", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AccessTime, contentDescription = "Duration")
                    Text(elapsedTimeText, style = MaterialTheme.typography.bodyLarge)
                    Text("Trajanje", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isTracking) runViewModel.stopRun() else runViewModel.startRun()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isTracking) "Završi trčanje" else "Započni trčanje")
            }
        }
    }
}