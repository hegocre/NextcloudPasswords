package com.hegocre.nextcloudpasswords.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val LightColorPalette = lightColors(
    primary = NextcloudBlue500,
    primaryVariant = NextcloudBlue700,
    secondary = NextcloudBlue500,
    secondaryVariant = NextcloudBlue700,
)

private val DarkColorPalette = darkColors(
    primary = NextcloudBlue200,
    primaryVariant = NextcloudBlue500,
    secondary = NextcloudBlue200,
    secondaryVariant = NextcloudBlue500,
)

@Composable
fun NextcloudPasswordsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}