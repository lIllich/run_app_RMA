package com.example.run_app_rma.presentation.publish

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.run_app_rma.domain.model.RunEntity
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import com.google.accompanist.swiperefresh.SwipeRefresh // Import SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState // Import rememberSwipeRefreshState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishRunScreen(
    modifier: Modifier = Modifier,
    publishRunViewModel: PublishRunViewModel = viewModel()
) {
    val localRuns = publishRunViewModel.localRuns
    val isLoading by publishRunViewModel.isLoading
    val isRefreshing by publishRunViewModel.isRefreshing.collectAsState()
    val errorMessage by publishRunViewModel.errorMessage
    val successMessage by publishRunViewModel.successMessage
    val selectedRun by publishRunViewModel.selectedRun
    val caption by publishRunViewModel.caption

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val decimalFormat = DecimalFormat("#.##")

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Objavi Trčanje",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading && !isRefreshing) {
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

        Spacer(modifier = Modifier.height(16.dp))

        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { publishRunViewModel.loadLocalRuns() },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (localRuns.isEmpty() && !isLoading && !isRefreshing) {
                    Text("Nema lokalno spremljenih trčanja za objavu.")
                } else {
                    Text("Odaberite trčanje za objavu:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(localRuns, key = { it.id }) { run ->
                            RunItemCard(
                                run = run,
                                dateFormat = dateFormat,
                                decimalFormat = decimalFormat,
                                isSelected = run.id == selectedRun?.id,
                                onRunSelected = { publishRunViewModel.selectRun(it) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        selectedRun?.let { run ->
            Text("Odabrano trčanje:", style = MaterialTheme.typography.titleSmall)
            Text("Vrijeme: ${dateFormat.format(Date(run.startTime))} - ${run.endTime?.let { dateFormat.format(Date(it)) } ?: "N/A"}")
            Text("Udaljenost: ${run.distance?.let { decimalFormat.format(it / 1000) } ?: "N/A"} km")
            Text("Prosječni tempo: ${run.avgPace?.let { decimalFormat.format(it) } ?: "N/A"} min/km")

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = caption,
                onValueChange = { publishRunViewModel.onCaptionChanged(it) },
                label = { Text("Dodajte opis (opcionalno)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { publishRunViewModel.publishSelectedRun() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Objavi Trčanje")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunItemCard(
    run: RunEntity,
    dateFormat: SimpleDateFormat,
    decimalFormat: DecimalFormat,
    isSelected: Boolean,
    onRunSelected: (RunEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRunSelected(run) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Trčanje započeto: ${dateFormat.format(Date(run.startTime))}",
                style = MaterialTheme.typography.bodyMedium
            )
            run.endTime?.let {
                Text(
                    text = "Završeno: ${dateFormat.format(Date(it))}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            run.distance?.let {
                Text(
                    text = "Udaljenost: ${decimalFormat.format(it / 1000)} km",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            run.avgPace?.let {
                Text(
                    text = "Prosječni tempo: ${decimalFormat.format(it)} min/km",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
