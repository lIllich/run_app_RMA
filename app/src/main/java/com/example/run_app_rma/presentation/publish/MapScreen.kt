package com.example.run_app_rma.presentation.publish

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.run_app_rma.data.db.AppDatabase
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import androidx.compose.ui.graphics.Color // Assuming this is needed for Polyline color
import androidx.compose.material3.IconButton // For the IconButton
import androidx.compose.material3.Icon // For the Icon
import androidx.compose.material.icons.Icons // For the Icons object
import androidx.compose.material.icons.automirrored.filled.ArrowBack


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    runId: Long,
    appDatabase: AppDatabase, // You might need to pass this or a Dao directly
    onBackClick: () -> Unit // For navigating back from the map screen
) {
    val runDetailsViewModel: RunDetailsViewModel = viewModel(
        factory = RunDetailsViewModel.Factory(
            runId = runId,
            runDao = appDatabase.runDao(), // Pass the necessary DAOs
            locationDao = appDatabase.locationDao(),
            runPostRepository = null, // Not needed for map display
            userRepository = null, // Not needed for map display
            firebaseAuth = null // Not needed for map display
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
                .padding(paddingValues), // Apply padding from Scaffold
            cameraPositionState = cameraPositionState
        ) {
            Polyline(
                points = pathPoints,
                color = Color.Red, // Make sure Color is imported
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