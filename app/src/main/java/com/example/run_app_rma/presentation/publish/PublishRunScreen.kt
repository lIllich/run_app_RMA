package com.example.run_app_rma.presentation.publish

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishRunScreen(
    modifier: Modifier = Modifier,
    publishRunViewModel: PublishRunViewModel = viewModel(), // ViewModel will be provided by MainScreenWithTabs
    onRunClick: (Long) -> Unit
) {
    val localRuns = publishRunViewModel.localRuns
    val isLoading by publishRunViewModel.isLoading
//    val errorMessage by publishRunViewModel.errorMessage
//    val successMessage by publishRunViewModel.successMessage

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val decimalFormat = DecimalFormat("#.##")
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        publishRunViewModel.eventFlow.collectLatest { event ->
            when (event) {
                is PublishRunViewModel.UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Povijest trčanja",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (localRuns.isEmpty() && !isLoading) {
            Text("Nema lokalno spremljenih trčanja za objavu.")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(localRuns, key = { it.id }) { run ->
                    RunItemCard(
                        run = run,
                        dateFormat = dateFormat,
                        decimalFormat = decimalFormat,
                        onRunSelected = { runId -> onRunClick(runId) },
                        onDeleteRun = { runId -> publishRunViewModel.deleteRun(runId) } // Pass delete function
                    )
                }
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
    onRunSelected: (Long) -> Unit,
    onDeleteRun: (Long) -> Unit // New parameter for delete action
) {
    var expanded by remember { mutableStateOf(false) } // State for dropdown menu

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRunSelected(run.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
            Box(
                modifier = Modifier.wrapContentSize(Alignment.TopEnd)
            ) {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Više opcija")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Izbriši trčanje") },
                        onClick = {
                            onDeleteRun(run.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}