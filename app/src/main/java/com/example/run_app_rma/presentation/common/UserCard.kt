package com.example.run_app_rma.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults // Import ButtonDefaults for custom colors
import androidx.compose.material3.CircularProgressIndicator // Import CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.run_app_rma.R
import com.example.run_app_rma.data.firestore.model.User

/**
 * Reusable Composable for displaying a user's profile image and display name.
 *
 * @param user The User object to display.
 * @param modifier Modifier for this composable.
 * @param onClick Optional lambda to be invoked when the card is clicked.
 * @param showFollowButton Boolean flag to control the visibility of the follow button. Default is false.
 * @param isFollowing Boolean indicating if the current user is following this user. Only relevant if showFollowButton is true.
 * @param onToggleFollow Lambda to be invoked when the follow button is clicked.
 * Receives the userId and the current isFollowing status.
 * @param isTogglingFollow Boolean indicating if the follow/unfollow action for this specific user is in progress.
 */
@Composable
fun UserCard(
    user: User,
    modifier: Modifier = Modifier,
    onClick: ((String) -> Unit)? = null,
    showFollowButton: Boolean = false,
    isFollowing: Boolean = false,
    onToggleFollow: ((String, Boolean) -> Unit)? = null,
    isTogglingFollow: Boolean = false // New parameter for individual toggle loading
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null && !isTogglingFollow) { onClick?.invoke(user.id) } // Disable click when toggling
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = if (user.profileImageUrl != null && user.profileImageUrl.isNotEmpty()) {
                rememberAsyncImagePainter(user.profileImageUrl)
            } else {
                painterResource(R.drawable.ic_profile_placeholder) // Default placeholder
            },
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = user.displayName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f) // Allow text to take available space
        )

        // Show small CircularProgressIndicator when toggling follow
        if (isTogglingFollow) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp), // Small size for the indicator
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp)) // Space between indicator and button/end
        }

        // Conditional Follow Button
        if (showFollowButton && !isTogglingFollow) { // Hide button when loading
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onToggleFollow?.invoke(user.id, isFollowing) },
                enabled = onToggleFollow != null && !isTogglingFollow, // Button is enabled only if a toggle action is provided and not loading
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                    contentColor = if (isFollowing) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if (isFollowing) "Pratim" else "Prati")
            }
        }
    }
}
