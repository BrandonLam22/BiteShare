package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.data.BiteShareRepository

data class PrivacyUiState(
    val dataCollectionEnabled: Boolean = true,
)

class PrivacyViewModel(
    private val repo: BiteShareRepository? = null,
) {
    var uiState by mutableStateOf(PrivacyUiState())
        private set
}
