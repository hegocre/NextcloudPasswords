package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class ReadableFormWidthTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setFormInContainerOfWidth(containerWidth: Dp) {
        composeTestRule.setContent {
            Box(modifier = Modifier.width(containerWidth)) {
                Box(modifier = Modifier.readableFormWidth().testTag(FORM_TAG))
            }
        }
    }

    @Test
    fun formIsCappedOnWideContainers() {
        setFormInContainerOfWidth(1000.dp)

        composeTestRule.onNodeWithTag(FORM_TAG).assertWidthIsEqualTo(600.dp)
    }

    @Test
    fun formFillsNarrowContainers() {
        setFormInContainerOfWidth(360.dp)

        composeTestRule.onNodeWithTag(FORM_TAG).assertWidthIsEqualTo(360.dp)
    }

    private companion object {
        const val FORM_TAG = "form"
    }
}
