package org.example.biteshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.example.biteshare.app.AppRoot
import org.example.biteshare.app.PickMeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            PickMeTheme {
                AppRoot()
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    PickMeTheme {
        AppRoot()
    }
}