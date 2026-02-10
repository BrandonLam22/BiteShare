package org.example.biteshare.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.biteshare.viewmodel.RecommendsViewModel

class RecommendsView(
    private val vm: RecommendsViewModel,
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
            Text(
                text = s.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))

            s.items.forEach { r ->
                RestaurantCard(
                    category = r.category,
                    name = r.name,
                    price = r.price,
                    eta = r.eta,
                    rating = r.rating,
                    saved = r.isSaved,
                    onToggleSaved = { vm.toggleSaved(r.id) }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    @Composable
    private fun RestaurantCard(
        category: String,
        name: String,
        price: String,
        eta: String,
        rating: Double,
        saved: Boolean,
        onToggleSaved: () -> Unit,
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(category) },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    )

                    IconButton(
                        onClick = onToggleSaved,
                    ) {
                        Text(if (saved) "♥" else "♡")
                    }

                    Text(
                        text = "Image",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("$price  •  $eta", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("⭐ $rating", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
