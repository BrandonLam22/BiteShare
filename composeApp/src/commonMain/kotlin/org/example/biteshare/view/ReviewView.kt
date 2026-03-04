package org.example.biteshare.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.biteshare.components.LoginSignupButton
import org.example.biteshare.domain.Model
import org.example.biteshare.viewmodel.ReviewViewModel
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults

@Preview
@Composable
fun ReviewView(viewModel: ReviewViewModel = ReviewViewModel(Model())) {
    // These would move to ReviewViewModel later for proper MVVM
//    var restaurantName by remember { mutableStateOf("") }
//    var reviewText by remember { mutableStateOf("") }
//    val selectedTags = remember { mutableStateListOf<String>() }
//    val availableTags = listOf("I hate it!", "Wait long", "Economical",
//                               "Too Spicy", "Good Taste", "Expensive",
//                               "Come Back", "Too Salty", "Raw")

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
        OutlinedTextField(
            value = viewModel.restaurantName, // read from ViewModel
            onValueChange = { viewModel.restaurantName = it }, // Write to ViewModel
            placeholder = {
                Text(
                    text = "Restaurant Name",
                    fontSize = 16.sp,
                    color = Color.Gray
                )},
            modifier = Modifier
                .fillMaxWidth(0.9f),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF7A00),
                unfocusedBorderColor = Color(0xFFFF8C00)
            )
        )

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
            text = "Some frequently used tags",
            fontSize = 18.sp,
            color = Color(0xFFFF7A00),
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 20.dp, bottom = 8.dp)
        )

        // We use a FlowRow or simple Row/Column for the tags
        TagGrid(
            tags = viewModel.availableTags,
            selectedTags = viewModel.selectedTags,
            onTagClick = { tag ->
                viewModel.toggleTag(tag) // expresses intention to ViewModel
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

        LoginSignupButton(
            text = "Post",
            onClick = {
                // Trigger the onPostClicked() method to send data to Model
                viewModel.onPostClicked()
            }
        )

    }
}

@Composable
fun TagGrid(
    tags: List<String>,
    selectedTags: List<String>,
    onTagClick: (String) -> Unit
) {
    // FlowRow automatically wraps items to the next line
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center, // Centers the cluster of tags
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 3
    ) {
        tags.forEach { tag ->
            TagItem(
                tag = tag,
                isSelected = selectedTags.contains(tag),
                onClick = { onTagClick(tag)}
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
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        // Orange if selected, light gray if not
        color = if (isSelected) Color(0xFFFF7A00) else Color(0xFFF1F1F1),
        modifier = Modifier
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = tag,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (isSelected) Color.White else Color.Black,
            fontSize = 14.sp
        )
    }
}

