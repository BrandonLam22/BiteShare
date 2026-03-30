package org.example.biteshare.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.biteshare.viewmodel.EditProfileViewModel
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.ui.text.input.ImeAction

class EditProfileView(
    private val vm: EditProfileViewModel,
    private val onBack: () -> Unit,
    private val onChangePassword: () -> Unit,
) {
    @Composable
    fun Content() {
        val s = vm.uiState

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(18.dp))

            // Header with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Text("←", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(32.dp))

            // Profile picture
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = s.username.firstOrNull()?.uppercase() ?: "U",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { /* TODO: Change photo */ }) {
                    Text("Change Photo")
                }
            }

            Spacer(Modifier.height(32.dp))

            // Username field
            OutlinedTextField(
                value = s.username,
                onValueChange = vm::updateUsername,
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Email field
            OutlinedTextField(
                value = s.email,
                onValueChange = vm::updateEmail,
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Bio field
            OutlinedTextField(
                value = s.bio,
                onValueChange = vm::updateBio,
                label = { Text("Bio") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )

            Spacer(Modifier.height(32.dp))

            OutlinedButton(
                onClick = onChangePassword,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Change Password", modifier = Modifier.padding(vertical = 8.dp))
            }

            Spacer(Modifier.height(32.dp))

            // Food Preferences Section
            PreferenceSection(
                title = "Food Preferences",
                subtitle = "Add cuisines or foods you enjoy",
                placeholder = "e.g., Pizza, Sushi, Japanese",
                inputValue = s.preferenceInput,
                onInputChange = vm::updatePreferenceInput,
                onAdd = vm::addPreference,
                showError = s.showPreferenceError,
                errorMessage = "Please enter a valid preference",
                items = s.preferences,
                onRemove = vm::removePreference,
                emptyMessage = "No preferences added yet"
            )

            Spacer(Modifier.height(32.dp))

            // Food Restrictions Section
            PreferenceSection(
                title = "Food Restrictions",
                subtitle = "Add dietary restrictions or allergies",
                placeholder = "e.g., Vegan, Gluten, Peanuts",
                inputValue = s.restrictionInput,
                onInputChange = vm::updateRestrictionInput,
                onAdd = vm::addRestriction,
                showError = s.showRestrictionError,
                errorMessage = "Please enter a valid restriction",
                items = s.foodRestrictions,
                onRemove = vm::removeRestriction,
                emptyMessage = "No restrictions added"
            )

            Spacer(Modifier.height(32.dp))

            s.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
            }

            // Save button
            Button(
                onClick = { vm.saveProfile(onSuccess = onBack) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = s.hasChanges && !s.isSaving
            ) {
                Text(
                    text = if (s.isSaving) "Saving..." else "Save Changes",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Cancel button
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", modifier = Modifier.padding(vertical = 8.dp))
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    @Composable
    private fun PreferenceSection(
        title: String,
        subtitle: String,
        placeholder: String,
        inputValue: String,
        onInputChange: (String) -> Unit,
        onAdd: () -> Unit,
        showError: Boolean,
        errorMessage: String,
        items: Set<String>,
        onRemove: (String) -> Unit,
        emptyMessage: String
    ) {
        Column {
            // Title and subtitle
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            // Input field with add button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = onInputChange,
                    label = { Text(placeholder) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    isError = showError,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onAdd() }
                    )
                )

                IconButton(
                    onClick = onAdd,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(48.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "+",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            // Error message
            if (showError) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Display added items
            if (items.isEmpty()) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { item ->
                        InputChip(
                            selected = true,
                            onClick = { onRemove(item) },
                            label = { Text(item) },
                            trailingIcon = {
                                Text(
                                    "✕",  // CHANGED: Use × symbol instead of Icon
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}