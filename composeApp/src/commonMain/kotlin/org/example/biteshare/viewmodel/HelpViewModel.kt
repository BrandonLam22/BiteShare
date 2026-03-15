package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.data.BiteShareRepository

data class HelpUiState(
    val faqExpanded: Boolean = false,
)

class HelpViewModel(
    private val repo: BiteShareRepository? = null,
) {
    var uiState by mutableStateOf(HelpUiState())
        private set
}
