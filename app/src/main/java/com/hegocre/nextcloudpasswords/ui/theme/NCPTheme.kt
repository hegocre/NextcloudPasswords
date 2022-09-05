package com.hegocre.nextcloudpasswords.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.hegocre.nextcloudpasswords.R

private val LightColorPalette = lightColorScheme(
    primary = NextcloudBlue500,
    secondary = NextcloudBlue500,
    tertiary = NextcloudBlue700

)

private val DarkColorPalette = darkColorScheme(
    primary = NextcloudBlue200,
    secondary = NextcloudBlue200,
    tertiary = NextcloudBlue500
)

private val AmoledColorPalette = darkColorScheme(
    primary = NextcloudBlue200,
    secondary = NextcloudBlue200,
    tertiary = NextcloudBlue500,
    background = Color.Black,
    surface = Color.Black,
)

@Composable
fun NextcloudPasswordsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

enum class NCPTheme(
    @StringRes val title: Int,
    val Theme: @Composable (@Composable () -> Unit) -> Unit
) {
    System(
        title = R.string.from_system,
        Theme = { content ->
            val isDynamicColor =
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
            val colorScheme = when {
                isDynamicColor && isSystemInDarkTheme() -> dynamicDarkColorScheme(LocalContext.current)
                isDynamicColor && !isSystemInDarkTheme() -> dynamicLightColorScheme(LocalContext.current)
                isSystemInDarkTheme() -> DarkColorPalette
                else -> LightColorPalette
            }

            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                shapes = Shapes,
                content = content
            )
        }
    ),
    Light(
        title = R.string.light,
        Theme = { content ->
            val isDynamicColor =
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
            MaterialTheme(
                colorScheme = if (isDynamicColor) dynamicLightColorScheme(LocalContext.current) else LightColorPalette,
                typography = Typography,
                shapes = Shapes,
                content = content
            )
        }
    ),
    Dark(
        title = R.string.dark,
        Theme = { content ->
            val isDynamicColor =
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
            MaterialTheme(
                colorScheme = if (isDynamicColor) dynamicDarkColorScheme(LocalContext.current) else DarkColorPalette,
                typography = Typography,
                shapes = Shapes,
                content = content
            )
        }
    ),
    Amoled(
        title = R.string.black,
        Theme = { content ->
            MaterialTheme(
                colorScheme = AmoledColorPalette,
                typography = Typography,
                shapes = Shapes,
                content = content
            )
        }
    );

    companion object {
        fun fromTitle(title: String): NCPTheme =
            when (title) {
                System.name -> System
                Light.name -> Light
                Dark.name -> Dark
                Amoled.name -> Amoled
                else -> System
            }
    }

}

@Composable
fun ColorScheme.isLight() = this.background.luminance() > 0.5