package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.NCPScreen
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NCPNavigationSuiteTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val topLevelScreens = listOf(NCPScreen.Passwords, NCPScreen.Favorites, NCPScreen.Folders)

    private fun setNavigationSuite(
        showNavigation: Boolean,
        onScreenSelected: (NCPScreen) -> Unit = {}
    ) {
        composeTestRule.setContent {
            NextcloudPasswordsTheme {
                NCPNavigationSuite(
                    allScreens = topLevelScreens,
                    currentScreen = NCPScreen.Passwords,
                    onScreenSelected = onScreenSelected,
                    showNavigation = showNavigation,
                    content = {}
                )
            }
        }
    }

    @Test
    fun showsTopLevelScreensWhenNavigationIsShown() {
        setNavigationSuite(showNavigation = true)

        composeTestRule.onNodeWithText(context.getString(R.string.passwords)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.favorites)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.folders)).assertIsDisplayed()
    }

    @Test
    fun hidesTopLevelScreensWhenNavigationIsHidden() {
        setNavigationSuite(showNavigation = false)

        composeTestRule.onNodeWithText(context.getString(R.string.favorites)).assertIsNotDisplayed()
    }

    @Test
    fun selectingScreenReportsIt() {
        var selectedScreen: NCPScreen? = null
        setNavigationSuite(showNavigation = true, onScreenSelected = { selectedScreen = it })

        composeTestRule.onNodeWithText(context.getString(R.string.folders)).performClick()

        assertEquals(NCPScreen.Folders, selectedScreen)
    }
}
