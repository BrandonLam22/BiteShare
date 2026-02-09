package org.example.biteshare.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.painterResource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import biteshare.composeapp.generated.resources.Res
import biteshare.composeapp.generated.resources.welcome_screen_image
import org.example.biteshare.components.LoginSignupButton
import org.example.biteshare.components.WelcomeScreenImage
import org.example.biteshare.components.TextButton

@Preview
@Composable
fun LoginScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize() // take up the whole screen
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

            var username by remember { mutableStateOf("") } // tells Compose to save this value in the phone's memory even when the UI refreshes
            var password by remember { mutableStateOf("") }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it }, // 'it' is the new text typed by the user
                label = {
                    Text(
                        text = "Username",
                        fontSize = 20.sp,
                        color = Color.Gray,
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
                value = password,
                onValueChange = { password = it },
                label = {
                    Text(
                        text = "Password",
                        fontSize = 20.sp,
                        color = Color.Gray,
                    )
                },
                visualTransformation = PasswordVisualTransformation(), // hide the text
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 10.dp)
                    .height(55.dp),
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

            LoginSignupButton(text = "Login", onClick = { /* Add navigation here later */ })

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

            TextButton(text = "Sign Up", onClick = { /* We will navigate to the Sign Up screen here */ })
        }

    }
}