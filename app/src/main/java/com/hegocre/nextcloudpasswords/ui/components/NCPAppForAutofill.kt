package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.api.FoldersApi
import com.hegocre.nextcloudpasswords.ui.NCPScreen
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.ui.viewmodels.PasswordsViewModel
import kotlinx.coroutines.launch
import com.hegocre.nextcloudpasswords.services.autofill.SaveData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextcloudPasswordsAppForAutofill(
    passwordsViewModel: PasswordsViewModel,
    onLogOut: () -> Unit,
    isAutofillRequest: Boolean = false,
    defaultSearchQuery: String = "",
    passwordId: String? = null,
    saveData: SaveData? = null,
    replyAutofill: ((String, String, String) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()

    val navController = rememberNavController()
    val backstackEntry = navController.currentBackStackEntryAsState()
    val currentScreen = NCPScreen.fromRoute(
        backstackEntry.value?.destination?.route
    )

    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    val modalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val needsMasterPassword by passwordsViewModel.needsMasterPassword.collectAsState()
    val masterPasswordInvalid by passwordsViewModel.masterPasswordInvalid.collectAsState()

    val sessionOpen by passwordsViewModel.sessionOpen.collectAsState()
    val showSessionOpenError by passwordsViewModel.showSessionOpenError.collectAsState()
    val isRefreshing by passwordsViewModel.isRefreshing.collectAsState()

    var showLogOutDialog by rememberSaveable { mutableStateOf(false) }
    var showAddElementDialog by rememberSaveable { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (isAutofillRequest) searchExpanded = true
    }
    val (searchQuery, setSearchQuery) = rememberSaveable { mutableStateOf(defaultSearchQuery) }

    val server = remember {
        passwordsViewModel.server
    }

    NextcloudPasswordsTheme {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState()
        )

        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .imePadding(),
            floatingActionButton = {
                AnimatedVisibility(
                    visible = currentScreen != NCPScreen.PasswordEdit &&
                            currentScreen != NCPScreen.FolderEdit && sessionOpen,
                    enter = scaleIn(),
                    exit = scaleOut(),
                ) {
                    FloatingActionButton(
                        onClick = { showAddElementDialog = true },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(id = R.string.action_create_element)
                        )
                    }
                }
            }
        ) { innerPadding ->
            NCPNavHostForAutofill(
                modifier = Modifier.padding(innerPadding),
                navController = navController,
                passwordsViewModel = passwordsViewModel,
                searchQuery = searchQuery,
                passwordId = passwordId,
                isAutofillRequest = isAutofillRequest,
                saveData = saveData,
                modalSheetState = modalSheetState,
                replyAutofill = replyAutofill,
            )

            if (showLogOutDialog) {
                LogOutDialog(
                    onDismissRequest = { showLogOutDialog = false },
                    onConfirmButton = onLogOut
                )
            }

            if (showAddElementDialog) {
                AddElementDialog(
                    onPasswordAdd = {
                        navController.navigate("${NCPScreen.PasswordEdit.name}/none")
                        showAddElementDialog = false
                    },
                    onFolderAdd = {
                        navController.navigate("${NCPScreen.FolderEdit.name}/none")
                        showAddElementDialog = false
                    },
                    onDismissRequest = {
                        showAddElementDialog = false
                    }
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
                    errorText = if (masterPasswordInvalid) stringResource(R.string.error_invalid_password) else "",
                    onDismissRequest = { }
                )
            }

            if (openBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { openBottomSheet = false },
                    contentWindowInsets = { WindowInsets.navigationBars },
                    sheetState = modalSheetState
                ) {
                    PasswordItem(
                        passwordInfo = passwordsViewModel.visiblePassword.value,
                        onEditPassword = if (sessionOpen) {
                            {
                                coroutineScope.launch {
                                    modalSheetState.hide()
                                }.invokeOnCompletion {
                                    if (!modalSheetState.isVisible) {
                                        openBottomSheet = false
                                    }
                                }
                                navController.navigate("${NCPScreen.PasswordEdit.name}/${passwordsViewModel.visiblePassword.value?.first?.id ?: "none"}")
                            }
                        } else null,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}