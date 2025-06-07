package com.example.run_app_rma.presentation.runpost

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.run_app_rma.presentation.publish.ElevationGraph

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ElevationGraphScreen(
    postId: String,
    onBack: () -> Unit,
    runMapViewModel: RunMapViewModel = viewModel()
) {
    val runPost by runMapViewModel.getRunPost(postId).collectAsState(initial = null)

    Log.d("ElevationGraphScreen", "runPost is: $runPost")
    Log.d("ElevationGraphScreen", "pathPoints is empty: ${runPost?.pathPoints?.isEmpty()}")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Graf Elevacije") },
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
                Log.d("ElevationGraphScreen", "Displaying graph with ${post.pathPoints.size} points")

                val elevations = post.pathPoints.map { it.elevation.toFloat() }
                val timestamps = List(post.pathPoints.size) { index -> post.startTime + (index * 1000L) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    ElevationGraph(
                        normalizedAlts = elevations,
                        timestamps = timestamps,
                        startTime = post.startTime,
                        modifier = Modifier.padding(16.dp)
                    )
                }

            } else {
                Log.d("ElevationGraphScreen", "Showing 'No elevation data'")
                Text("No elevation data", modifier = Modifier.padding(16.dp).padding(paddingValues))
            }
        } ?: run {
            Log.d("ElevationGraphScreen", "Showing 'Loading...' (runPost is null)")
            Text("Loading...", modifier = Modifier.padding(16.dp).padding(paddingValues))
        }
    }
}