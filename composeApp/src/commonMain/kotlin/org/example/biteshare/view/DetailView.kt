package org.example.biteshare.view

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
import kotlin.time.Clock
import kotlin.time.Instant
import org.example.biteshare.domain.ReviewTagCatalog
import org.example.biteshare.viewmodel.DetailViewModel
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
                if (selectedKey != null) {
                    RestaurantDetailImage(
                        imageRef = selectedKey,
                        contentDescription = "${restaurant.name} image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
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

                val detailImages = detail?.images.orEmpty()
                if (detailImages.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        detailImages.forEachIndexed { index, key ->
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
                                RestaurantDetailImage(
                                    imageRef = key,
                                    contentDescription = "${restaurant.name} thumbnail ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("⭐", style = MaterialTheme.typography.bodyMedium)
                    val displayRating = when {
                        s.reviewHighlights.isNotEmpty() -> s.averageReviewScore
                        (detail?.rating ?: 0.0) > 0.0 -> detail!!.rating
                        else -> restaurant.rating
                    }
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

                val detailAttributes = detail?.attributes.orEmpty()
                if (detailAttributes.isNotEmpty()) {
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
                        detailAttributes.forEach { attr ->
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

                val detailFeaturedItems = detail?.featuredItems.orEmpty()
                if (detailFeaturedItems.isNotEmpty()) {
                    Text(
                        text = "Featured Items",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(itemSpacing))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        detailFeaturedItems.forEach { item ->
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

                if (s.reviewHighlights.isNotEmpty()) {
                    Text(
                        text = "Reviews",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.reviewHighlights.forEach { review ->
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
                                                text = review.username ?: "Anonymous",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = postedTimeLabel(review.createdAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = review.ratingLabel(),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
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
                                                    label = { Text(ReviewTagCatalog.labelFor(tag)) }
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

    private fun Double.formatOneDecimal(): String {
        val rounded = (this * 10.0).roundToInt() / 10.0
        return rounded.toString()
    }

    private fun postedTimeLabel(createdAt: String?): String {
        if (createdAt.isNullOrBlank()) return "Recently posted"

        return runCatching {
            val postedAt = Instant.parse(createdAt)
            val elapsedSeconds = (Clock.System.now() - postedAt).inWholeSeconds.coerceAtLeast(0)

            when {
                elapsedSeconds < 60 -> "Posted just now"
                elapsedSeconds < 3_600 -> "Posted ${elapsedSeconds / 60} minute${if (elapsedSeconds / 60 == 1L) "" else "s"} ago"
                elapsedSeconds < 86_400 -> "Posted ${elapsedSeconds / 3_600} hour${if (elapsedSeconds / 3_600 == 1L) "" else "s"} ago"
                elapsedSeconds < 604_800 -> "Posted ${elapsedSeconds / 86_400} day${if (elapsedSeconds / 86_400 == 1L) "" else "s"} ago"
                else -> "Posted ${elapsedSeconds / 604_800} week${if (elapsedSeconds / 604_800 == 1L) "" else "s"} ago"
            }
        }.getOrDefault("Recently posted")
    }
}
