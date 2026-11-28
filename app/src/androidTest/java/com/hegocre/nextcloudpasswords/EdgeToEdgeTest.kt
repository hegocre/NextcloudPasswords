package com.hegocre.nextcloudpasswords

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import com.hegocre.nextcloudpasswords.ui.activities.AboutActivity
import com.hegocre.nextcloudpasswords.ui.activities.LoginActivity
import org.junit.Assert
import org.junit.Assume
import org.junit.Test

class EdgeToEdgeTest {
    @Test
    fun loginScreenDrawsEdgeToEdgeWithoutNavigationBarScrim() {
        assertEdgeToEdgeWithoutNavigationBarScrim(LoginActivity::class.java)
    }

    @Test
    fun aboutScreenDrawsEdgeToEdgeWithoutNavigationBarScrim() {
        assertEdgeToEdgeWithoutNavigationBarScrim(AboutActivity::class.java)
    }

    private fun <T : Activity> assertEdgeToEdgeWithoutNavigationBarScrim(activityClass: Class<T>) {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)

        ActivityScenario.launch(activityClass).use { scenario ->
            scenario.onActivity { activity ->
                val window = activity.window
                Assert.assertFalse(window.isNavigationBarContrastEnforced)
                Assert.assertEquals(
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS,
                    window.attributes.layoutInDisplayCutoutMode
                )
            }
        }
    }
}
