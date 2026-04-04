package org.example.biteshare.presentation

import org.example.biteshare.viewmodel.HelpViewModel
import kotlin.test.Test
import kotlin.test.assertFalse

class HelpViewModelTest {

    @Test
    fun defaultStateIsCollapsed() {
        val vm = HelpViewModel()
        assertFalse(vm.uiState.faqExpanded)
    }
}
