package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.User
import com.example.ui.theme.IgBlue

@Composable
fun EditProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (username: String, fullName: String, bio: String, link: String) -> Unit
) {
    var username by remember(user) { mutableStateOf(user.username) }
    var fullName by remember(user) { mutableStateOf(user.fullName) }
    var bio by remember(user) { mutableStateOf(user.bio) }
    var link by remember(user) { mutableStateOf(user.link) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit profile",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; validationError = null },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_profile_name_input")
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; validationError = null },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_profile_username_input")
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it; validationError = null },
                    label = { Text("Bio") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_profile_bio_input")
                )
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it; validationError = null },
                    label = { Text("Link") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_profile_link_input")
                )
                validationError?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedUsername = username.trim()
                    val trimmedName = fullName.trim()
                    when {
                        trimmedName.isEmpty() || trimmedName.length > 60 -> {
                            validationError = "Name must be 1 to 60 characters."
                        }
                        trimmedUsername.length < 2 || trimmedUsername.length > 30 -> {
                            validationError = "Username must be 2 to 30 characters."
                        }
                        bio.length > 250 -> {
                            validationError = "Bio cannot exceed 250 characters."
                        }
                        link.length > 120 -> {
                            validationError = "Link cannot exceed 120 characters."
                        }
                        else -> {
                            onSave(trimmedUsername, trimmedName, bio.trim(), link.trim())
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IgBlue,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("edit_profile_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
