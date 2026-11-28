package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ButtonGroupPreferenceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val themes = mapOf("system" to "System", "light" to "Light", "dark" to "Dark")
    private var selectedTheme: String? = null

    private fun setButtonGroupPreference(selectedItem: String) {
        composeTestRule.setContent {
            NextcloudPasswordsTheme {
                ButtonGroupPreference(
                    items = themes,
                    selectedItem = selectedItem,
                    onItemSelected = { selectedTheme = it },
                    title = { Text(text = "Theme") }
                )
            }
        }
    }

    @Test
    fun showsSelectedItemAsChecked() {
        setButtonGroupPreference(selectedItem = "light")

        composeTestRule.onNodeWithText("Light").assertIsOn()
        composeTestRule.onNodeWithText("System").assertIsOff()
        composeTestRule.onNodeWithText("Dark").assertIsOff()
    }

    @Test
    fun selectingItemReportsItsKey() {
        setButtonGroupPreference(selectedItem = "light")

        composeTestRule.onNodeWithText("Dark").performClick()

        assertEquals("dark", selectedTheme)
    }

    @Test
    fun selectingCheckedItemKeepsIt() {
        setButtonGroupPreference(selectedItem = "light")

        composeTestRule.onNodeWithText("Light").performClick()

        assertEquals("light", selectedTheme)
    }
}
