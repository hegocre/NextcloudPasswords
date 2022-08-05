package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hegocre.nextcloudpasswords.api.FoldersApi
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.data.viewmodels.PasswordsViewModel
import com.hegocre.nextcloudpasswords.ui.NCPScreen
import com.hegocre.nextcloudpasswords.ui.theme.ThemeProvider
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
fun NextcloudPasswordsApp(
    passwordsViewModel: PasswordsViewModel,
    onLogOut: () -> Unit,
    isAutofillRequest: Boolean = false,
    defaultSearchQuery: String = "",
    onPasswordClick: ((Password) -> Unit)? = null
) {
    val context = LocalContext.current

    val theme by ThemeProvider.getInstance(context).currentTheme.collectAsState()

    theme.Theme {
        val navController = rememberNavController()
        val backstackEntry = navController.currentBackStackEntryAsState()
        val currentScreen = NCPScreen.fromRoute(
            backstackEntry.value?.destination?.route
        )

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

        val needsMasterPassword by passwordsViewModel.needsMasterPassword.collectAsState()
        val masterPasswordInvalid by passwordsViewModel.masterPasswordInvalid.collectAsState()

        var logOutDialogOpen by rememberSaveable { mutableStateOf(false) }

        val scope = rememberCoroutineScope()
        val keyboardController = LocalSoftwareKeyboardController.current

        var searchExpanded by rememberSaveable { mutableStateOf(isAutofillRequest) }
        val (searchQuery, setSearchQuery) = rememberSaveable { mutableStateOf(defaultSearchQuery) }

        val onPwClick: (Password) -> Unit = onPasswordClick ?: { password ->
            passwordsViewModel.setVisiblePassword(password)
            keyboardController?.hide()
            scope.launch {
                drawerState.open()
            }
        }

        ModalNavigationDrawer(
            drawerContent = {
                Column(Modifier.defaultMinSize(minHeight = 5.dp)) {
                    PasswordItem(
                        password = passwordsViewModel.visiblePassword.value,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            },
            drawerState = drawerState,
            gesturesEnabled = passwordsViewModel.visiblePassword.value != null
        ) {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
                rememberSplineBasedDecay(),
                rememberTopAppBarState()
            )

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    NCPSearchTopBar(
                        title = when (currentScreen) {
                            NCPScreen.Passwords, NCPScreen.Favorites -> stringResource(currentScreen.title)
                            NCPScreen.Folders -> {
                                passwordsViewModel.visibleFolder.value?.let {
                                    if (it.id == FoldersApi.DEFAULT_FOLDER_UUID) stringResource(
                                        currentScreen.title
                                    )
                                    else it.label
                                } ?: stringResource(currentScreen.title)
                            }
                        },
                        searchQuery = searchQuery,
                        setSearchQuery = setSearchQuery,
                        isAutofill = isAutofillRequest,
                        searchExpanded = searchExpanded,
                        onSearchClick = { searchExpanded = true },
                        onSearchCloseClick = {
                            searchExpanded = false
                            setSearchQuery("")
                        },
                        onLogoutClick = { logOutDialogOpen = true },
                        scrollBehavior = scrollBehavior
                    )
                },
                bottomBar = {
                    NCPBottomNavigation(
                        allScreens = NCPScreen.values().toList(),
                        currentScreen = currentScreen,
                        onScreenSelected = { screen ->
                            navController.navigate(screen.name) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                },
                floatingActionButton = {
                    if (!isAutofillRequest) {
                        FloatingActionButton(
                            onClick = { },
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add")
                        }
                    }
                }
            ) { innerPadding ->
                NCPNavHost(
                    modifier = Modifier
                        .padding(innerPadding),
                    navController = navController,
                    passwordsViewModel = passwordsViewModel,
                    searchQuery = searchQuery,
                    drawerState = drawerState,
                    onPasswordClick = onPwClick,
                    searchVisibility = searchExpanded,
                    closeSearch = {
                        searchExpanded = false
                        setSearchQuery("")
                    },
                )

                if (logOutDialogOpen) {
                    LogOutDialog(
                        onDismissRequest = { logOutDialogOpen = false },
                        onConfirmButton = onLogOut
                    )
                }
                if (needsMasterPassword) {
                    val (masterPassword, setMasterPassword) = rememberSaveable {
                        mutableStateOf("")
                    }
                    val (savePassword, setSavePassword) = rememberSaveable {
                        mutableStateOf(false)
                    }
                    MasterPasswordDialog(
                        masterPassword = masterPassword,
                        setMasterPassword = setMasterPassword,
                        savePassword = savePassword,
                        setSavePassword = setSavePassword,
                        onOkClick = {
                            passwordsViewModel.setMasterPassword(masterPassword, savePassword)
                            setMasterPassword("")
                        },
                        errorText = if (masterPasswordInvalid) "Invalid password" else "",
                        onDismissRequest = { passwordsViewModel.dismissMasterPasswordDialog() }
                    )
                }
            }
        }
    }
}