package com.example.run_app_rma.presentation.profile

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image // Import for gallery icon
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
import com.example.run_app_rma.data.firestore.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    userId: String,
    onProfileUpdated: () -> Unit,
    onBack: () -> Unit,
    editProfileViewModel: EditProfileViewModel = viewModel(
        factory = EditProfileViewModel.Factory(
            userRepository = UserRepository(FirebaseFirestore.getInstance()),
            firebaseAuth = FirebaseAuth.getInstance(),
            firebaseStorage = FirebaseStorage.getInstance()
        )
    )
) {
    val context = LocalContext.current
    val currentUser by editProfileViewModel.currentUser.collectAsState()
    val isLoading by editProfileViewModel.isLoading.collectAsState()
    val errorMessage by editProfileViewModel.errorMessage.collectAsState()
    val successMessage by editProfileViewModel.successMessage.collectAsState()

    var displayName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf<String>("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) } // Temporary URI for camera output

    // Initialize state with current user data when available
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            displayName = user.displayName.ifEmpty { "" }
            age = user.age?.toString().orEmpty()
            // Only set selectedImageUri from network if no local image has been picked yet
            if (selectedImageUri == null && user.profileImageUrl != null && user.profileImageUrl.isNotEmpty()) {
                selectedImageUri = Uri.parse(user.profileImageUrl)
            }
        }
    }

    // Observe error and success messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            editProfileViewModel.clearMessages()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            editProfileViewModel.clearMessages()
            onProfileUpdated()
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedImageUri = uri
            } else {
                // No image selected or URI is null (e.g., user cancelled)
                Toast.makeText(context, "Nema odabrane slike.", Toast.LENGTH_SHORT).show()
                selectedImageUri = null // Ensure it's cleared if no image is picked
            }
        }
    )

    // Launcher for taking a picture with the camera
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                selectedImageUri = tempCameraUri // If picture taken successfully, use the temporary URI
            } else {
                // Camera capture failed or user cancelled
                Toast.makeText(context, "Snimanje slike neuspješno.", Toast.LENGTH_SHORT).show()
                tempCameraUri = null // Clear temp URI if failed
                selectedImageUri = null // Also clear selectedImageUri if using tempCameraUri directly
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Uredi profil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Natrag")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Image
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable { /* No direct click on image anymore to show dialog */ },
                contentAlignment = Alignment.Center
            ) {
                val painter = if (selectedImageUri != null && selectedImageUri.toString().isNotEmpty()) {
                    rememberAsyncImagePainter(selectedImageUri)
                } else {
                    painterResource(R.drawable.ic_profile_placeholder) // Default placeholder
                }
                Image(
                    painter = painter,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // The camera icon overlay is removed as separate buttons will handle this
            }

            // Row with camera and gallery icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = {
                    pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Icon(Icons.Default.Image, contentDescription = "Odaberi iz galerije")
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = {
                    val newUri = createImageUri(context)
                    if (newUri != null) {
                        tempCameraUri = newUri
                        takePictureLauncher.launch(newUri)
                    } else {
                        Toast.makeText(context, "Nije moguće stvoriti datoteku za sliku.", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Snimi kamerom")
                }
            }
            // End of new Row with icons

            // Display Name Input
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Ime") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            // Age Input
            OutlinedTextField(
                value = age,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                        age = newValue
                    }
                },
                label = { Text("Dob") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Changes Button
            Button(
                onClick = {
                    val ageInt = age.toIntOrNull()
                    editProfileViewModel.updateUserProfile(
                        userId = userId,
                        displayName = displayName,
                        age = ageInt,
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


// Helper function to create a temporary URI for camera output
fun createImageUri(context: Context): Uri? {
    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "JPEG_${name}.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/RunAppRMA")
        }
    }
    return try {
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}