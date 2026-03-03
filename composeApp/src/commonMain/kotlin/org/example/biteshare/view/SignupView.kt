package org.example.biteshare.view

import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.biteshare.components.LoginSignupButton
import org.example.biteshare.components.WelcomeScreenImage
import org.example.biteshare.components.TextButton
import org.example.biteshare.viewmodel.SignupViewModel

// @Preview
@Composable
fun SignupView(viewModel: SignupViewModel,
               onSignupSuccess: () -> Unit,
               onNavigateToLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize() // take up the whole screen
            .background(Color.White), // set the background color to white
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        WelcomeScreenImage()

        Column(modifier = Modifier.padding(horizontal = 15.dp)) {
            Text(
                text = "Sign Up",
                color = Color(0xFFFF7A00), // Hex code for that orange color
                fontSize = 35.sp, // 'sp' is for text sizing
                fontWeight = FontWeight.Bold, // make it thick
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

//            var username by remember { mutableStateOf("") }
//            var password by remember { mutableStateOf("") }
//            var email by remember { mutableStateOf("") }

            OutlinedTextField(
                value = viewModel.username,
                onValueChange = { viewModel.username = it }, // 'it' is the new text typed by the user
                label = {
                    Text(
                        text = "Username",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(55.dp),
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
                        color = Color.Gray
                    )
                },
                visualTransformation = PasswordVisualTransformation(), // hide the text
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 5.dp)
                    .height(55.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF7A00),
                    unfocusedBorderColor = Color(0xFFFF8C00),
                )
            )

            OutlinedTextField(
                value = viewModel.email,
                onValueChange = { viewModel.email = it },
                label = {
                    Text(
                        text = "Email",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                },
                visualTransformation = PasswordVisualTransformation(), // hide the text
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 5.dp)
                    .height(55.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF7A00),
                    unfocusedBorderColor = Color(0xFFFF8C00),
                )
            )

            Spacer(modifier = Modifier.height(30.dp))

            LoginSignupButton(
                text = "Sign Up",
                onClick = {
                    if (viewModel.onSignupClicked()) { // Call function in ViewModel
                        onSignupSuccess()
                    }
                }
            )

        }

        Row(
            modifier = Modifier.padding(top = 20.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Part 1: the plain gray text
            Text(
                text = "Already have an account? ",
                color = Color.Gray,
                fontSize = 14.sp
            )

            TextButton(text = "Login", onClick = { onNavigateToLogin() })
        }

    }
}
