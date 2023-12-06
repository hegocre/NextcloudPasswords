package com.hegocre.nextcloudpasswords.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import com.materialkolor.dynamicColorScheme

private val defaultLightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)


private val defaultDarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)

@Composable
fun NextcloudPasswordsTheme(
    content: @Composable () -> Unit
) {
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
            } catch (e: IllegalArgumentException) {
                Color(0xFF745BCA)
            }
        }
    }
    val useSystemDynamicColor by preferencesManager.getUseSystemDynamicColor()
        .collectAsState(initial = false)

    val colorScheme = when {
        useSystemDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            when {
                appTheme == NCPTheme.LIGHT -> dynamicLightColorScheme(context)
                appTheme == NCPTheme.DARK -> dynamicDarkColorScheme(context)
                appTheme == NCPTheme.AMOLED -> dynamicDarkColorScheme(context).copy(
                    background = Color.Black,
                    surface = Color.Black
                )

                isSystemInDarkTheme() -> dynamicDarkColorScheme(context)
                else -> dynamicLightColorScheme(context)
            }
        }

        useNextcloudInstanceColor -> {
            when (appTheme) {
                NCPTheme.LIGHT -> dynamicColorScheme(instanceColor, false)
                NCPTheme.DARK -> dynamicColorScheme(instanceColor, true)
                NCPTheme.AMOLED -> dynamicColorScheme(instanceColor, true).copy(
                    background = Color.Black,
                    surface = Color.Black
                )

                else -> dynamicColorScheme(instanceColor, isSystemInDarkTheme())
            }
        }

        appTheme == NCPTheme.LIGHT -> defaultLightColorScheme
        appTheme == NCPTheme.DARK -> defaultDarkColorScheme
        appTheme == NCPTheme.AMOLED -> defaultDarkColorScheme.copy(
            background = Color.Black,
            surface = Color.Black
        )

        isSystemInDarkTheme() -> defaultDarkColorScheme
        else -> defaultLightColorScheme
    }
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = colorScheme.isLight()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                if (colorScheme.isLight()) {
                    window.navigationBarColor = Color.Black.copy(alpha = 0.3f).toArgb()
                } else {
                    window.navigationBarColor = Color.Transparent.toArgb()
                }
            } else {
                insetsController.isAppearanceLightNavigationBars = colorScheme.isLight()
                window.navigationBarColor = Color.Transparent.toArgb()
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
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