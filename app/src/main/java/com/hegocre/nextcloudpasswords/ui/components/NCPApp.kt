package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.api.FoldersApi
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.data.viewmodels.PasswordsViewModel
import com.hegocre.nextcloudpasswords.ui.NCPScreen
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
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
    val coroutineScope = rememberCoroutineScope()

    val navController = rememberNavController()
    val backstackEntry = navController.currentBackStackEntryAsState()
    val currentScreen = NCPScreen.fromRoute(
        backstackEntry.value?.destination?.route
    )

    val modalSheetState = rememberModalBottomSheetState()

    val needsMasterPassword by passwordsViewModel.needsMasterPassword.collectAsState()
    val masterPasswordInvalid by passwordsViewModel.masterPasswordInvalid.collectAsState()

    var logOutDialogOpen by rememberSaveable { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    var searchExpanded by rememberSaveable { mutableStateOf(isAutofillRequest) }
    val (searchQuery, setSearchQuery) = rememberSaveable { mutableStateOf(defaultSearchQuery) }

    val onPwClick: (Password) -> Unit = onPasswordClick ?: { password ->
        passwordsViewModel.setVisiblePassword(password)
        keyboardController?.hide()
        coroutineScope.launch {
            modalSheetState.show()
        }
    }

    NextcloudPasswordsTheme {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState()
        )

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                if (currentScreen != NCPScreen.Edit) {
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

                            else -> ""
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
                } else {
                    TopAppBar(
                        title = { Text(text = stringResource(id = R.string.edit)) },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "back"
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (currentScreen != NCPScreen.Edit) {
                    NCPBottomNavigation(
                        allScreens = NCPScreen.values().toList().filter { !it.hidden },
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
                }
            },
            floatingActionButton = {
                if (!isAutofillRequest && currentScreen != NCPScreen.Edit) {
                    FloatingActionButton(
                        onClick = { navController.navigate("${NCPScreen.Edit.name}/none") },
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add")
                    }
                }
            }
        ) { innerPadding ->
            NCPNavHost(
                modifier = Modifier.padding(innerPadding),
                navController = navController,
                passwordsViewModel = passwordsViewModel,
                searchQuery = searchQuery,
                modalSheetState = modalSheetState,
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

            if (modalSheetState.isVisible) {
                ModalBottomSheet(
                    onDismissRequest = {
                        coroutineScope.launch {
                            modalSheetState.hide()
                        }
                    }
                ) {
                    PasswordItem(
                        password = passwordsViewModel.visiblePassword.value,
                        onEditPassword = {
                            coroutineScope.launch {
                                modalSheetState.hide()
                            }
                            navController.navigate("${NCPScreen.Edit.name}/${passwordsViewModel.visiblePassword.value?.id ?: "none"}")
                        },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}