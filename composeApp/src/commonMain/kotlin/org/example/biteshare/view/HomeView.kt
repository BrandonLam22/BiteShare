package org.example.biteshare.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.biteshare.domain.CategoryItem
import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.PopularItem
import org.example.biteshare.viewmodel.HomeSearchMode
import org.example.biteshare.viewmodel.HomeViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

import biteshare.composeapp.generated.resources.Res
import biteshare.composeapp.generated.resources.local_home

private val categoryEmoji = mapOf(
    "local" to "L",
    "fastfood" to "F",
    "drink" to "D",
    "breakfast" to "B",
)

private fun categoryImageRes(label: String): DrawableResource? {
    return Res.drawable.local_home
}

private fun popularImageRes(title: String): DrawableResource? {
    return Res.drawable.local_home
}

class HomeView(
    private val vm: HomeViewModel,
    private val onRestaurantSearch: (String) -> Unit,
    private val onTagClick: (String) -> Unit = {},
    private val onRestaurantBrowseClick: () -> Unit = {},
    private val onSettingsClick: () -> Unit = {},
) {

    @Composable
    fun Content() {
        val s = vm.uiState
        var searchMenuExpanded by remember { mutableStateOf(false) }
        if (s.isLoading && s.categories.isEmpty()) {
            LoadingState()
            return
        }
        if (s.errorMessage != null && s.categories.isEmpty()) {
            ErrorState(
                message = s.errorMessage,
                onRetry = vm::retryLoad
            )
            return
        }
        if (s.categories.isEmpty() && s.popularDishes.isEmpty() && s.popularDrinks.isEmpty()) {
            EmptyState(onRetry = vm::retryLoad)
            return
        }
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hi, Weclome to BiteShare.",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (s.isLoading) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Refreshing...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (s.errorMessage != null && s.categories.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Some content may be outdated.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Row {
                    IconButton(onClick = { vm.retryLoad() }) {
                        Text("↻", style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = onSettingsClick) {
                        Text("⚙️", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Surface(
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
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { searchMenuExpanded = true }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = s.searchMode.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "⌄",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = searchMenuExpanded,
                            onDismissRequest = { searchMenuExpanded = false }
                        ) {
                            HomeSearchMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(mode.label)
                                            Text(
                                                text = if (mode == s.searchMode) "✓" else "",
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        vm.setSearchMode(mode)
                                        searchMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    VerticalDivider(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    OutlinedTextField(
                        value = s.searchQuery,
                        onValueChange = vm::updateSearchQuery,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp),
                        singleLine = true,
                        placeholder = {
                            Text(
                                if (s.searchMode == HomeSearchMode.RESTAURANT) {
                                    "Search restaurants"
                                } else {
                                    "Search name or user id"
                                }
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = {
                            if (s.searchMode == HomeSearchMode.RESTAURANT) {
                                onRestaurantSearch(s.searchQuery)
                            } else {
                                vm.searchFriends()
                            }
                        },
                        enabled = s.searchQuery.isNotBlank() && !s.isFriendActionLoading
                    ) {
                        Text("Search")
                    }
                }
            }

            if (s.searchMode == HomeSearchMode.FRIEND) {
                Spacer(Modifier.height(12.dp))

                if (s.friendActionMessage != null) {
                    Text(
                        text = s.friendActionMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (s.friendSearchResults.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        s.friendSearchResults.forEach { friend ->
                            FriendSearchResultCard(
                                friend = friend,
                                isFriend = friend.id in s.currentFriendIds,
                                enabled = !s.isFriendActionLoading,
                                onToggle = { vm.toggleFriend(friend) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
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
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                s.categories.forEach { cat ->
                    CategoryChip(
                        item = cat,
                        emoji = categoryEmoji[cat.id] ?: (cat.label.firstOrNull()?.uppercase() ?: "?"),
                        onClick = { onTagClick(cat.label) }
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
                TextButton(onClick = onRestaurantBrowseClick) {
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
                    PopularCard(
                        item = item,
                        onClick = { onTagClick(item.title) }
                    )
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
                TextButton(onClick = onRestaurantBrowseClick) {
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
                    PopularCard(
                        item = item,
                        onClick = { onTagClick(item.title) }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    @Composable
    private fun LoadingState() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Loading homepage...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    @Composable
    private fun ErrorState(
        message: String,
        onRetry: () -> Unit,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Failed to load homepage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }

    @Composable
    private fun EmptyState(onRetry: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No homepage content yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Pull to refresh or retry to fetch latest recommendations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick = onRetry) {
                    Text("Refresh")
                }
            }
        }
    }

    @Composable
    private fun CategoryChip(
        item: CategoryItem,
        emoji: String,
        onClick: () -> Unit,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(72.dp)
        ) {
            Surface(
                onClick = onClick,
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
    private fun PopularCard(
        item: PopularItem,
        onClick: () -> Unit,
    ) {
        Surface(
            onClick = onClick,
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

    @Composable
    private fun FriendSearchResultCard(
        friend: Friend,
        isFriend: Boolean,
        enabled: Boolean,
        onToggle: () -> Unit,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = friend.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = friend.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = friend.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = onToggle,
                    enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(if (isFriend) "−" else "+")
                }
            }
        }
    }
}
