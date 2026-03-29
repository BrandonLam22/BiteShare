package org.example.biteshare.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.biteshare.viewmodel.ProfileViewModel

class ProfileView(
    private val vm: ProfileViewModel,
    private val onSavedRestaurants: () -> Unit,
    private val onPrivacy: () -> Unit,
    private val onHelp: () -> Unit,
    private val onLogout: () -> Unit,
    private val onEditProfile: () -> Unit,
    private val onFriendsList: () -> Unit,
    private val onFriendRequests: () -> Unit,
    private val onMyReviews: () -> Unit,
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
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(32.dp))

            // User info section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = s.name.take(1),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        text = s.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = s.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onEditProfile,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit Profile")
                }
                Spacer(Modifier.width(12.dp))
                StatColumn(
                    count = s.friendCount.toString(),
                    label = "Friends",
                    modifier = Modifier.weight(0.7f),
                    onClick = onFriendsList
                )
                /*Spacer(Modifier.width(12.dp))
                StatColumn(
                    count = s.followersCount.toString(),
                    label = "Followers",
                    modifier = Modifier.weight(0.7f)
                )*/
            }

            Spacer(Modifier.height(32.dp))

            // Menu items
            MenuItem(
                icon = "🍴",
                text = "Saved Restaurants",
                onClick = onSavedRestaurants
            )

            Spacer(Modifier.height(16.dp))

            MenuItem(
                icon = "⭐",
                text = "My Reviews",
                onClick = onMyReviews
            )

            Spacer(Modifier.height(16.dp))

            MenuItem(
                icon = "👥",
                text = "Friend Requests (${s.incomingRequestCount})",
                onClick = onFriendRequests
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🔔", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "Push Notifications",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = s.notificationsEnabled,
                    onCheckedChange = { _ -> vm.toggleNotifications() }
                )
            }

            Spacer(Modifier.height(32.dp))

            MenuItem(
                icon = "🔒",
                text = "Privacy",
                onClick = onPrivacy
            )
            Spacer(Modifier.height(16.dp))

            MenuItem(
                icon = "❓",
                text = "Help",
                onClick = onHelp
            )

            Spacer(Modifier.height(16.dp))

            MenuItem(
                icon = "👋",
                text = "Logout",
                onClick = {vm.logout()
                        onLogout()}
            )
        }
    }

    @Composable
    private fun StatColumn(
        count: String,
        label: String,
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
    ) {
        Surface(
            onClick = onClick ?: {},
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            color = if (onClick != null) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                Text(
                    text = count,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun MenuItem(
        icon: String,
        text: String,
        onClick: () -> Unit,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(icon, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(">", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}