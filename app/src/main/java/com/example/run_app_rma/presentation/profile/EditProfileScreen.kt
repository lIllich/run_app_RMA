package com.example.run_app_rma.presentation.profile

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }    // temporary URI for camera output

    // initialize state with current user data when available
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            displayName = user.displayName.ifEmpty { "" }
            age = user.age?.toString().orEmpty()
            // only set selectedImageUri from network if no local image has been picked yet
            if (selectedImageUri == null && !user.profileImageUrl.isNullOrEmpty()) {
                selectedImageUri = user.profileImageUrl.toUri()
            }
        }
    }

    // observe error and success messages
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
                // no image selected or URI is null (example: user cancelled)
                Toast.makeText(context, "Nema odabrane slike.", Toast.LENGTH_SHORT).show()
                selectedImageUri = null
            }
        }
    )

    // launcher for taking a picture with the camera
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                selectedImageUri = tempCameraUri    // if picture taken successfully, use the temporary URI
            } else {
                // camera capture failed or user cancelled
                Toast.makeText(context, "Snimanje slike neuspješno.", Toast.LENGTH_SHORT).show()
                tempCameraUri = null // clear temp URI if failed
                selectedImageUri = null // also clear selectedImageUri
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Uredi profil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Natrag")
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
            // profile image
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val painter = if (selectedImageUri != null && selectedImageUri.toString().isNotEmpty()) {
                    rememberAsyncImagePainter(selectedImageUri)
                } else {
                    painterResource(R.drawable.ic_profile_placeholder)
                }
                Image(
                    painter = painter,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // row with camera and gallery icons
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

            // name input
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Ime") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            // age input
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

            // save changes button
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


// helper function to create a temporary URI for camera output
fun createImageUri(context: Context): Uri? {
    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "JPEG_${name}.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/RunAppRMA")
    }
    return try {
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}