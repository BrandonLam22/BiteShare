package org.example.biteshare.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import org.example.biteshare.domain.PickMode
import org.example.biteshare.viewmodel.PickForMeViewModel

class PickForMeView(
    private val vm: PickForMeViewModel,
    private val onGo: () -> Unit,
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

                Spacer(Modifier.height(26.dp))
            } else {
                Spacer(Modifier.height(60.dp))
            }

            Spacer(Modifier.weight(1f))

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
