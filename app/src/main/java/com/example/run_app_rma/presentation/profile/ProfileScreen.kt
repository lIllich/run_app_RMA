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
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel,
    onLogout: () -> Unit, // Callback za odjavu
    onEditProfile: (String) -> Unit // Callback za uređivanje profila, prosljeđuje UID
) {
    val currentUser by profileViewModel.currentUser.collectAsState()
    val isLoading by profileViewModel.isLoading.collectAsState()
    val errorMessage by profileViewModel.errorMessage.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
            Text(text = "Error: ${errorMessage}")
            Button(onClick = { profileViewModel.fetchUserProfile() }) {
                Text("Pokušaj ponovo")
            }
        } else if (currentUser != null) {
            UserProfileContent(user = currentUser!!)
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = { onEditProfile(currentUser!!.id) }, // Proslijedi UID na klik
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Text("Uredi podatke")
                }
                Button(
                    onClick = {
                        profileViewModel.logoutUser()
                        onLogout() // Pozovi callback za navigaciju na LoginScreen
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text("Odjavi se")
                }
            }
        } else {
            Text(text = "Korisnik nije prijavljen ili profil nije pronađen.")
            // Možda dodati gumb za povratak na prijavu ako nije prijavljen
            Button(onClick = { onLogout() }) {
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
    } ?: Text(text = "Dob: Nije postavljeno")

    Text(text = "Ukupna pređena udaljenost: ${decimalFormat.format(user.totalDistanceRun / 1000)} km")
    Text(text = "Broj trčanja: ${user.totalRuns}")
}