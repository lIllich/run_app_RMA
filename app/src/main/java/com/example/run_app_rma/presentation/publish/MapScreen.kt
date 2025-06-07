package com.example.run_app_rma.presentation.publish

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.run_app_rma.data.db.AppDatabase
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    runId: Long,
    appDatabase: AppDatabase,
    onBackClick: () -> Unit
) {
    val runDetailsViewModel: RunDetailsViewModel = viewModel(
        factory = RunDetailsViewModel.Factory(
            runId = runId,
            runDao = appDatabase.runDao(),
            locationDao = appDatabase.locationDao(),
            runPostRepository = null,   // not needed
            userRepository = null,      // not needed
            firebaseAuth = null         // not needed
        )
    )
    val locationData by runDetailsViewModel.locationData.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Karta Trčanja") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Natrag")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (locationData.isEmpty()) {
            Text("Učitavanje podataka o lokaciji...", modifier = Modifier.padding(paddingValues))
            return@Scaffold
        }

        val pathPoints = locationData.map { LatLng(it.lat, it.lon) }
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(pathPoints.first(), 15f)
        }

        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            cameraPositionState = cameraPositionState
        ) {
            Polyline(
                points = pathPoints,
                color = Color.Red,
                width = 8f
            )
            Marker(
                state = rememberMarkerState(position = pathPoints.first()),
                title = "Start"
            )
            if (pathPoints.size > 1) {
                Marker(
                    state = rememberMarkerState(position = pathPoints.last()),
                    title = "End"
                )
            }
        }
    }
}