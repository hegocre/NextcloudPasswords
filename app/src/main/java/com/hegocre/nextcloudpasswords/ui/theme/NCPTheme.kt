package com.hegocre.nextcloudpasswords.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec

private fun seedColorScheme(seedColor: Color, isDark: Boolean, isAmoled: Boolean = false) =
    dynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        isAmoled = isAmoled,
        style = PaletteStyle.TonalSpot,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
    )

@Composable
fun NextcloudPasswordsTheme(
    content: @Composable () -> Unit
) {
    val isPreview = LocalInspectionMode.current

    if (isPreview) {
        NCPMaterialTheme(
            colorScheme = seedColorScheme(NextcloudBlue, isDark = isSystemInDarkTheme()),
            content = content
        )
    } else {
        val context = LocalContext.current
        val preferencesManager = PreferencesManager.getInstance(context)
        val appTheme by preferencesManager.getAppTheme().collectAsState(initial = NCPTheme.SYSTEM)
        val useNextcloudInstanceColor by preferencesManager.getUseInstanceColor()
            .collectAsState(initial = false)
        val instanceColorString by preferencesManager.getInstanceColor()
            .collectAsState(initial = "#745bca")
        val instanceColor by remember {
            derivedStateOf {
                try {
                    Color(instanceColorString.toColorInt())
                } catch (_: IllegalArgumentException) {
                    Color(0xFF745BCA)
                }
            }
        }
        val useSystemDynamicColor by preferencesManager.getUseSystemDynamicColor()
            .collectAsState(initial = false)

        val isAmoled = appTheme == NCPTheme.AMOLED
        val isDark = when (appTheme) {
            NCPTheme.LIGHT -> false
            NCPTheme.DARK, NCPTheme.AMOLED -> true
            else -> isSystemInDarkTheme()
        }

        val colorScheme = if (useSystemDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when {
                isAmoled -> dynamicDarkColorScheme(context).copy(
                    background = Color.Black,
                    surface = Color.Black
                )

                isDark -> dynamicDarkColorScheme(context)
                else -> dynamicLightColorScheme(context)
            }
        } else {
            seedColorScheme(
                seedColor = if (useNextcloudInstanceColor) instanceColor else NextcloudBlue,
                isDark = isDark,
                isAmoled = isAmoled
            )
        }
        val view = LocalView.current

        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = colorScheme.isLight()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    insetsController.isAppearanceLightNavigationBars = colorScheme.isLight()
                }
            }
        }

        NCPMaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

@Composable
private fun NCPMaterialTheme(
    colorScheme: ColorScheme,
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        content = content
    )
}

fun ColorScheme.isLight() = this.background.luminance() > 0.5

val ColorScheme.favoriteColor: Color
    get() {
        return if (isLight()) Amber500 else Amber200
    }

val ColorScheme.statusGood: Color
    get() {
        return if (isLight()) Green500 else Green200
    }

val ColorScheme.statusWeak: Color
    get() {
        return if (isLight()) Amber500 else Amber200
    }

val ColorScheme.statusBreached: Color
    get() {
        return if (isLight()) Red500 else Red200
    }

object NCPTheme {
    const val SYSTEM = "system_theme"
    const val LIGHT = "light_theme"
    const val DARK = "dark_theme"
    const val AMOLED = "amoled_theme"
}