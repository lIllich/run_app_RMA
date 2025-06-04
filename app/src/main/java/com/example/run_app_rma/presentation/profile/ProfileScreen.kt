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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign // Correct import for TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.run_app_rma.R // Pretpstavka da postoji defaultna slika
import com.example.run_app_rma.data.firestore.model.RunPost // Import RunPost
import com.example.run_app_rma.data.firestore.model.User
import com.example.run_app_rma.presentation.common.RunPostCard // Import the reusable RunPostCard
import java.text.DecimalFormat
import java.text.SimpleDateFormat // Import SimpleDateFormat
import java.util.Date // Import Date
import java.util.Locale // Import Locale

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    onLogout: () -> Unit,
    onEditProfile: (String) -> Unit
) {
    // Observe states from the ViewModel, providing initial values to help type inference
    val currentUser by profileViewModel.currentUser.collectAsState(initial = null)
    val isLoading by profileViewModel.isLoading.collectAsState(initial = false)
    val errorMessage by profileViewModel.errorMessage.collectAsState(initial = null)
    val userPosts by profileViewModel.userPosts.collectAsState(initial = emptyList()) // Ensure initial list type is correct
    val userProfilesForPosts by profileViewModel.userProfilesForPosts.collectAsState(initial = emptyMap()) // Ensure initial map type is correct
    val userLikedPostIds by profileViewModel.userLikedPostIds.collectAsState(initial = emptySet()) // Ensure initial set type is correct

    // Date and Decimal formats for displaying run data consistently
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val decimalFormat = DecimalFormat("#.##")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Changed to Top to allow scrolling for posts
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(50.dp))
            Text("Učitavanje profila i objava...")
        } else if (errorMessage != null) {
            Text("Greška: $errorMessage", color = MaterialTheme.colorScheme.error)
            Button(onClick = { profileViewModel.fetchUserProfileAndPosts() }) { // Call combined fetch
                Text("Pokušaj ponovo")
            }
        } else if (currentUser != null) {
            UserProfileContent(user = currentUser!!)
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

            Spacer(modifier = Modifier.height(24.dp)) // Spacer before posts section

            Text(
                text = "Moje objave",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center // Corrected: Use TextAlign.Center
            )

            // Check if userPosts is empty using the .isEmpty() extension function for List
            if (userPosts.isEmpty()) {
                Text("Još nema objava.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(userPosts, key = { it.id }) { post -> // 'it.id' now correctly refers to RunPost.id
                        RunPostCard(
                            post = post,
                            user = userProfilesForPosts[post.userId], // 'post.userId' now correctly refers to RunPost.userId
                            dateFormat = dateFormat,
                            decimalFormat = decimalFormat,
                            onLikeClick = { postId, isLiked -> profileViewModel.toggleLike(postId, isLiked) }, // 'toggleLike' resolved
                            isLiked = userLikedPostIds.contains(post.id) // 'post.id' resolved
                        )
                    }
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

@Composable
fun UserProfileContent(user: User) {
    val decimalFormat = DecimalFormat("#.##")
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    Image(
        painter = if (user.profileImageUrl != null && user.profileImageUrl.isNotEmpty()) {
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
    Text(text = "Ime: ${user.displayName.ifEmpty { "N/A" }}")
    Text(text = "Email: ${user.email}")
    user.age?.let { age ->
        Text(text = "Dob: $age")
    }
    Text(text = "Ukupna udaljenost: ${decimalFormat.format(user.totalDistanceRun)} km")
    Text(text = "Ukupno trčanja: ${user.totalRuns}")
    user.lastRunTimestamp?.let { timestamp ->
        Text(text = "Zadnje trčanje: ${dateFormat.format(Date(timestamp))}")
    }
    user.createdAt?.let { date ->
        Text(text = "Član od: ${dateFormat.format(date)}")
    }
}
