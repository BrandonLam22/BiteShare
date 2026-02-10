package org.example.biteshare.view

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.biteshare.viewmodel.HelpViewModel

class HelpView(
    private val vm: HelpViewModel,
    private val onBack: () -> Unit,
) {
    @Composable
    fun Content() {
        val s = vm.uiState

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Text("←", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Help",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(24.dp))

            // FAQ items
            Text(
                text = "How do I pick a restaurant?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tap the Pick tab, choose 'Me Only' or 'With My Friends', and press the Go button to get personalized recommendations.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "How do I save restaurants?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tap the heart icon on any restaurant card to save it for later. View all saved restaurants from your Profile.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Need more help?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Contact us at support@biteshare.com",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}