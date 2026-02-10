package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.model.FakeRepository

data class PrivacyUiState(
    val dataCollectionEnabled: Boolean = true,
)

class PrivacyViewModel(
    private val repo: FakeRepository? = null,
) {
    var uiState by mutableStateOf(PrivacyUiState())
        private set
}