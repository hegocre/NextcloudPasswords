package com.hegocre.nextcloudpasswords.ui.components

import androidx.window.core.layout.WindowSizeClass
import com.hegocre.nextcloudpasswords.ui.NCPScreen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordDetailPaneTest {
    private val phoneWindow = WindowSizeClass(minWidthDp = 411, minHeightDp = 800)
    private val unfoldedFoldableWindow = WindowSizeClass(minWidthDp = 933, minHeightDp = 700)

    @Test
    fun phoneShowsDetailsInBottomSheet() {
        assertFalse(showsPasswordDetailPane(phoneWindow, NCPScreen.Passwords))
    }

    @Test
    fun unfoldedFoldableShowsDetailsInSidePane() {
        assertTrue(showsPasswordDetailPane(unfoldedFoldableWindow, NCPScreen.Passwords))
        assertTrue(showsPasswordDetailPane(unfoldedFoldableWindow, NCPScreen.Favorites))
    }

    @Test
    fun editorsKeepFullWidthOnUnfoldedFoldable() {
        assertFalse(showsPasswordDetailPane(unfoldedFoldableWindow, NCPScreen.PasswordEdit))
        assertFalse(showsPasswordDetailPane(unfoldedFoldableWindow, NCPScreen.FolderEdit))
    }
}
