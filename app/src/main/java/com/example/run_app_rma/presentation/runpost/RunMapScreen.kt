package com.example.run_app_rma.presentation.runpost

import android.annotation.SuppressLint
import android.util.Log
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunMapScreen(
    postId: String,
    onBack: () -> Unit,
    runMapViewModel: RunMapViewModel = viewModel()
) {
    val runPost by runMapViewModel.getRunPost(postId).collectAsState(initial = null)

    Log.d("RunMapScreen", "runPost is: $runPost")
    Log.d("RunMapScreen", "pathPoints is empty: ${runPost?.pathPoints?.isEmpty()}")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Karta Trčanja") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        runPost?.let { post ->
            if (post.pathPoints.isNotEmpty()) {
                Log.d("RunMapScreen", "Displaying map with ${post.pathPoints.size} points")
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(
                        LatLng(post.pathPoints[0].latitude, post.pathPoints[0].longitude), 15f
                    )
                }

                GoogleMap(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    cameraPositionState = cameraPositionState
                ) {
                    Polyline(
                        points = post.pathPoints.map {
                            LatLng(it.latitude, it.longitude)
                        },
                        color = Color.Red,
                        width = 8f
                    )
                }
            } else {
                Log.d("RunMapScreen", "Showing 'No path data available'")
                Text("No path data available", modifier = Modifier.padding(16.dp))
            }
        } ?: run {
            Log.d("RunMapScreen", "Showing 'Loading...' (runPost is null)")
            Text("Loading...", modifier = Modifier.padding(16.dp))
        }
    }
}
