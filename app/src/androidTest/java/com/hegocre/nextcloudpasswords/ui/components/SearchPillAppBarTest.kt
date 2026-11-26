package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchPillAppBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private var searchQuery: String? = null
    private var searchOpened = false
    private var searchClosed = false

    private fun setSearchPillAppBar(
        query: String,
        searchExpanded: Boolean,
        placeholder: String = context.getString(R.string.action_search)
    ) {
        composeTestRule.setContent {
            NextcloudPasswordsTheme {
                SearchPillAppBar(
                    searchQuery = query,
                    setSearchQuery = { searchQuery = it },
                    searchExpanded = searchExpanded,
                    onSearchClick = { searchOpened = true },
                    onSearchCloseClick = { searchClosed = true },
                    placeholder = placeholder
                )
            }
        }
    }

    @Test
    fun tappingSearchFieldOpensSearch() {
        setSearchPillAppBar(query = "", searchExpanded = false)

        composeTestRule.onNodeWithText(context.getString(R.string.action_search)).performClick()

        composeTestRule.waitUntil { searchOpened }
    }

    @Test
    fun clearButtonClearsTypedQueryAndKeepsSearchOpen() {
        setSearchPillAppBar(query = "gps", searchExpanded = true)

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.action_clear_search_query))
            .performClick()

        assertEquals("", searchQuery)
        assertFalse(searchClosed)
    }

    @Test
    fun clearButtonIsHiddenWhenQueryIsEmpty() {
        setSearchPillAppBar(query = "", searchExpanded = true)

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.action_clear_search_query))
            .assertDoesNotExist()
    }

    @Test
    fun searchFieldShowsGivenPlaceholder() {
        setSearchPillAppBar(query = "", searchExpanded = false, placeholder = "Search (230)")

        composeTestRule.onNodeWithText("Search (230)").assertIsDisplayed()
    }

    @Test
    fun backButtonClosesSearch() {
        setSearchPillAppBar(query = "gps", searchExpanded = true)

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.navigation_back))
            .performClick()

        assertTrue(searchClosed)
    }
}
