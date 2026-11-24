package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchAppBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private var searchQuery: String? = null
    private var searchClosed = false

    private fun setSearchAppBar(query: String) {
        composeTestRule.setContent {
            NextcloudPasswordsTheme {
                SearchAppBar(
                    searchQuery = query,
                    setSearchQuery = { searchQuery = it },
                    onBackPressed = { searchClosed = true }
                )
            }
        }
    }

    @Test
    fun clearButtonClearsTypedQueryAndKeepsSearchOpen() {
        setSearchAppBar(query = "gps")

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.action_clear_search_query))
            .performClick()

        assertEquals("", searchQuery)
        assertFalse(searchClosed)
    }

    @Test
    fun clearButtonClosesSearchWhenQueryIsEmpty() {
        setSearchAppBar(query = "")

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.action_close_search))
            .performClick()

        assertTrue(searchClosed)
    }
}
