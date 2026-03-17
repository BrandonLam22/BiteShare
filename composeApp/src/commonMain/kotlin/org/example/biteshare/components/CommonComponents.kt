package org.example.biteshare.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import biteshare.composeapp.generated.resources.Res
import biteshare.composeapp.generated.resources.welcome_screen_image
import org.jetbrains.compose.resources.painterResource

@Composable
fun WelcomeScreenImage() {
    Image(
        painter = painterResource(Res.drawable.welcome_screen_image),
        contentDescription = "Welcome screen image",
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp), // set how big it is
        contentScale = ContentScale.Fit // this makes the image fill the area nicely
    )
}

@Composable
fun LoginSignupButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(0.9f),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A00)) // fill it with orange
    ) {
        Text(text, fontSize = 25.sp, fontWeight = FontWeight.Normal)
    }
}

@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFFFF8C00),
        )
    }
}
