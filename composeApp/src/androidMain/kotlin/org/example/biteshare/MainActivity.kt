package org.example.biteshare

import android.net.http.HttpResponseCache.install
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
    private val appModel = Model()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            BiteShareTheme {
                AuthGate(model = appModel)
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    BiteShareTheme {
        AppRoot(model = Model())
    }
}
