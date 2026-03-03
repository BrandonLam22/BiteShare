package org.example.biteshare.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.biteshare.domain.CategoryItem
import org.example.biteshare.domain.PopularItem
import org.example.biteshare.viewmodel.HomeViewModel
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

private val categoryEmoji = mapOf(
    "local" to "L",
    "fastfood" to "F",
    "drink" to "D",
    "breakfast" to "B",
)

private fun categoryImageRes(label: String): DrawableResource? {
    return when (label.lowercase()) {
        "local" -> Res.drawable.local_home
        "fast food" -> Res.drawable.fast_food_home
        "drink" -> Res.drawable.drink_home
        "breakfast" -> Res.drawable.breakfast_home
        else -> null
    }
}

private fun popularImageRes(title: String): DrawableResource? {
    return when (title.lowercase()) {
        "chess burger" -> Res.drawable.chesse_burger_home
        "chesse burger" -> Res.drawable.chesse_burger_home
        "pizza" -> Res.drawable.pizza_home
        "beyayenet" -> Res.drawable.beyayenet_home
        "coffee" -> Res.drawable.coffee_home
        "milkshake" -> Res.drawable.milkshake_home
        "capo chino" -> Res.drawable.capo_chino_home
        else -> null
    }
}

class HomeView(
    private val vm: HomeViewModel,
    private val onSearchClick: () -> Unit,
    private val onSettingsClick: () -> Unit = {},
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
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = s.greeting,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onSettingsClick) {
                    Text("⚙️", style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(Modifier.height(20.dp))

            Surface(
                onClick = onSearchClick,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔍", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                s.categories.forEach { cat ->
                    CategoryChip(
                        item = cat,
                        emoji = categoryEmoji[cat.id] ?: "?"
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Popular Dish",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = { }) {
                    Text("See all", color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                s.popularDishes.forEach { item ->
                    PopularCard(item = item)
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Popular Drink",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = { }) {
                    Text("See all", color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                s.popularDrinks.forEach { item ->
                    PopularCard(item = item)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    @Composable
    private fun CategoryChip(
        item: CategoryItem,
        emoji: String,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(72.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    val imageRes = categoryImageRes(item.label)
                    if (imageRes == null) {
                        Text(emoji, style = MaterialTheme.typography.headlineSmall)
                    } else {
                        Image(
                            painter = painterResource(imageRes),
                            contentDescription = item.label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }

    @Composable
    private fun PopularCard(item: PopularItem) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.width(160.dp)
        ) {
            Column(modifier = Modifier.padding(0.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val imageRes = popularImageRes(item.title)
                    if (imageRes == null) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("🍴", style = MaterialTheme.typography.displaySmall)
                        }
                    } else {
                        Image(
                            painter = painterResource(imageRes),
                            contentDescription = "${item.title} photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
