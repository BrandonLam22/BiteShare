package org.example.biteshare.view


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.example.biteshare.viewmodel.ChangePasswordViewModel

class ChangePasswordView(
    private val vm: ChangePasswordViewModel,
    private val onBack: () -> Unit,
) {
    @Composable
    fun Content() {
        val s = vm.uiState

        LaunchedEffect(s.isSaved) {
            if (s.isSaved) onBack()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(18.dp))

            // Header
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Text("←", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.weight(1f))
                Text("Change Password", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(32.dp))

            PasswordField(
                value = s.currentPassword,
                onValueChange = vm::updateCurrentPassword,
                label = "Current Password",
                error = s.currentPasswordError
            )
            Spacer(Modifier.height(16.dp))
            PasswordField(
                value = s.newPassword,
                onValueChange = vm::updateNewPassword,
                label = "New Password",
                error = s.newPasswordError
            )
            Spacer(Modifier.height(16.dp))
            PasswordField(
                value = s.confirmPassword,
                onValueChange = vm::updateConfirmPassword,
                label = "Confirm New Password",
                error = s.confirmPasswordError
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { vm.save() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Password", modifier = Modifier.padding(vertical = 8.dp))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }

    @Composable
    private fun PasswordField(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        error: String?
    ) {
        Column {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = error != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
    }
}