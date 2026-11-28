package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NCPSearchTopBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun placeholderFor(passwordCount: Int?, folderLabel: String? = null): String {
        var placeholder = ""
        composeTestRule.setContent {
            placeholder = searchPlaceholder(passwordCount = passwordCount, folderLabel = folderLabel)
        }
        composeTestRule.waitForIdle()
        return placeholder
    }

    @Test
    fun placeholderShowsPasswordCount() {
        assertEquals(
            context.getString(R.string.action_search_with_count, 230),
            placeholderFor(passwordCount = 230)
        )
    }

    @Test
    fun placeholderNamesOpenFolder() {
        assertEquals(
            context.getString(R.string.action_search_in_folder_with_count, "Work", 12),
            placeholderFor(passwordCount = 12, folderLabel = "Work")
        )
    }

    @Test
    fun placeholderFallsBackToPlainSearchWhileCountIsUnknown() {
        assertEquals(context.getString(R.string.action_search), placeholderFor(passwordCount = null))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun searchFieldHidesOnScrollDownAndReturnsOnScrollUp() {
        composeTestRule.setContent {
            NextcloudPasswordsTheme {
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
                Column(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)) {
                    NCPSearchTopBar(
                        username = "",
                        serverAddress = "",
                        searchPlaceholder = SEARCH_PLACEHOLDER,
                        scrollBehavior = scrollBehavior
                    )
                    LazyColumn(modifier = Modifier.testTag(LIST_TAG)) {
                        items((1..100).toList()) { Text(text = "Row $it", modifier = Modifier.height(48.dp)) }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag(LIST_TAG).performTouchInput { swipeUp() }
        composeTestRule.onNodeWithText(SEARCH_PLACEHOLDER).assertIsNotDisplayed()

        composeTestRule.onNodeWithTag(LIST_TAG).performTouchInput {
            swipeDown(startY = centerY, endY = centerY + height / 4f)
        }
        composeTestRule.onNodeWithText(SEARCH_PLACEHOLDER).assertIsDisplayed()
    }

    private companion object {
        const val SEARCH_PLACEHOLDER = "Search (100)"
        const val LIST_TAG = "list"
    }
}
