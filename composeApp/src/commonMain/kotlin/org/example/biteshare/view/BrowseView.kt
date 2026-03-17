package org.example.biteshare.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.viewmodel.BrowseViewModel

class BrowseView(
    private val vm: BrowseViewModel,
    private val onBack: () -> Unit,
    private val onRestaurantClick: (String) -> Unit,
) {

    @Composable
    fun Content() {
        val s = vm.uiState

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("← Back")
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Browse",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = s.searchQuery,
                onValueChange = vm::updateSearchQuery,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Search restaurant, type, or location") },
                leadingIcon = { Text("🔍") },
                trailingIcon = {
                    if (s.searchQuery.isNotBlank()) {
                        TextButton(onClick = vm::clearSearch) {
                            Text("Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            // Top Addis Food Places header
            Text(
                text = s.headerTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (s.activeTag != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Filtered by: ${s.activeTag}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⭐", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${s.resultCount} results",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (s.activeTag != null) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = vm::clearTagFilter) {
                        Text("Clear")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Restaurant list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (s.restaurants.isEmpty()) {
                    item {
                        Text(
                            text = if (s.searchQuery.isNotBlank()) {
                                "No restaurants found for \"${s.searchQuery}\"."
                            } else {
                                "No restaurants found for this tag."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
                items(s.restaurants, key = { it.id }) { restaurant ->
                    RestaurantCard(
                        restaurant = restaurant,
                        searchQuery = s.searchQuery,
                        onClick = { onRestaurantClick(restaurant.id) }
                    )
                }
            }
        }
    }

    @Composable
    private fun RestaurantCard(
        restaurant: Restaurant,
        searchQuery: String,
        onClick: () -> Unit,
    ) {
        val query = searchQuery.trim().lowercase()
        val directMatch = query.isBlank() ||
            restaurant.name.lowercase().contains(query) ||
            restaurant.category.lowercase().contains(query) ||
            restaurant.location.lowercase().contains(query)

        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (directMatch) 1f else 0.82f)
        ) {
            Column(modifier = Modifier.padding(0.dp)) {
                // Cover image area - clickable to go to Detail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clickable(onClick = onClick)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    // Popular tag (top-left)
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "Popular",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    // Heart icon (top-right)
                    Text(
                        text = if (restaurant.isSaved) "❤️" else "🤍",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    )
                    // Placeholder image content
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text("🍴", style = MaterialTheme.typography.displayMedium)
                    }
                }
                // Info row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = highlightText(
                            fullText = restaurant.name,
                            query = searchQuery,
                            highlightColor = MaterialTheme.colorScheme.primary
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = highlightText(
                            fullText = "${restaurant.category} • ${restaurant.location}",
                            query = searchQuery,
                            highlightColor = MaterialTheme.colorScheme.tertiary
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (searchQuery.isNotBlank() && !directMatch) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Matched by tags/details",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = restaurant.price,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = restaurant.eta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${restaurant.rating} stars",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    private fun highlightText(
        fullText: String,
        query: String,
        highlightColor: Color,
    ) = buildAnnotatedString {
        if (query.isBlank()) {
            append(fullText)
            return@buildAnnotatedString
        }
        val source = fullText
        val sourceLower = source.lowercase()
        val needle = query.trim().lowercase()
        if (needle.isBlank()) {
            append(source)
            return@buildAnnotatedString
        }

        var cursor = 0
        while (cursor < source.length) {
            val idx = sourceLower.indexOf(needle, startIndex = cursor)
            if (idx < 0) {
                append(source.substring(cursor))
                break
            }
            if (idx > cursor) {
                append(source.substring(cursor, idx))
            }
            withStyle(
                style = SpanStyle(
                    color = highlightColor,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(source.substring(idx, idx + needle.length))
            }
            cursor = idx + needle.length
        }
    }
}
