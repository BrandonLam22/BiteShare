package org.example.biteshare.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.biteshare.components.LoginSignupButton
import org.example.biteshare.components.WelcomeScreenImage
import org.example.biteshare.components.TextButton

@Preview
@Composable
fun SignupView(viewModel: SignupViewModel = SignupViewModel()) {
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
                        fontSize = 20.sp,
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
                        fontSize = 20.sp,
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
                        fontSize = 20.sp,
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

            LoginSignupButton(
                text = "Sign Up",
                onClick = { viewModel.onSignupClicked() } // Call function in ViewModel
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

            TextButton(text = "Login", onClick = { /* We will navigate to the Sign Up screen here */ })
        }

    }
}

