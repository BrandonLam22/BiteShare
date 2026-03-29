package org.example.biteshare.view

import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.biteshare.components.LoginSignupButton
import org.example.biteshare.components.WelcomeScreenImage
import org.example.biteshare.components.TextButton
import org.example.biteshare.viewmodel.LoginViewModel

// @Preview
@Composable
fun LoginView(viewModel: LoginViewModel,
              onLoginSuccess: () -> Unit,
              onNavigateToSignup: () -> Unit) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize() // take up the whole screen
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
            .background(Color.White), // set the background color to white
        horizontalAlignment = Alignment.CenterHorizontally, // center the title and buttons
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        WelcomeScreenImage()

        Column(modifier = Modifier.padding(horizontal = 15.dp)) {
            Text(
                text = "Login",
                color = Color(0xFFFF7A00), // Hex code for that orange color
                fontSize = 35.sp, // 'sp' is for text sizing
                fontWeight = FontWeight.Bold, // make it thick
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            OutlinedTextField(
                value = viewModel.username, // Read from ViewModel
                onValueChange = { viewModel.username = it }, // 'it' is the new text typed by the user, write to ViewModel
                label = {
                    Text(
                        text = "Username",
                        fontSize = 16.sp,
                        color = Color.Gray,
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth(0.9f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF7A00),
                    unfocusedBorderColor = Color(0xFFFF8C00),
                )
            )

            OutlinedTextField(
                value = viewModel.password,
                onValueChange = { viewModel.password = it },
                label = {
                    Text(
                        text = "Password",
                        fontSize = 16.sp,
                        color = Color.Gray,
                    )
                },
                visualTransformation = PasswordVisualTransformation(), // hide the text
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF7A00),
                    unfocusedBorderColor = Color(0xFFFF8C00),
                )
            )

            TextButton(
                onClick = { /* add navigation to the "reset" screen later */ },
                modifier = Modifier
                    .fillMaxWidth(0.9f) // keep it the same width as the input boxes
                    .wrapContentWidth(Alignment.End), // push the button to the right
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "forgot password",
                    color = Color(0xFFFF8C00),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            LoginSignupButton(
                text = if (viewModel.isLoading) "Logging in..." else "Login",
                onClick = {
                    if (!viewModel.isLoading) { // View calls function in ViewModel
                        viewModel.onLoginClicked(onLoginSuccess)
                    }
                }
            )

            viewModel.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    color = Color.Red,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(25.dp))
        }

        Row(
            modifier = Modifier.padding(top = 20.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Part 1: the plain gray text
            Text(
                text = "Don't have an account? ",
                color = Color.Gray,
                fontSize = 14.sp
            )

            TextButton(text = "Sign Up", onClick = { onNavigateToSignup() })
        }

    }
}

