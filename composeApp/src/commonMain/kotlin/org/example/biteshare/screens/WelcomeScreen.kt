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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import biteshare.composeapp.generated.resources.Res
import biteshare.composeapp.generated.resources.welcome_screen_image

@Preview
@Composable
fun WelcomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize() // Take up the whole screen
            .background(Color.White), // sets the background to white
        horizontalAlignment = Alignment.CenterHorizontally // Center everything left-to-right
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Image(
            painter = painterResource(Res.drawable.welcome_screen_image),
            contentDescription = "Welcome screen image",
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp), // set how big it is
            contentScale = ContentScale.Fit // this makes the image fill the area nicely
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Welcome",
                color = Color(0xFFFF7A00), // Hex code for that orange color
                fontSize = 40.sp, // 'sp' is for text sizing
                fontWeight = FontWeight.Bold, // make it thick

            )

            Text(
                text = "Discover and review food places",
                color = Color(0xFFFF8C00),
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp) // Add space around it
            )

            Button(
                onClick = { /* Add navigation here later */},
                modifier = Modifier.fillMaxWidth(0.8f),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A00)) // fill it with orange
            ) {
                Text("Login", fontSize = 25.sp, fontWeight = FontWeight.Normal)
            }

            Spacer(modifier = Modifier.height(16.dp)) // Adds a little gap between the buttons

            OutlinedButton(
                onClick = { /* Sign up logic */},
                modifier = Modifier.fillMaxWidth(0.8f),
                shape = RoundedCornerShape(6.dp), // sharper corners
                border = BorderStroke(1.dp, Color(0xFFFF8C00)) // add a border around a component
            ) {
                Text("Sign Up", fontSize = 25.sp, color = Color(0xFFFF8C00), fontWeight = FontWeight.Normal)
            }
        }

    }

}

