package org.example.biteshare.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.biteshare.domain.BudgetFilter
import org.example.biteshare.domain.CuisineFilter
import org.example.biteshare.domain.DistanceFilter
import org.example.biteshare.domain.PickMode
import org.example.biteshare.viewmodel.PickForMeViewModel

class PickForMeView(
    private val vm: PickForMeViewModel,
    private val onGo: () -> Unit,
    private val onInvites: () -> Unit,
    private val onHistory: () -> Unit,
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
            Text(
                text = "Pick for Me",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(18.dp))
            ModeToggle(
                selected = s.mode,
                onSelect = vm::setMode
            )

            Spacer(Modifier.height(22.dp))

            if (s.mode == PickMode.WITH_FRIENDS) {
                Text(
                    text = "Select Your Friends",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(s.friends) { f ->
                        FriendAvatar(
                            name = f.name,
                            selected = s.selectedFriendIds.contains(f.id),
                            onClick = { vm.toggleFriend(f.id) }
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onInvites, modifier = Modifier.weight(1f)) {
                        Text("Vote Invites")
                    }
                    Button(onClick = onHistory, modifier = Modifier.weight(1f)) {
                        Text("Voting History")
                    }
                }

                Spacer(Modifier.height(22.dp))
            } else {
                Spacer(Modifier.height(8.dp))
            }

            Text(
                text = "Advanced Filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            FilterGroupTitle("Distance")
            val hasLocation = s.currentLocation != null
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DistanceFilter.entries.toList()) { distance ->
                    FilterChip(
                        selected = s.filters.distance == distance,
                        onClick = { vm.setDistance(distance) },
                        label = { Text(distance.label) }
                    )
                }
            }
            if (!hasLocation) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Location unavailable. Tap a distance to request access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            FilterGroupTitle("Budget")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BudgetFilter.entries.toList()) { budget ->
                    FilterChip(
                        selected = s.filters.budget == budget,
                        onClick = { vm.setBudget(budget) },
                        label = { Text(budget.label) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            FilterGroupTitle("Cuisine")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CuisineFilter.entries.toList().chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { cuisine ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = s.filters.cuisine == cuisine,
                                onClick = { vm.setCuisine(cuisine) },
                                label = { Text(cuisine.label) }
                            )
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Minimum rating: ${s.filters.minRating}/10",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = s.filters.minRating.toFloat(),
                onValueChange = { vm.setMinRating(it.toDouble()) },
                valueRange = 0f..10f,
                steps = 19
            )

            Text(
                text = "${s.resultPreviewCount} matches based on your filters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                FloatingActionButton(onClick = onGo) {
                    Text("🚀")
                }
                Spacer(Modifier.width(10.dp))
                Text("Go!", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    @Composable
    private fun FilterGroupTitle(title: String) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))
    }

    @Composable
    private fun ModeToggle(
        selected: PickMode,
        onSelect: (PickMode) -> Unit,
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
                TogglePill(
                    text = "Me Only",
                    selected = selected == PickMode.ME_ONLY,
                    onClick = { onSelect(PickMode.ME_ONLY) },
                    modifier = Modifier.weight(1f)
                )
                TogglePill(
                    text = "With My Friends",
                    selected = selected == PickMode.WITH_FRIENDS,
                    onClick = { onSelect(PickMode.WITH_FRIENDS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    @Composable
    private fun TogglePill(
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
                .clickable { onClick() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    @Composable
    private fun FriendAvatar(
        name: String,
        selected: Boolean,
        onClick: () -> Unit,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(72.dp)
        ) {
            Surface(
                shape = CircleShape,
                border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable { onClick() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.take(1),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(name, style = MaterialTheme.typography.labelMedium)
        }
    }

}
