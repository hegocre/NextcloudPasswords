package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hegocre.nextcloudpasswords.ui.NCPScreen
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme

@Composable
fun NCPBottomNavigation(
    allScreens: List<NCPScreen>,
    currentScreen: NCPScreen,
    onScreenSelected: (NCPScreen) -> Unit
) {
    BottomNavigation(
        elevation = if (MaterialTheme.colors.isLight) AppBarDefaults.TopAppBarElevation else 0.dp,
        backgroundColor = MaterialTheme.colors.background
    ) {
        allScreens.forEach { screen ->
            BottomNavigationItem(
                icon = {
                    Icon(
                        imageVector = if (currentScreen == screen) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.name
                    )
                },
                label = { Text(text = stringResource(screen.title)) },
                selected = currentScreen == screen,
                onClick = { onScreenSelected(screen) },
                selectedContentColor = MaterialTheme.colors.primary,
                unselectedContentColor = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium)
            )
        }
    }
}

@Preview(name = "Bottom Navigation preview")
@Composable
fun NCPBottomNavigationPreview() {
    NextcloudPasswordsTheme {
        NCPBottomNavigation(
            allScreens = NCPScreen.values().toList(),
            currentScreen = NCPScreen.Passwords,
            onScreenSelected = {}
        )
    }
}