package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AddElementButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private var addedElement: String? = null

    private fun setAddElementButton(menuBelowButton: Boolean, canAddFolder: Boolean = true) {
        val onFolderAdd: (() -> Unit)? = if (canAddFolder) {
            { addedElement = FOLDER }
        } else null

        composeTestRule.setContent {
            NextcloudPasswordsTheme {
                AddElementButton(
                    onPasswordAdd = { addedElement = PASSWORD },
                    onFolderAdd = onFolderAdd,
                    menuBelowButton = menuBelowButton
                )
            }
        }
    }

    private fun clickAddButton() {
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.action_create_element))
            .performClick()
    }

    @Test
    fun floatingMenuOffersFolder() {
        setAddElementButton(menuBelowButton = false)

        clickAddButton()
        composeTestRule.onNodeWithText(context.getString(R.string.folder)).performClick()

        assertEquals(FOLDER, addedElement)
    }

    @Test
    fun dropdownMenuOffersPassword() {
        setAddElementButton(menuBelowButton = true)

        clickAddButton()
        composeTestRule.onNodeWithText(context.getString(R.string.password)).performClick()

        assertEquals(PASSWORD, addedElement)
    }

    @Test
    fun addsPasswordDirectlyWhenFoldersCannotBeAdded() {
        setAddElementButton(menuBelowButton = false, canAddFolder = false)

        clickAddButton()

        assertEquals(PASSWORD, addedElement)
    }

    private companion object {
        const val PASSWORD = "password"
        const val FOLDER = "folder"
    }
}
