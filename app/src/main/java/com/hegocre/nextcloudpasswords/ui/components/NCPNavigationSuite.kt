package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
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
 * wider ones, such as unfolded foldables and tablets. [content] is told whether the rail is shown,
 * in which case [primaryAction] is placed in the rail header instead of being left to [content].
 */
@Composable
fun NCPNavigationSuite(
    allScreens: List<NCPScreen>,
    currentScreen: NCPScreen,
    onScreenSelected: (NCPScreen) -> Unit,
    showNavigation: Boolean,
    modifier: Modifier = Modifier,
    primaryAction: @Composable () -> Unit = {},
    content: @Composable (showsNavigationRail: Boolean) -> Unit
) {
    val navigationSuiteType =
        NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfoV2())
    val showsNavigationRail = showNavigation && navigationSuiteType in verticalNavigationSuiteTypes
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
        primaryActionContent = {
            if (showsNavigationRail) {
                // The rail places its header at the start edge, the items are inset by 20dp
                Box(modifier = Modifier.padding(start = 20.dp)) {
                    primaryAction()
                }
            }
        },
    ) {
        content(showsNavigationRail)
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
