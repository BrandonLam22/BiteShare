package org.example.biteshare.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.biteshare.viewmodel.ProfileViewModel

private val ReviewOrange = Color(0xFFFF7A00)
private val MenuCardShape = RoundedCornerShape(18.dp)
private val ProfileCardShape = RoundedCornerShape(24.dp)

class ProfileView(
    private val vm: ProfileViewModel,
    private val onSavedRestaurants: () -> Unit,
    private val onPrivacy: () -> Unit,
    private val onHelp: () -> Unit,
    private val onLogout: () -> Unit,
    private val onEditProfile: () -> Unit,
    private val onFriendsList: () -> Unit,
    private val onFriendRequests: () -> Unit,
) {

    @Composable
    fun Content() {
        val s = vm.uiState

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 40.sp,
                color = ReviewOrange,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(32.dp))

            // User info section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ProfileCardShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = s.name.take(1),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = s.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = s.bio.ifBlank { "Add a short bio to tell friends about your food vibe." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (s.bio.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                Color.Black
                            },
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(0.7f)
                        .height(60.dp)
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
                icon = "👥",
                text = "Friend Requests (${s.incomingRequestCount})",
                onClick = onFriendRequests
            )

            Spacer(Modifier.height(16.dp))

            ToggleMenuItem(
                icon = "🔔",
                text = "Push Notifications",
                checked = s.notificationsEnabled,
                onCheckedChange = { vm.toggleNotifications() }
            )

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
                showIcon = false,
                isDestructive = true,
                onClick = {vm.logout()
                        onLogout()}
            )

            Spacer(Modifier.height(24.dp))
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
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black
                )
            }
        }
    }

    @Composable
    private fun MenuItem(
        icon: String,
        text: String,
        showIcon: Boolean = true,
        isDestructive: Boolean = false,
        onClick: () -> Unit,
    ) {
        val containerColor = if (isDestructive) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surface
        }
        val borderColor = if (isDestructive) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        }
        val iconColor = if (isDestructive) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        }
        val contentColor = if (isDestructive) {
            MaterialTheme.colorScheme.error
        } else {
            Color.Black
        }

        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MenuCardShape,
            color = containerColor,
            tonalElevation = 2.dp,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showIcon) {
                    Surface(
                        shape = CircleShape,
                        color = iconColor,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(icon, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "›",
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
            }
        }
    }

    @Composable
    private fun ToggleMenuItem(
        icon: String,
        text: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MenuCardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(icon, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            }
        }
    }
}