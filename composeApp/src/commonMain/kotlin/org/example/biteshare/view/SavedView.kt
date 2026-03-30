package org.example.biteshare.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.viewmodel.SavedViewModel

class SavedView(
    private val vm: SavedViewModel,
    private val onBack: () -> Unit = {},
    private val onRestaurantClick: (Restaurant) -> Unit
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
                    text = "Saved",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp)) // Balance the back button
            }

            Spacer(Modifier.height(16.dp))

            // Toggle between Time and Name sorting
            SortToggle(
                selected = s.sortBy,
                onSelect = vm::setSortBy
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(s.savedRestaurants) { restaurant ->
                    SavedRestaurantCard(
                        category = restaurant.category,
                        name = restaurant.name,
                        price = restaurant.price,
                        eta = restaurant.eta,
                        rating = restaurant.rating,
                        onToggleSaved = { vm.toggleSaved(restaurant.id) },
                        onClick = { onRestaurantClick(restaurant) }
                    )
                }
            }
        }
    }

    @Composable
    private fun SortToggle(
        selected: String,
        onSelect: (String) -> Unit,
    ) {
        val shape = RoundedCornerShape(22.dp)
        Surface(
            shape = shape,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(6.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                /*SortPill(
                    text = "Time",
                    selected = selected == "time",
                    onClick = { onSelect("time") },
                    modifier = Modifier.weight(1f)
                )
                SortPill(
                    text = "Name",
                    selected = selected == "name",
                    onClick = { onSelect("name") },
                    modifier = Modifier.weight(1f)
                )*/
            }
        }
    }
/*
    @Composable
    private fun SortPill(
        text: String,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

        Surface(
            color = bg,
            contentColor = fg,
            shape = RoundedCornerShape(20.dp),
            modifier = modifier
                .height(40.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
*/
    @Composable
    private fun SavedRestaurantCard(
        category: String,
        name: String,
        price: String,
        eta: String,
        rating: Double,
        onToggleSaved: () -> Unit,
        onClick: () -> Unit,
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
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
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Text("♥", style = MaterialTheme.typography.titleLarge)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "$rating stars",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}