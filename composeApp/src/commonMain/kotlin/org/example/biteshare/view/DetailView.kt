package org.example.biteshare.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.example.biteshare.viewmodel.DetailViewModel
import org.example.biteshare.viewmodel.MealTab
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import biteshare.composeapp.generated.resources.Res
import biteshare.composeapp.generated.resources.beyayenet_home
import biteshare.composeapp.generated.resources.breakfast_home
import biteshare.composeapp.generated.resources.capo_chino_home
import biteshare.composeapp.generated.resources.chesse_burger_home
import biteshare.composeapp.generated.resources.coffee_home
import biteshare.composeapp.generated.resources.drink_home
import biteshare.composeapp.generated.resources.fast_food_home
import biteshare.composeapp.generated.resources.local_home
import biteshare.composeapp.generated.resources.milkshake_home
import biteshare.composeapp.generated.resources.pizza_home

class DetailView(
    private val vm: DetailViewModel,
    private val onBack: () -> Unit,
) {

    @Composable
    fun Content() {
        val s = vm.uiState
        val detail = s.restaurantDetail
        val uriHandler = LocalUriHandler.current
        val sectionSpacing = 20.dp
        val itemSpacing = 10.dp
        var selectedImageIndex by remember(detail?.restaurantId) { mutableStateOf(0) }
        val restaurant = s.restaurant ?: run {
            Text("Loading...", modifier = Modifier.padding(24.dp))
            return
        }
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Top bar: back, title, heart, search
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Text("←", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                IconButton(onClick = { vm.toggleSaved() }) {
                    Text(
                        text = if (restaurant.isSaved) "❤️" else "🤍",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                IconButton(onClick = { }) {
                    Text("🔍", style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Large restaurant image with thumbnail selector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val imageKeys = detail?.images.orEmpty()
                val selectedKey = imageKeys.getOrNull(selectedImageIndex)
                val selectedImageRes = selectedKey?.let(::imageResByKey)
                if (selectedImageRes != null) {
                    Image(
                        painter = painterResource(selectedImageRes),
                        contentDescription = "${restaurant.name} image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text("🍴", style = MaterialTheme.typography.displayLarge)
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                if (!detail?.images.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        detail!!.images.forEachIndexed { index, key ->
                            val thumbRes = imageResByKey(key)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (index == selectedImageIndex) 2.dp else 1.dp,
                                    color = if (index == selectedImageIndex) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                                modifier = Modifier
                                    .size(64.dp)
                                    .clickable { selectedImageIndex = index }
                            ) {
                                if (thumbRes != null) {
                                    Image(
                                        painter = painterResource(thumbRes),
                                        contentDescription = "${restaurant.name} thumbnail ${index + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text("🍽️")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(sectionSpacing))
                }

                // Restaurant name
                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                // Rating + reviews
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("⭐", style = MaterialTheme.typography.bodyMedium)
                    val displayRating = if (s.reviewCount > 0) s.averageReviewScore else restaurant.rating
                    Text(
                        text = "${displayRating.formatOneDecimal()} • ${s.reviewCount} reviews",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(itemSpacing))
                // Location, price, distance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = s.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = s.priceRange,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = s.distance,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(itemSpacing))

                Text(
                    text = detail?.location?.address ?: "Address not available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(itemSpacing))

                Text(
                    text = detail?.description ?: "No description available.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(Modifier.height(sectionSpacing))

                if (!detail?.attributes.isNullOrEmpty()) {
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(itemSpacing))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        detail!!.attributes.forEach { attr ->
                            AssistChip(
                                onClick = {},
                                label = { Text(attr) }
                            )
                        }
                    }
                    Spacer(Modifier.height(sectionSpacing))
                }

                if (s.popularReviewTags.isNotEmpty()) {
                    Text(
                        text = "Popular Review Tags",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(itemSpacing))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        s.popularReviewTags.forEach { tag ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(tag) }
                            )
                        }
                    }
                    Spacer(Modifier.height(sectionSpacing))
                }

                if (!detail?.featuredItems.isNullOrEmpty()) {
                    Text(
                        text = "Featured Items",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(itemSpacing))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        detail!!.featuredItems.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = item.price,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (item.description.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(sectionSpacing))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            detail?.googleMapsLink?.let(uriHandler::openUri)
                        },
                        enabled = !detail?.googleMapsLink.isNullOrBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open in Maps")
                    }
                    OutlinedButton(
                        onClick = {
                            detail?.restaurantWebsite?.let(uriHandler::openUri)
                        },
                        enabled = !detail?.restaurantWebsite.isNullOrBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Visit Website")
                    }
                }

                Spacer(Modifier.height(itemSpacing))

                Button(
                    onClick = {
                        detail?.googleMapsLink?.let(uriHandler::openUri)
                    },
                    enabled = !detail?.googleMapsLink.isNullOrBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Start Directions")
                }

                Spacer(Modifier.height(sectionSpacing))

                // Meal tabs: Lunch, Dinner, Brunch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MealTab.entries.forEach { tab ->
                        val selected = s.selectedMeal == tab
                        Button(
                            onClick = { vm.selectMeal(tab) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tab.name)
                        }
                    }
                }

                Spacer(Modifier.height(itemSpacing))

                // Time slots horizontal scroll
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    s.timeSlots.forEach { time ->
                        OutlinedButton(
                            onClick = { vm.selectTimeSlot(time) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (s.selectedTimeSlot == time) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(time, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(Modifier.height(itemSpacing))

                s.selectedTimeSlot?.let { selected ->
                    Text(
                        text = "Selected time: $selected (${s.selectedMeal.name})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(itemSpacing))
                }

                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("See All Available Times")
                }

                if (s.reviewHighlights.isNotEmpty()) {
                    Spacer(Modifier.height(sectionSpacing))
                    Text(
                        text = "Recent Reviews",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.reviewHighlights.forEachIndexed { index, review ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Reviewer #${review.id.takeLast(3)}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = postedTimeLabel(index),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "${review.rating}/10",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = review.content,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (review.tags.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            review.tags.forEach { tag ->
                                                AssistChip(
                                                    onClick = {},
                                                    label = { Text(tag) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    private fun imageResByKey(key: String): DrawableResource? {
        return when (key.lowercase()) {
            "pizza_home" -> Res.drawable.pizza_home
            "chesse_burger_home" -> Res.drawable.chesse_burger_home
            "coffee_home" -> Res.drawable.coffee_home
            "milkshake_home" -> Res.drawable.milkshake_home
            "capo_chino_home" -> Res.drawable.capo_chino_home
            "beyayenet_home" -> Res.drawable.beyayenet_home
            "local_home" -> Res.drawable.local_home
            "fast_food_home" -> Res.drawable.fast_food_home
            "drink_home" -> Res.drawable.drink_home
            "breakfast_home" -> Res.drawable.breakfast_home
            else -> null
        }
    }

    private fun Double.formatOneDecimal(): String {
        val rounded = (this * 10.0).roundToInt() / 10.0
        return rounded.toString()
    }

    private fun postedTimeLabel(index: Int): String {
        return when (index) {
            0 -> "Posted 2 days ago"
            1 -> "Posted 5 days ago"
            else -> "Posted 1 week ago"
        }
    }
}
