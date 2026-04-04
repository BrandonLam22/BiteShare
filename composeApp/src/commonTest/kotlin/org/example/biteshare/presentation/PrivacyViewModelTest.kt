package org.example.biteshare.presentation

import org.example.biteshare.viewmodel.PrivacyViewModel
import kotlin.test.Test
import kotlin.test.assertTrue

class PrivacyViewModelTest {

    @Test
    fun defaultStateHasCollectionEnabled() {
        val vm = PrivacyViewModel()
        assertTrue(vm.uiState.dataCollectionEnabled)
    }
}
