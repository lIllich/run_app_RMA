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
import androidx.compose.ui.graphics.nativeCanvas
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
import kotlin.math.ceil
import kotlin.math.floor

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
//                Text("Duljina rute: ${runDetailsViewModel.getRouteLength(locationData)}")
                Text("Koraci: ${run.steps?.toString() ?: "N/A"}")

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
                        Text("Prikaži kartu")
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

// elevation graph smoothing
fun movingAverage(data: List<Float>, windowSize: Int): List<Float> {
    if (data.size <= windowSize) return data
    val result = mutableListOf<Float>()
    for (i in data.indices) {
        val start = maxOf(0, i - windowSize / 2)
        val end = minOf(data.size - 1, i + windowSize / 2)
        val window = data.subList(start, end + 1)
        result.add(window.average().toFloat())
    }
    return result
}

@Composable
fun ElevationGraph(locationData: List<LocationDataEntity>) {
    if (locationData.isEmpty()) return

    val altitudes = locationData.map { it.alt }
    val timestamps = locationData.map { it.timestamp }

    val minAlt = altitudes.minOrNull() ?: 0.0
    val maxAlt = altitudes.maxOrNull() ?: 0.0
    val elevationRange = maxAlt - minAlt

    val useKilometers = elevationRange >= 1000
    val yUnitLabel = if (useKilometers) "km" else "m"

    val rawAlts = altitudes.map {
        val value = if (useKilometers) it / 1000.0 else it
        value.toFloat()
    }

    // Smooth with moving average (e.g., windowSize = 5)
    val normalizedAlts = movingAverage(rawAlts, windowSize = 5)

    val startTime = timestamps.firstOrNull() ?: 0L

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val width = size.width
        val height = size.height

        // Lijevi rub se koristi za Y-osi (brojevi + oznaka) → gotovo uz rub
        val yAxisOffset = 0f // minimalni razmak od lijevog ruba
        val yAxisLabelWidth = 80f // prostor za brojeve Y-osi
        val leftPadding = yAxisOffset + yAxisLabelWidth // ovo gura graf udesno od Y-osi
        val rightPadding = 20f
        val bottomPadding = 50f
        val topPadding = 50f

        val graphPaddingX = 20f // dodatni razmak između grafa i Y-osi
        val graphPaddingY = 20f // razmak između grafa i vrha/dna grafa

        val graphWidth = width - leftPadding - rightPadding
        val graphHeight = height - bottomPadding - topPadding

        val xStep = (graphWidth - 2 * graphPaddingX) / (normalizedAlts.size - 1).coerceAtLeast(1)

        val minY = floor((normalizedAlts.minOrNull() ?: 0f))
        val maxY = ceil((normalizedAlts.maxOrNull() ?: 0f))
        val yRange = maxY - minY

        val path = Path()
        path.moveTo(
            leftPadding + graphPaddingX, // ⬅️ linija grafa počinje nakon Y-osi + unutarnji razmak
            topPadding + graphPaddingY + (graphHeight - 2 * graphPaddingY) * (1f - (normalizedAlts.first() - minY) / yRange)
        )

        for (i in 1 until normalizedAlts.size) {
            val x = leftPadding + graphPaddingX + i * xStep
            val y = topPadding + graphPaddingY + (graphHeight - 2 * graphPaddingY) * (1f - (normalizedAlts[i] - minY) / yRange)
            path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = Color.Blue,
            style = Stroke(width = 3f)
        )

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 34f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }

        val yLabelCount = 7
        for (i in 0 until yLabelCount) {
            val fraction = i / (yLabelCount - 1).toFloat()
            val value = maxY - fraction * yRange
            val y = topPadding + graphHeight * fraction
            val label = value.toInt().toString()

            drawContext.canvas.nativeCanvas.drawText(
                label,
                yAxisOffset,
                y + 10f,
                textPaint
            )
        }

        // Mjerna jedinica na vrhu Y-osi (pomaknuta malo iznad prve oznake)
        drawContext.canvas.nativeCanvas.drawText(
            yUnitLabel,
            yAxisOffset,
            topPadding - 40f,
            textPaint
        )

        val xLabelCount = 5
        val step = (normalizedAlts.size - 1) / (xLabelCount - 1).coerceAtLeast(1)

        for (i in 0 until xLabelCount) {
            val index = i * step
            if (index >= timestamps.size) continue

            val x = leftPadding + graphPaddingX + index * xStep // ⬅️ isto pravilo pomaka kao graf
            val elapsedMillis = timestamps[index] - startTime
            val minutes = elapsedMillis / 60000
            val seconds = (elapsedMillis / 1000) % 60
            val timeLabel = String.format("%d:%02d", minutes, seconds)

            drawContext.canvas.nativeCanvas.drawText(
                timeLabel,
                x - 30f,
                height - 10f,
                textPaint
            )
        }
    }
}
