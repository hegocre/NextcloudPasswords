package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.hegocre.nextcloudpasswords.ui.NCPScreen
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme

private val verticalNavigationSuiteTypes = setOf(
    NavigationSuiteType.WideNavigationRailCollapsed,
    NavigationSuiteType.WideNavigationRailExpanded,
    NavigationSuiteType.NavigationRail,
    NavigationSuiteType.NavigationDrawer,
)

/**
 * Shows the top level screens as a bottom bar on compact windows and as a navigation rail on
 * wider ones, such as unfolded foldables and tablets. [content] is told whether the rail is shown.
 */
@Composable
fun NCPNavigationSuite(
    allScreens: List<NCPScreen>,
    currentScreen: NCPScreen,
    onScreenSelected: (NCPScreen) -> Unit,
    showNavigation: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (showsNavigationRail: Boolean) -> Unit
) {
    val navigationSuiteType =
        NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfoV2())
    val navigationSuiteState = rememberNavigationSuiteScaffoldState(
        initialValue = if (showNavigation) NavigationSuiteScaffoldValue.Visible
        else NavigationSuiteScaffoldValue.Hidden
    )
    LaunchedEffect(showNavigation) {
        if (showNavigation) navigationSuiteState.show() else navigationSuiteState.hide()
    }

    NavigationSuiteScaffold(
        navigationItems = {
            allScreens.forEach { screen ->
                NavigationSuiteItem(
                    selected = currentScreen == screen,
                    onClick = { onScreenSelected(screen) },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == screen) screen.selectedIcon else screen.unselectedIcon,
                            contentDescription = null
                        )
                    },
                    label = { Text(text = stringResource(screen.title)) },
                    navigationSuiteType = navigationSuiteType
                )
            }
        },
        modifier = modifier,
        navigationSuiteType = navigationSuiteType,
        state = navigationSuiteState,
        // Top aligned items collide with the collapsed title bar next to the rail
        navigationItemVerticalArrangement = Arrangement.Center,
    ) {
        content(showNavigation && navigationSuiteType in verticalNavigationSuiteTypes)
    }
}

@Preview(name = "Navigation suite preview")
@Composable
fun NCPNavigationSuitePreview() {
    NextcloudPasswordsTheme {
        NCPNavigationSuite(
            allScreens = NCPScreen.entries.filter { !it.hidden },
            currentScreen = NCPScreen.Passwords,
            onScreenSelected = {},
            showNavigation = true,
            content = {}
        )
    }
}
