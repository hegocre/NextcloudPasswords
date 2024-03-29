package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.hegocre.nextcloudpasswords.ui.NCPScreen
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme

@Composable
fun NCPBottomNavigation(
    allScreens: List<NCPScreen>,
    currentScreen: NCPScreen,
    onScreenSelected: (NCPScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        windowInsets = WindowInsets.navigationBars
    ) {
        allScreens.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (currentScreen == screen) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.name
                    )
                },
                label = { Text(text = stringResource(screen.title)) },
                selected = currentScreen == screen,
                onClick = { onScreenSelected(screen) },
                alwaysShowLabel = false,
            )
        }
    }
}

@Preview(name = "Bottom Navigation preview")
@Composable
fun NCPBottomNavigationPreview() {
    NextcloudPasswordsTheme {
        NCPBottomNavigation(
            allScreens = NCPScreen.entries.filter { !it.hidden },
            currentScreen = NCPScreen.Passwords,
            onScreenSelected = {}
        )
    }
}