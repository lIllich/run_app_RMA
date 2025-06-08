package com.example.run_app_rma.presentation.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.run_app_rma.R
import com.example.run_app_rma.data.firestore.model.User
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel = viewModel(),
    onLogout: () -> Unit,
    onEditProfile: (String) -> Unit,
    onViewUserPosts: (String) -> Unit,
    onViewFollowing: (String) -> Unit,
    onViewFollowers: (String) -> Unit,
    onUserClick: (String) -> Unit // Added for consistency with MainScreenWithTabs
) {
    val currentUser by profileViewModel.currentUser.collectAsState(initial = null)
    val isLoading by profileViewModel.isLoading.collectAsState()
    val errorMessage by profileViewModel.errorMessage.collectAsState()
    val followingCount by profileViewModel.followingCount.collectAsState()
    val followersCount by profileViewModel.followersCount.collectAsState()
    val postCount by profileViewModel.postCount.collectAsState()
    val unlockedTitles by profileViewModel.unlockedTitles.collectAsState()

    val weeklyStepsProgress by profileViewModel.weeklyStepsProgress.collectAsState()
    val weeklyDurationProgress by profileViewModel.weeklyDurationProgress.collectAsState()
    val weeklyDistanceProgress by profileViewModel.weeklyDistanceProgress.collectAsState()

    var showSetGoalsDialog by remember { mutableStateOf(false) }

    // Scroll state from your branch to make the combined screen scrollable
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState), // Apply vertical scroll
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            Text("Učitavanje profila...")
        } else if (errorMessage != null) {
            Text("Greška: $errorMessage", color = MaterialTheme.colorScheme.error)
            Button(onClick = { profileViewModel.fetchUserProfileAndCounts() }) {
                Text("Pokušaj ponovo")
            }
        } else if (currentUser != null) {
            // Your UserProfileContent with titles feature
            UserProfileContent(user = currentUser!!, titles = unlockedTitles)
            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = { currentUser?.let { onEditProfile(it.id) } },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text("Uredi profil")
                }
                Button(
                    onClick = { onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text("Odjavi se")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { currentUser?.let { onViewUserPosts(it.id) } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Moje objave ($postCount)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = { currentUser?.let { onViewFollowing(it.id) } },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$followingCount", style = MaterialTheme.typography.titleLarge)
                        Text("Pratim", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Button(
                    onClick = { currentUser?.let { onViewFollowers(it.id) } },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$followersCount", style = MaterialTheme.typography.titleLarge)
                        Text("Pratitelji", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Weekly goals feature from the main branch
            Button(
                onClick = { showSetGoalsDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Postavi tjedne ciljeve")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Tjedni ciljevi napredak:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            WeeklyGoalProgress(
                label = "Koraci:",
                current = currentUser?.weeklyGoalSteps,
                progress = weeklyStepsProgress
            )
            WeeklyGoalProgress(
                label = "Trajanje:",
                current = currentUser?.weeklyGoalDuration,
                progress = weeklyDurationProgress
            )
            WeeklyGoalProgress(
                label = "Udaljenost:",
                current = currentUser?.weeklyGoalDistance,
                progress = weeklyDistanceProgress
            )

            if (currentUser?.id == "2MKIn3Un7HevvAAROb4z3cWaxFy2" && false) {
                Button(
                    onClick = { profileViewModel.recalculateDistances() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("Preračunaj ukupnu udaljenost")
                }
            }

        } else {
            Text(text = "Korisnik nije prijavljen ili profil nije pronađen.")
            Button(onClick = { onLogout() }) {
                Text("Idi na prijavu")
            }
        }
    }

    if (showSetGoalsDialog) {
        SetWeeklyGoalsDialog(
            onDismiss = { showSetGoalsDialog = false },
            onConfirm = { steps, durationMinutes, distanceKm ->
                profileViewModel.setWeeklyGoals(steps, durationMinutes, distanceKm)
                showSetGoalsDialog = false
            },
            initialSteps = currentUser?.weeklyGoalSteps,
            initialDurationMinutes = currentUser?.weeklyGoalDuration?.let { TimeUnit.MILLISECONDS.toMinutes(it).toInt() },
            initialDistanceKm = currentUser?.weeklyGoalDistance?.let { it / 1000f }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserProfileContent(user: User, titles: List<String> = emptyList()) {
    val decimalFormat = DecimalFormat("#.##")
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    Image(
        painter = if (!user.profileImageUrl.isNullOrEmpty()) {
            rememberAsyncImagePainter(user.profileImageUrl)
        } else {
            painterResource(R.drawable.ic_profile_placeholder)
        },
        contentDescription = "Profile Picture",
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(text = user.displayName.ifEmpty { "N/A" }, style = MaterialTheme.typography.headlineSmall)

    if (titles.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            titles.forEach { title ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    Text(text = "Email: ${user.email}")
    user.age?.let { age ->
        Text(text = "Dob: $age")
    }
    Text(text = "Ukupna udaljenost: ${decimalFormat.format(user.totalDistanceRun / 1000)} km")
    Text(text = "Ukupno trčanja: ${user.totalRuns}")

    user.lastRunTimestamp?.let { timestamp ->
        Text(text = "Posljednje trčanje: ${dateFormat.format(Date(timestamp))}")
    }
}

@Composable
fun WeeklyGoalProgress(label: String, current: Any?, progress: Float?) {
    if (current != null && progress != null) {
        val progressPercentage = String.format(Locale.getDefault(), "%.1f%%", progress)
        val goalText = when (current) {
            is Int -> "$current"
            is Long -> "${TimeUnit.MILLISECONDS.toMinutes(current)} min"
            is Float -> "${String.format(Locale.getDefault(), "%.1f", current / 1000f)} km"
            else -> ""
        }
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$label $goalText", style = MaterialTheme.typography.bodyMedium)
                Text("$progressPercentage", style = MaterialTheme.typography.bodyMedium)
            }
            LinearProgressIndicator(
                progress = { (progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}


@Composable
fun SetWeeklyGoalsDialog(
    onDismiss: () -> Unit,
    onConfirm: (steps: Int?, durationMinutes: Int?, distanceKm: Float?) -> Unit,
    initialSteps: Int?,
    initialDurationMinutes: Int?,
    initialDistanceKm: Float?
) {
    var steps by remember { mutableStateOf(initialSteps?.toString() ?: "") }
    var duration by remember { mutableStateOf(initialDurationMinutes?.toString() ?: "") }
    var distance by remember { mutableStateOf(initialDistanceKm?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Postavi tjedne ciljeve") },
        text = {
            Column {
                TextField(
                    value = steps,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                            steps = newValue
                        }
                    },
                    label = { Text("Broj koraka") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = duration,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                            duration = newValue
                        }
                    },
                    label = { Text("Trajanje (minuta)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = distance,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("^\\d*\\.?\\d*\$"))) {
                            distance = newValue
                        }
                    },
                    label = { Text("Udaljenost (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    steps.toIntOrNull(),
                    duration.toIntOrNull(),
                    distance.toFloatOrNull()
                )
            }) {
                Text("Potvrdi")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Odustani")
            }
        }
    )
}