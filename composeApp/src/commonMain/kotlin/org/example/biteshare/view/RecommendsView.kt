package org.example.biteshare.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.example.biteshare.viewmodel.RecommendsViewModel
import org.jetbrains.compose.resources.painterResource

import biteshare.composeapp.generated.resources.Res
import biteshare.composeapp.generated.resources.compose_multiplatform
import biteshare.composeapp.generated.resources.burgers
import biteshare.composeapp.generated.resources.coffee
import biteshare.composeapp.generated.resources.pizza
import biteshare.composeapp.generated.resources.sushi

class RecommendsView(
    private val vm: RecommendsViewModel,
    private val onBack: () -> Unit = {},
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
                    text = s.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp)) // balance back button width
            }
            Spacer(Modifier.height(12.dp))

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
        val imageRes = when (category.lowercase()) {
            "pizza" -> Res.drawable.pizza
            "sushi" -> Res.drawable.sushi
            "burgers" -> Res.drawable.burgers
            "coffee" -> Res.drawable.coffee
            else -> Res.drawable.compose_multiplatform
        }

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
                    Image(
                        painter = painterResource(imageRes),
                        contentDescription = "$name photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    AssistChip(
                        onClick = {},
                        label = { Text(category) },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    )

                    IconButton(
                        onClick = onToggleSaved,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Text(if (saved) "♥" else "♡")
                    }
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
