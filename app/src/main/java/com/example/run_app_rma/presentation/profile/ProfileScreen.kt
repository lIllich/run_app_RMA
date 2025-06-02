package com.example.run_app_rma.presentation.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.run_app_rma.R // Pretpstavka da postoji defaultna slika
import com.example.run_app_rma.data.firestore.model.User
import java.text.DecimalFormat

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    onLogout: () -> Unit,
    onEditProfile: (String) -> Unit
) {
    val currentUser by profileViewModel.currentUser.collectAsState() // Corrected: collectAsState for currentUser
    val isLoading by profileViewModel.isLoading.collectAsState()
    val errorMessage by profileViewModel.errorMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(50.dp))
            Text("Učitavanje profila...")
        } else if (errorMessage != null) {
            Text("Greška: $errorMessage", color = MaterialTheme.colorScheme.error)
            Button(onClick = { profileViewModel.fetchUserProfile() }) {
                Text("Pokušaj ponovo")
            }
        } else if (currentUser != null) {
            UserProfileContent(user = currentUser!!) // Non-null assertion after check
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = { currentUser?.let { onEditProfile(it.id) } }, // Corrected: access user.id
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Text("Uredi profil")
                }
                Button(
                    onClick = { onLogout() }, // Corrected: use onLogout lambda from parent
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text("Odjavi se")
                }
            }
        } else {
            Text(text = "Korisnik nije prijavljen ili profil nije pronađen.")
            Button(onClick = { onLogout() }) { // Ensure this triggers logout flow if no user
                Text("Idi na prijavu")
            }
        }
    }
}

@Composable
fun UserProfileContent(user: User) {
    val decimalFormat = DecimalFormat("#.##")

    Image(
        painter = if (user.profileImageUrl != null && user.profileImageUrl.isNotEmpty()) {
            rememberAsyncImagePainter(user.profileImageUrl)
        } else {
            painterResource(R.drawable.ic_profile_placeholder) // Dodaj defaultnu sliku profila u drawable
        },
        contentDescription = "Profile Picture",
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(text = "Ime: ${user.displayName.ifEmpty { "N/A" }}")
    Text(text = "Email: ${user.email}")
    user.age?.let { age ->
        Text(text = "Dob: $age")
    }
    Text(text = "Ukupna udaljenost: ${decimalFormat.format(user.totalDistanceRun)} km")
    Text(text = "Ukupno trčanja: ${user.totalRuns}")
    // Display lastRunTimestamp if available
    user.lastRunTimestamp?.let { timestamp ->
        // Format timestamp as needed, e.g., using SimpleDateFormat
        // val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        // Text(text = "Zadnje trčanje: ${dateFormat.format(Date(timestamp))}")
        Text(text = "Zadnje trčanje (timestamp): $timestamp") // For now, just display raw timestamp
    }
    // Display createdAt if available
    user.createdAt?.let { timestamp ->
        // val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        // Text(text = "Kreirano: ${dateFormat.format(Date(timestamp))}")
        Text(text = "Kreirano (timestamp): $timestamp") // For now, just display raw timestamp
    }
}