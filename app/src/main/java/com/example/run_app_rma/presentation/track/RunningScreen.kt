package com.example.run_app_rma.presentation.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.run_app_rma.sensor.tracking.StepCountManager

@Composable
fun RunningScreen(
    modifier: Modifier = Modifier,
    runViewModel: RunViewModel
) {
    val isTracking by runViewModel.isTracking.collectAsState()
    val locationText by runViewModel.liveLocationData
//    val sensorText by runViewModel.liveSensorData
    val stepCount by StepCountManager.liveStepCount.collectAsState()
    val elapsedTime by runViewModel.elapsedTime.collectAsState()
    val distanceMeters by runViewModel.distanceMeters.collectAsState()

    // debug
//    val context = LocalContext.current
    //***


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = locationText)
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Steps: $stepCount")
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Elapsed: ${elapsedTime / 1000 / 60}m ${(elapsedTime / 1000) % 60}s"
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Distance: ${"%.2f".format(distanceMeters / 1000)} km"
        )
        Spacer(modifier = Modifier.height(24.dp))

        if(!isTracking) {
            Button(
                onClick = { runViewModel.startRun() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start run")
            }
        } else {
            Button(
                onClick = { runViewModel.stopRun() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Stop run")
            }
        }

        // debug
//        Button(
//            onClick = { runViewModel.exportDatabase(context) },
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(top = 16.dp)
//        ) {
//            Text("Export DB")
//        }
        //***
    }
}