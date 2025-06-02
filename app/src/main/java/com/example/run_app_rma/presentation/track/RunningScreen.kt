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
import androidx.compose.ui.unit.dp

@Composable
fun RunningScreen(
    modifier: Modifier = Modifier,
    runViewModel: RunViewModel
) {
    val isTracking by runViewModel.isTracking.collectAsState()
    val locationText by runViewModel.liveLocationData
    val accelerometerText by runViewModel.liveAccelerometerData
    val gyroscopeText by runViewModel.liveGyroscopeData

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = locationText)
        Text(text = accelerometerText)
        Text(text = gyroscopeText)

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

        // Uklonjen Logout gumb jer se sada upravlja tabovima
        /*
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
        */
    }
}