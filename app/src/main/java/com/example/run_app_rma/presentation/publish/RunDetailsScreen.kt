package com.example.run_app_rma.presentation.publish

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.run_app_rma.data.db.AppDatabase
import com.example.run_app_rma.domain.model.LocationDataEntity
import com.example.run_app_rma.data.firestore.repository.RunPostRepository
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailsScreen(
    runId: Long,
    appDatabase: AppDatabase,
    runPostRepository: RunPostRepository,
    userRepository: UserRepository,
    firebaseAuth: FirebaseAuth,
    modifier: Modifier = Modifier,
    onViewMapClick: (Long) -> Unit
) {
    val runDetailsViewModel: RunDetailsViewModel = viewModel(
        factory = RunDetailsViewModel.Factory(
            runId = runId,
            runDao = appDatabase.runDao(),
            locationDao = appDatabase.locationDao(),
            runPostRepository = runPostRepository,
            userRepository = userRepository,
            firebaseAuth = firebaseAuth
        )
    )

    val runDetails by runDetailsViewModel.runDetails.collectAsState()
    val locationData by runDetailsViewModel.locationData.collectAsState()
    val caption by runDetailsViewModel.caption
    val isLoading by runDetailsViewModel.isLoading
    val errorMessage by runDetailsViewModel.errorMessage
    val successMessage by runDetailsViewModel.successMessage

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val decimalFormat = DecimalFormat("#.##")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Detalji Trčanja") })
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }

            successMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }

            runDetails?.let { run ->
                Text(
                    text = "Vrijeme: ${dateFormat.format(Date(run.startTime))} - ${run.endTime?.let { dateFormat.format(Date(it)) } ?: "N/A"}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text("Trajanje: ${runDetailsViewModel.getDurationFormatted(run)}")
                Text("Udaljenost: ${run.distance?.let { decimalFormat.format(it / 1000) } ?: "N/A"} km")
                Text("Prosječni tempo: ${run.avgPace?.let { decimalFormat.format(it) } ?: "N/A"} min/km")
                Text("Prosječna brzina: ${runDetailsViewModel.getAverageSpeed(run)}")
                Text("Razlika u elevaciji: ${runDetailsViewModel.getElevationGain(locationData)}")
                Text("Duljina rute: ${runDetailsViewModel.getRouteLength(locationData)}")

                Spacer(modifier = Modifier.height(16.dp))

                // elevation graph
                if (locationData.isNotEmpty()) {
                    Text("Graf Elevacije", style = MaterialTheme.typography.titleSmall)
                    ElevationGraph(locationData = locationData)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Button to open fullscreen map
                if (locationData.isNotEmpty()) {
                    Button(
                        onClick = { onViewMapClick(runId) }, // Pass runId to the navigation lambda
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Prikaz karte preko cijelog zaslona")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }


                // kilometer splits
                val kilometerSplits = runDetailsViewModel.getKilometerSplits(locationData)
                if (kilometerSplits.isNotEmpty()) {
                    Text("Podaci po kilometru", style = MaterialTheme.typography.titleSmall)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp) // Limit height for splits list
                    ) {
                        itemsIndexed(kilometerSplits) { index, split ->
                            Text("Kilometar ${index + 1}: Vrijeme: ${split.first}, Tempo: ${split.second}")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }


                OutlinedTextField(
                    value = caption,
                    onValueChange = { runDetailsViewModel.onCaptionChanged(it) },
                    label = { Text("Dodajte opis (opcionalno)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { runDetailsViewModel.publishRun() },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Objavi Trčanje")
                }
            } ?: run {
                Text("Učitavanje detalja trčanja...", modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun ElevationGraph(locationData: List<LocationDataEntity>) {
    val altitudes = locationData.map { it.alt }
    if (altitudes.isEmpty()) return

    val minAlt = altitudes.minOrNull() ?: 0.0
    val maxAlt = altitudes.maxOrNull() ?: 0.0

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(8.dp)
    ) {
        val width = size.width
        val height = size.height
        val xStep = width / (altitudes.size - 1).toFloat()

        val path = Path()
        if (altitudes.isNotEmpty()) {
            val firstPointY = height - ((altitudes.first() - minAlt) / (maxAlt - minAlt).toFloat()) * height
            path.moveTo(0f, firstPointY.toFloat())

            for (i in 1 until altitudes.size) {
                val x = i * xStep
                val y = height - ((altitudes[i] - minAlt) / (maxAlt - minAlt).toFloat()) * height
                path.lineTo(x, y.toFloat())
            }
        }
        drawPath(
            path = path,
            color = Color.Blue,
            style = Stroke(width = 3f)
        )
    }
}