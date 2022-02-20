package com.hegocre.nextcloudpasswords.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.hegocre.nextcloudpasswords.R

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

private val AmoledColorPalette = darkColors(
    primary = NextcloudBlue200,
    primaryVariant = NextcloudBlue500,
    secondary = NextcloudBlue200,
    secondaryVariant = NextcloudBlue500,
    background = Color.Black,
    surface = Color.Black,
)

@Composable
fun NextcloudPasswordsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = lightColors(),
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Composable
private fun NCPThemeContent(
    content: @Composable () -> Unit
) {
    val systemController = rememberSystemUiController()
    systemController.setSystemBarsColor(MaterialTheme.colors.background)
    content()
}

enum class NCPTheme(
    @StringRes val title: Int,
    val Theme: @Composable (@Composable () -> Unit) -> Unit
) {
    System(
        title = R.string.from_system,
        Theme = { content ->
            val colors = if (isSystemInDarkTheme())
                DarkColorPalette
            else
                LightColorPalette

            MaterialTheme(
                colors = colors,
                typography = Typography,
                shapes = Shapes,
                content = { NCPThemeContent(content) }
            )
        }
    ),
    Light(
        title = R.string.light,
        Theme = { content ->
            MaterialTheme(
                colors = LightColorPalette,
                typography = Typography,
                shapes = Shapes,
                content = { NCPThemeContent(content) }
            )
        }
    ),
    Dark(
        title = R.string.dark,
        Theme = { content ->
            MaterialTheme(
                colors = DarkColorPalette,
                typography = Typography,
                shapes = Shapes,
                content = { NCPThemeContent(content) }
            )
        }
    ),
    Amoled(
        title = R.string.black,
        Theme = { content ->
            MaterialTheme(
                colors = AmoledColorPalette,
                typography = Typography,
                shapes = Shapes,
                content = { NCPThemeContent(content) }
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