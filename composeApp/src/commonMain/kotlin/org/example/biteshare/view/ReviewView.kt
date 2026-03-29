package org.example.biteshare.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.biteshare.components.LoginSignupButton
import org.example.biteshare.domain.Model
import org.example.biteshare.domain.ReviewTag
import org.example.biteshare.domain.ReviewTagCategory
import org.example.biteshare.viewmodel.ReviewViewModel
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults

@Preview
@Composable
fun ReviewView(viewModel: ReviewViewModel = ReviewViewModel(Model())) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // SECTION 1: TITLE
        Text(
            text = "Quick Review",
            color = Color(0xFFFF7A00),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 24.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // SECTION 2: RESTAURANT SEARCH
        RestaurantSearchField(viewModel = viewModel)

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 2b: RATING SLIDER (1-10)
        Text(
            text = "Rating",
            color = Color(0xFFFF7A00),
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 20.dp, bottom = 4.dp)
        )

        Text(
            text = "${viewModel.rating} / 10",
            color = Color(0xFFFF8C00),
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 20.dp, bottom = 8.dp)
        )

        Slider(
            value = viewModel.rating.toFloat(),
            onValueChange = { viewModel.rating = it.toInt().coerceIn(1, 10) },
            valueRange = 1f..10f,
            steps = 8, // 1–10 gives 9 steps; steps = 8 for 9 segments
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(horizontal = 16.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFF7A00),
                activeTrackColor = Color(0xFFFF7A00),
                inactiveTrackColor = Color(0xFFFF8C00).copy(alpha = 0.3f),
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 3: TAG SELECTION
        Text(
            text = "Pick Tags by Category",
            fontSize = 18.sp,
            color = Color(0xFFFF7A00),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 20.dp, bottom = 10.dp)
        )

        // We use a FlowRow or simple Row/Column for the tags
        TagCategoryList(
            categories = viewModel.tagCategories,
            selectedTags = viewModel.selectedTags,
            onTagClick = { tagId ->
                viewModel.toggleTag(tagId) // expresses intention to ViewModel
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 4: ONE LINE REVIEW
        Text(
            text = "One line review",
            color = Color(0xFFFF7A00),
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 20.dp, bottom = 8.dp)
        )

        OutlinedTextField(
            value = viewModel.reviewText,
            onValueChange = { viewModel.updateReviewText(it) }, // Character limit logic
            placeholder = {
                Text(
                    text = "Maximum 50 characters",
                    fontSize = 12.sp
                )
            },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(55.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF7A00),
                unfocusedBorderColor = Color(0xFFFF8C00)
            )
        )

        // Character counter
        Text(
            text = "${viewModel.reviewText.length}/50",
            fontSize = 10.sp,
            color = if (viewModel.reviewText.length > 50) Color.Red else Color.Gray,
            modifier = Modifier.align(Alignment.End).padding(end = 25.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        viewModel.postErrorMessage?.let { error ->
            Text(
                text = error,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(bottom = 12.dp)
            )
        }

        viewModel.postStatusMessage?.let { message ->
            Text(
                text = message,
                color = Color(0xFF2E7D32),
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(bottom = 12.dp)
            )
        }

        LoginSignupButton(
            text = if (viewModel.isPosting) "Posting..." else "Post",
            onClick = {
                viewModel.onPostClicked()
            },
            enabled = !viewModel.isPosting,
        )

    }
}

/**
 * Displays the restaurant search UI for the review page.
 * Shows a text field with suggestions, or a selected chip with an X button
 */
@Composable
private fun RestaurantSearchField(viewModel: ReviewViewModel) {
    val orange = Color(0xFFFF7A00)
    val lightOrange = Color(0xFFFF8C00)

    Column(modifier = Modifier.fillMaxWidth(0.9f)) {
        if (viewModel.selectedRestaurantName != null) {
            InputChip(
                selected = true,
                onClick = {},
                label = {
                    Text(
                        text = viewModel.selectedRestaurantName.orEmpty(),
                        fontSize = 16.sp
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.clearSelectedRestaurant() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Text("✕", color = orange, fontSize = 14.sp)
                    }
                },
                colors = InputChipDefaults.inputChipColors(
                    selectedContainerColor = Color(0xFFFFF0E1),
                    selectedLabelColor = Color.Black,
                    selectedTrailingIconColor = orange
                ),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedTextField(
                value = viewModel.restaurantQuery,
                onValueChange = { viewModel.updateRestaurantQuery(it) },
                placeholder = {
                    Text(
                        text = "Search restaurant by name",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = orange,
                    unfocusedBorderColor = lightOrange
                )
            )

            if (viewModel.restaurantSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        viewModel.restaurantSuggestions.forEachIndexed { index, name ->
                            SuggestionItem(
                                name = name,
                                onClick = { viewModel.selectRestaurant(name) }
                            )
                            if (index < viewModel.restaurantSuggestions.lastIndex) {
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color(0xFFFFE0CC))
                                )
                            }
                        }
                    }
                }
            } else if (viewModel.restaurantQuery.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No restaurant matches that prefix yet.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        viewModel.restaurantSearchError?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/**
 * Displays one clickable restaurant suggestion in the dropdown list.
 */
@Composable
private fun SuggestionItem(
    name: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Select",
                color = Color(0xFFFF7A00),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun TagCategoryList(
    categories: List<ReviewTagCategory>,
    selectedTags: List<String>,
    onTagClick: (String) -> Unit
) {
    val cardBackground = Color(0xFFFFF7F0)
    val accent = Color(0xFFFF7A00)
    val titleColor = Color(0xFFB85C00)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        categories.forEach { category ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBackground,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(accent, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = category.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = titleColor,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TagGrid(
                        tags = category.tags,
                        selectedTags = selectedTags,
                        onTagClick = onTagClick
                    )
                }
            }
        }
    }
}

@Composable
fun TagGrid(
    tags: List<ReviewTag>,
    selectedTags: List<String>,
    onTagClick: (String) -> Unit
) {
    // FlowRow automatically wraps items to the next line
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 4
    ) {
        tags.forEach { tag ->
            TagItem(
                tag = tag.label,
                isSelected = selectedTags.contains(tag.id),
                onClick = { onTagClick(tag.id) }
            )
        }
    }
}

@Composable
fun TagItem(
    tag: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val selectedColor = Color(0xFFFF7A00)
    val selectedText = Color.White
    val unselectedText = Color(0xFF6B3E00)
    val unselectedBorder = Color(0xFFFFD7B3)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) selectedColor else Color.White,
        border = if (isSelected) null else BorderStroke(1.dp, unselectedBorder),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = Modifier.defaultMinSize(minHeight = 32.dp)
    ) {
        Text(
            text = tag,
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 7.dp),
            color = if (isSelected) selectedText else unselectedText,
            fontSize = 13.sp
        )
    }
}
