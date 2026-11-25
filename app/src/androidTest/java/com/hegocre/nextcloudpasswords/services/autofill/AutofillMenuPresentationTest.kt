package com.hegocre.nextcloudpasswords.services.autofill

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillMenuPresentationTest {
    private val appContext: Context =
        InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun labeledRowIsReadableWhenDarkThemeMeetsLightPopup() {
        assertRowIsReadable(nightMode = true, label = "Example - user")
    }

    @Test
    fun unlabeledRowIsReadableWhenDarkThemeMeetsLightPopup() {
        assertRowIsReadable(nightMode = true, label = null)
    }

    @Test
    fun labeledRowIsReadableInLightMode() {
        assertRowIsReadable(nightMode = false, label = "Example - user")
    }

    private fun assertRowIsReadable(nightMode: Boolean, label: String?) {
        val popupContext = autofillPopupContext(nightMode)
        val row = AutofillHelper.buildMenuPresentation(appContext, label)
            .apply(popupContext, FrameLayout(popupContext))

        val background = row.background as? ColorDrawable
        assertNotNull("Row must draw its own background", background)

        val textColor = row.findTextView().currentTextColor
        val contrast = ColorUtils.calculateContrast(textColor, background!!.color)
        assertTrue("Contrast $contrast is below 4.5:1", contrast >= 4.5)
    }

    // One UI draws the autofill popup on a light surface even in night mode, while rows
    // inflated from theme attributes pick up the dark theme's light text color.
    private fun autofillPopupContext(nightMode: Boolean): Context {
        val configuration = Configuration(appContext.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (nightMode) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        return ContextThemeWrapper(
            appContext.createConfigurationContext(configuration),
            if (nightMode) android.R.style.Theme_DeviceDefault else android.R.style.Theme_DeviceDefault_Light
        )
    }

    private fun View.findTextView(): TextView {
        if (this is TextView) return this
        val group = this as ViewGroup
        return (0 until group.childCount)
            .map { group.getChildAt(it) }
            .firstNotNullOf { child -> runCatching { child.findTextView() }.getOrNull() }
    }
}
