package org.example.biteshare.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.viewmodel.VoteWithFriendsViewModel

class VoteWithFriendsView(
    private val vm: VoteWithFriendsViewModel,
    private val onBack: () -> Unit,
    private val onFinish: (String) -> Unit = {},
) {
    @Composable
    fun Content() {
        val s = vm.uiState

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onBack) { Text("Back") }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Friends Vote",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            if (s.restaurants.isEmpty()) {
                Text("No restaurants available for voting.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Go Back") }
                return
            }

            Text(
                text = "Your vote (${vm.mySelectedCount()} selected)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "You can only select your own choices. Friends' choices are read-only.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (s.isVotingClosed) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Voting is closed. Results are locked.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))

            Text(
                text = "Friends' current picks",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(s.friends, key = { it.id }) { friend ->
                    FriendVoteCard(
                        friendName = friend.name,
                        selectedNames = vm.friendVoteRestaurantNames(friend.id),
                        selectedCount = vm.friendSelectedCount(friend.id)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Pick your restaurants",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(s.restaurants, key = { it.id }) { restaurant ->
                    VoteRestaurantCard(
                        restaurant = restaurant,
                        checked = vm.isMySelection(restaurant.id),
                        totalVotes = vm.voteCountForRestaurant(restaurant.id),
                        onToggle = { vm.toggleMyVote(restaurant.id) },
                        enabled = !s.isVotingClosed
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text("Back")
                }
                Button(
                    onClick = {
                        vm.finishVoting()
                        onFinish(vm.sessionId)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !s.isVotingClosed
                ) {
                    Text(if (s.isVotingClosed) "Voting Closed" else "Done")
                }
            }

            Spacer(Modifier.height(14.dp))
        }
    }

    @Composable
    private fun FriendVoteCard(
        friendName: String,
        selectedNames: List<String>,
        selectedCount: Int,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.width(220.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "$friendName ($selectedCount)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (selectedNames.isEmpty()) "No picks yet"
                    else selectedNames.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun VoteRestaurantCard(
        restaurant: Restaurant,
        checked: Boolean,
        totalVotes: Int,
        onToggle: () -> Unit,
        enabled: Boolean,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled, onClick = onToggle)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = null
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = restaurant.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = joinInfo(restaurant.category, restaurant.price, etaLabel(restaurant.eta)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(
                            text = "$totalVotes votes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

}
