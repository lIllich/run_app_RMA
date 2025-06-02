package com.example.run_app_rma.presentation.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.run_app_rma.R
import com.example.run_app_rma.data.firestore.repository.UserRepository // Uvezi UserRepository
import com.google.firebase.auth.FirebaseAuth // Uvezi FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore // Uvezi FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage // Uvezi FirebaseStorage
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    modifier: Modifier = Modifier,
    userId: String,
    onProfileUpdated: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val editProfileViewModel: EditProfileViewModel = viewModel(
        factory = EditProfileViewModel.Factory(
            userRepository = UserRepository(FirebaseFirestore.getInstance()),
            firebaseAuth = FirebaseAuth.getInstance(),
            firebaseStorage = FirebaseStorage.getInstance()
        )
    )

    val currentUser by editProfileViewModel.currentUser.collectAsState()
    val isLoading by editProfileViewModel.isLoading.collectAsState()
    val errorMessage by editProfileViewModel.errorMessage.collectAsState()
    val successMessage by editProfileViewModel.successMessage.collectAsState()

    var displayName by remember { mutableStateOf("") }
    var ageString by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) } // Trenutni URL slike s Firestorea

    // Inicijalizacija polja kada se korisnik učita
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            displayName = currentUser!!.displayName
            ageString = currentUser!!.age?.toString() ?: ""
            profileImageUrl = currentUser!!.profileImageUrl
        }
    }

    // Prikaz Toast poruka
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            editProfileViewModel.clearMessages()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            editProfileViewModel.clearMessages()
            onProfileUpdated() // Obavijesti da je profil ažuriran
            onBack() // Vrati se natrag nakon uspješnog ažuriranja
        }
    }

    // Launcheri za odabir slike
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            // Convert bitmap to URI for consistency, this requires saving to a temporary file
            // For simplicity, for now, we'll assume a direct URI for Gallery and handle bitmap separately if needed.
            // A more robust solution would save the bitmap to a temp file and get its URI.
            // For this example, let's keep it simpler.
            // If you want to handle bitmap, you'd need a utility function to save it and get a Uri.
            // For now, let's just use the Gallery picker or a more advanced camera intent that gives a Uri.
            Toast.makeText(context, "Camera input not fully implemented for URI handling in this example.", Toast.LENGTH_SHORT).show()
            // As a placeholder, if you get a URI from camera: selectedImageUri = camera_uri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Uredi Profil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Slika profila
            Image(
                painter = if (selectedImageUri != null) {
                    rememberAsyncImagePainter(selectedImageUri)
                } else if (profileImageUrl != null && profileImageUrl!!.isNotEmpty()) {
                    rememberAsyncImagePainter(profileImageUrl)
                } else {
                    painterResource(R.drawable.ic_profile_placeholder)
                },
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable { /* Ovdje otvori BottomSheet ili Dialog za odabir metode */ },
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = {
                // Ovdje bi se otvarao BottomSheet ili Dialog za odabir izvora slike
                // Za demo, direktno pokrećem galeriju.
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Text("Promijeni sliku")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Icon(Icons.Default.Image, contentDescription = "Odaberi iz galerije")
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = {
                    // Za kameru je potrebno HANDLEIRATI PRIVREMENI URI
                    // Ovo je samo primjer, za potpunu funkcionalnost kamere treba više koda.
                    // npr. takePicture.launch(createImageUri(context))
                    Toast.makeText(context, "Kamera funkcionalnost zahtijeva dodatnu implementaciju URI-ja.", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Snimi kamerom")
                }
            }


            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Ime") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = ageString,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                        ageString = newValue
                    }
                },
                label = { Text("Dob (neobavezno)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val age = ageString.toIntOrNull()
                    editProfileViewModel.updateUserProfile(
                        userId = userId,
                        displayName = displayName,
                        age = age,
                        profileImageUri = selectedImageUri
                    )
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Spremi promjene")
                }
            }
        }
    }
}

/*
// Helper function to create a temporary URI for camera output
// Requires permissions WRITE_EXTERNAL_STORAGE (Android < Q) and FILE_PROVIDER setup
// For Android Q+, use MediaStore.createWriteRequest
fun createImageUri(context: Context): Uri {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "temp_image_${System.currentTimeMillis()}.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RunAppRMA")
        }
    }
    return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?: throw IOException("Failed to create new MediaStore record.")
}
*/