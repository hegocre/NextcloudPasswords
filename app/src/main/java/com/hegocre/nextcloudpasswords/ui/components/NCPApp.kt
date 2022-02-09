package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hegocre.nextcloudpasswords.api.FoldersApi
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.data.viewmodels.PasswordsViewModel
import com.hegocre.nextcloudpasswords.ui.NCPScreen
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun NextcloudPasswordsApp(
    passwordsViewModel: PasswordsViewModel,
    onLogOut: () -> Unit,
    isAutofillRequest: Boolean = false,
    defaultSearchQuery: String = "",
    onPasswordClick: ((Password) -> Unit)? = null
) {
    NextcloudPasswordsTheme {
        val navController = rememberNavController()
        val backstackEntry = navController.currentBackStackEntryAsState()
        val currentScreen = NCPScreen.fromRoute(
            backstackEntry.value?.destination?.route
        )

        val bottomState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)

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
                bottomState.show()
            }
        }

        ModalBottomSheetLayout(
            sheetContent = {
                Column(Modifier.defaultMinSize(minHeight = 5.dp)) {
                    Spacer(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .height(4.dp)
                            .width(48.dp)
                            .background(
                                color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                                shape = MaterialTheme.shapes.small
                            )
                            .align(Alignment.CenterHorizontally)
                    )

                    PasswordItem(
                        password = passwordsViewModel.visiblePassword.value,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            },
            sheetState = bottomState,
            sheetBackgroundColor = MaterialTheme.colors.background,
            sheetElevation = if (MaterialTheme.colors.isLight) AppBarDefaults.TopAppBarElevation else 0.dp,
            sheetShape = MaterialTheme.shapes.large.copy(
                topStart = CornerSize(24.dp),
                topEnd = CornerSize(24.dp)
            ),
            scrimColor = Color.Black.copy(alpha = 0.32f)
        ) {
            Scaffold(
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
                        onLogoutClick = { logOutDialogOpen = true }
                    )
                },
                bottomBar = {
                    NCPBottomNavigation(
                        allScreens = NCPScreen.values().toList(),
                        currentScreen = currentScreen,
                        onScreenSelected = { screen -> navController.navigate(screen.name) }
                    )
                },
                floatingActionButton = if (!isAutofillRequest) {
                    {
                        FloatingActionButton(onClick = { }) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add")
                        }
                    }
                } else {
                    {}
                }
            ) { innerPadding ->
                NCPNavHost(
                    navController = navController,
                    passwordsViewModel = passwordsViewModel,
                    searchQuery = searchQuery,
                    modifier = Modifier.padding(innerPadding),
                    bottomState = bottomState,
                    onPasswordClick = onPwClick,
                    searchVisibility = searchExpanded,
                    closeSearch = {
                        searchExpanded = false
                        setSearchQuery("")
                    }
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