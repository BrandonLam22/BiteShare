package org.example.biteshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.example.biteshare.app.AppRoot
import org.example.biteshare.app.AuthGate
import org.example.biteshare.app.BiteShareTheme
import org.example.biteshare.domain.Model  // ADD THIS IMPORT

class MainActivity : ComponentActivity() {
    private val appModel = Model()  // ADD: Create Model once at app level

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            BiteShareTheme {
                AuthGate(model = appModel)  // CHANGED: Pass model to AuthGate
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    BiteShareTheme {
        AppRoot(model = Model())  // CHANGED: Pass model to AppRoot for preview
    }
}