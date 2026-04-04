package org.example.biteshare.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.RestaurantClassification
import org.example.biteshare.viewmodel.RecommendsViewModel
import org.jetbrains.compose.resources.painterResource

class RecommendsView(
    private val vm: RecommendsViewModel,
    private val onBack: () -> Unit = {},
    private val actionLabel: String? = null,
    private val onActionClick: (() -> Unit)? = null,
    private val onShuffle: (() -> Unit)? = null,
    private val onRestaurantClick: (Restaurant) -> Unit = {},
) {
    @Composable
    fun Content() {
        val s = vm.uiState
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
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

            if (actionLabel != null && onActionClick != null && s.items.isNotEmpty()) {
                if (onShuffle != null && s.canShuffle) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onShuffle,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Shuffle")
                        }
                        Button(
                            onClick = onActionClick,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(actionLabel)
                        }
                    }
                } else {
                    Button(
                        onClick = onActionClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(actionLabel)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (s.items.isEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "No recommendations match your current filters.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Go back and relax one or two filters.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                s.items.forEach { r ->
                    RestaurantCard(
                        restaurant = r,
                        onToggleSaved = { vm.toggleSaved(r.id) },
                        onClick = { onRestaurantClick(r) },
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

        }
    }

    @Composable
    private fun RestaurantCard(
        restaurant: Restaurant,
        onToggleSaved: () -> Unit,
        onClick: () -> Unit,
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                ) {
                    when {
                        !restaurant.imageUrl.isNullOrBlank() -> {
                            val imageUrl = restaurant.imageUrl.orEmpty()
                            RestaurantDetailImage(
                                imageRef = imageUrl,
                                contentDescription = restaurant.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        else -> {
                            val res = imageResByKey(
                                RestaurantClassification.categoryLabelToImageKey(restaurant.category),
                            )
                            if (res != null) {
                                Image(
                                    painter = painterResource(res),
                                    contentDescription = restaurant.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("🍴", style = MaterialTheme.typography.displayMedium)
                                }
                            }
                        }
                    }

                    AssistChip(
                        onClick = {},
                        label = { Text(restaurant.category) },
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
                        Text(if (restaurant.isSaved) "♥" else "♡")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(restaurant.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        val meta = joinInfo("  •  ", restaurant.price, etaLabel(restaurant.eta))
                        if (meta.isNotBlank()) {
                            Text(meta, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text("⭐ ${restaurant.rating}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
