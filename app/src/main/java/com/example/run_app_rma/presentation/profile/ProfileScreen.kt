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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.run_app_rma.R
import com.example.run_app_rma.data.firestore.model.User
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel = viewModel(),
    onLogout: () -> Unit,
    onEditProfile: (String) -> Unit,
    onViewUserPosts: (String) -> Unit,
    onViewFollowing: (String) -> Unit,
    onViewFollowers: (String) -> Unit,
) {
    val currentUser by profileViewModel.currentUser.collectAsState(initial = null)
    val isLoading by profileViewModel.isLoading.collectAsState()
    val errorMessage by profileViewModel.errorMessage.collectAsState()
    val followingCount by profileViewModel.followingCount.collectAsState()
    val followersCount by profileViewModel.followersCount.collectAsState()
    val postCount by profileViewModel.postCount.collectAsState()
    val unlockedTitles by profileViewModel.unlockedTitles.collectAsState()

//    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
//    val decimalFormat = DecimalFormat("#.##")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
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

            // buttons for posts, following, and followers
            Button(
                onClick = { currentUser?.let { onViewUserPosts(it.id) } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Moje objave ($postCount)")    // display post count
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

            // button to trigger recalculation, visible only for a specific user ID
            if (currentUser?.id == "1nfVhq0VD7amA3JGAtwcxGcyzd13") {
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

    // display lastRunTimestamp if available and format it
    user.lastRunTimestamp?.let { timestamp ->
        Text(text = "Posljednje trčanje: ${dateFormat.format(Date(timestamp))}")
    }
}
