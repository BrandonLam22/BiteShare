package org.example.biteshare.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.biteshare.viewmodel.FriendsListViewModel

class FriendsListView(
    private val vm: FriendsListViewModel,
    private val onBack: () -> Unit,
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

            // Header with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Text("←", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Friends",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp)) // Balance the back button
            }

            Spacer(Modifier.height(16.dp))

            // Friends count
            Text(
                text = "${s.friends.size} Friends",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // Friends list
            if (s.friends.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "No friends yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(s.friends) { friend ->
                        FriendCard(
                            name = friend.name,
                            onClick = { /* TODO: Navigate to friend profile */ }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun FriendCard(
        name: String,
        onClick: () -> Unit,
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Surface(
                    shape = CircleShape,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Name
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                // Arrow
                Text(">", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}