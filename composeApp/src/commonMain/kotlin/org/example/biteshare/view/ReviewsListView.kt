package org.example.biteshare.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.biteshare.viewmodel.ReviewsListViewModel

class ReviewsListView(
    private val vm: ReviewsListViewModel,
    private val onBack: () -> Unit,
) {
    @Composable
    fun Content() {
        val s = vm.uiState
        LaunchedEffect(Unit) {
            vm.refresh()
        }
        var reviewToDelete by remember { mutableStateOf<String?>(null) }
        var reviewToEdit by remember { mutableStateOf<String?>(null) }

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
                    text = "My Reviews",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(16.dp))

            // Review count
            Text(
                text = "${s.reviews.size} Reviews",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // Reviews list
            if (s.reviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No reviews yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Start reviewing restaurants!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(s.reviews) { review ->
                        ReviewCard(
                            restaurantName = review.restaurantName,
                            rating = review.rating,
                            comment = review.content,
                            date = review.createdAt,
                            onEdit = { reviewToEdit = review.id },
                            onDelete = { reviewToDelete = review.id }
                        )
                    }
                }
            }
        }

        // Delete confirmation dialog
        if (reviewToDelete != null) {
            DeleteConfirmationDialog(
                onConfirm = {
                    vm.deleteReview(reviewToDelete!!)
                    reviewToDelete = null
                },
                onDismiss = { reviewToDelete = null }
            )
        }

        // Edit review dialog
        if (reviewToEdit != null) {
            val review = s.reviews.find { it.id == reviewToEdit }
            if (review != null) {
                EditReviewDialog(
                    restaurantName = review.restaurantName,
                    currentRating = review.rating,
                    currentComment = review.content,
                    onSave = { newRating, newComment ->
                        vm.editReview(reviewToEdit!!, newRating.toInt(), newComment)
                        reviewToEdit = null
                    },
                    onDismiss = { reviewToEdit = null }
                )
            }
        }
    }

    @Composable
    private fun ReviewCard(
        restaurantName: String,
        rating: Int,
        comment: String,
        date: String?,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Restaurant name and rating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = restaurantName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = rating.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Comment
                Text(
                    text = comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                // Date and actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (date != null) {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onEdit) {
                            Text("Edit")
                        }
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DeleteConfirmationDialog(
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Delete Review") },
            text = { Text("Are you sure you want to delete this review? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    @Composable
    private fun EditReviewDialog(
        restaurantName: String,
        currentRating: Int,
        currentComment: String,
        onSave: (Int, String) -> Unit,
        onDismiss: () -> Unit,
    ) {
        var rating by remember { mutableStateOf(currentRating) }
        var comment by remember { mutableStateOf(currentComment) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Edit Review",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    Text(
                        text = restaurantName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    // Rating selector
                    Text(
                        text = "Rating",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..5).forEach { star ->
                            TextButton(
                                onClick = { rating = star },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (star <= rating) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            ) {
                                Text(
                                    text = "⭐",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Comment field
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Your Review") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onSave(rating, comment) },
                    shape = RoundedCornerShape(12.dp),
                    enabled = comment.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}