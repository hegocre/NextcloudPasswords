package com.hegocre.nextcloudpasswords.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.api.FoldersApi
import com.hegocre.nextcloudpasswords.data.password.DeletedPassword
import com.hegocre.nextcloudpasswords.data.serversettings.ServerSettings
import com.hegocre.nextcloudpasswords.ui.NCPScreen
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.ui.viewmodels.PasswordsViewModel
import com.hegocre.nextcloudpasswords.utils.AutofillData
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextcloudPasswordsApp(
    passwordsViewModel: PasswordsViewModel,
    onLogOut: () -> Unit,
    autofillData: AutofillData?,
    replyAutofill: ((String, String, String) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val showOTPTab by PreferencesManager.getInstance(context).getShowOTPTab()
        .collectAsState(false, context = Dispatchers.IO)

    val navController = rememberNavController()
    val backstackEntry = navController.currentBackStackEntryAsState()
    val currentScreen = NCPScreen.fromRoute(
        backstackEntry.value?.destination?.route
    )

    val keychain by passwordsViewModel.csEv1Keychain.observeAsState()
    val serverSettings by passwordsViewModel.serverSettings.observeAsState(initial = ServerSettings())
    val passwords by passwordsViewModel.passwords.observeAsState(initial = listOf())

    var showPasswordDetails by rememberSaveable { mutableStateOf(false) }
    val modalSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    val needsMasterPassword by passwordsViewModel.needsMasterPassword.collectAsState()
    val masterPasswordInvalid by passwordsViewModel.masterPasswordInvalid.collectAsState()

    val sessionOpen by passwordsViewModel.sessionOpen.collectAsState()
    val showSessionOpenError by passwordsViewModel.showSessionOpenError.collectAsState()
    val isRefreshing by passwordsViewModel.isRefreshing.collectAsState()

    var showLogOutDialog by rememberSaveable { mutableStateOf(false) }

    val showLocalNetworkAccessDialog by passwordsViewModel.showLocalNetworkAccessDialog.collectAsState()
    val localNetworkPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            passwordsViewModel.sync()
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (autofillData != null && autofillData is AutofillData.ChoosePwd) searchExpanded = true
    }
    val (searchQuery, setSearchQuery) = rememberSaveable { mutableStateOf(
        when (autofillData) {
            is AutofillData.ChoosePwd -> autofillData.searchHint
            is AutofillData.SaveAutofill -> autofillData.searchHint
            is AutofillData.Save -> autofillData.searchHint
            else -> ""
        }    
    )}

    val server = remember {
        passwordsViewModel.server
    }

    NextcloudPasswordsTheme {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState()
        )

        val addElementButton: @Composable (menuBelowButton: Boolean) -> Unit = { menuBelowButton ->
            AnimatedVisibility(
                visible = currentScreen != NCPScreen.PasswordEdit &&
                        currentScreen != NCPScreen.FolderEdit && sessionOpen
                        && currentScreen != NCPScreen.TrashedPasswords,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                AddElementButton(
                    onPasswordAdd = { navController.navigate("${NCPScreen.PasswordEdit.name}/none") },
                    onFolderAdd = if (autofillData is AutofillData.IsSave) null else {
                        { navController.navigate("${NCPScreen.FolderEdit.name}/none") }
                    },
                    menuBelowButton = menuBelowButton
                )
            }
        }

        val showsDetailPane =
            showsPasswordDetailPane(currentWindowAdaptiveInfoV2().windowSizeClass, currentScreen)

        val passwordDetails: @Composable (onDismiss: () -> Unit, modifier: Modifier) -> Unit =
            { onDismiss, modifier ->
                val currentPasswordInfo = passwordsViewModel.visiblePassword.value?.let { visible ->
                    val updatedPassword = passwords.find { it.id == visible.first.id }
                    if (updatedPassword != null) {
                        Pair(
                            // Update the revision, so that multiple HOTP counter updates work
                            visible.first.copy(
                                revision = updatedPassword.revision,
                                updated = updatedPassword.updated,
                            ),
                            visible.second
                        )
                    } else visible
                }
                // The details remember state per password, so a pane reused for another one must start fresh
                key(currentPasswordInfo?.first?.id) {
                    PasswordItem(
                        passwordInfo = currentPasswordInfo,
                        onEditPassword = if (sessionOpen) {
                            {
                                onDismiss()
                                navController.navigate("${NCPScreen.PasswordEdit.name}/${passwordsViewModel.visiblePassword.value?.first?.id ?: "none"}")
                            }
                        } else null,
                        onDeletePassword = if (sessionOpen) {
                            passwordsViewModel.visiblePassword.value?.first?.let {
                                {
                                    onDismiss()
                                    coroutineScope.launch {
                                        if (!passwordsViewModel.deletePassword(
                                                DeletedPassword(it.id, it.revision)
                                            ).await()) {
                                            showPasswordDetails = true
                                        }
                                    }
                                }
                            }
                        } else null,
                        onRestorePassword = if (sessionOpen) {
                            passwordsViewModel.visiblePassword.value?.first?.let {
                                {
                                    onDismiss()
                                    coroutineScope.launch {
                                        if (!passwordsViewModel.restorePassword(
                                                DeletedPassword(it.id, it.revision)
                                            ).await()) {
                                            showPasswordDetails = true
                                        }
                                    }
                                }
                            }
                        } else null,
                        updatePassword = { updatedPassword, shouldEncrypt, onSuccess, onFailure ->
                            coroutineScope.launch {
                                val updatedPwd = updatedPassword.let {
                                    val currentKeychain = keychain
                                    if (shouldEncrypt && currentKeychain != null) {
                                        it.encrypt(currentKeychain.current, currentKeychain)
                                    } else it
                                }

                                if (passwordsViewModel.updatePassword(updatedPwd).await()) {
                                    onSuccess()
                                } else {
                                    onFailure()
                                }
                            }

                        },
                        modifier = modifier
                    )
                }
            }

        NCPNavigationSuite(
            allScreens = NCPScreen.entries.filter {
                !it.hidden && !(it.name == NCPScreen.OTP.name && !showOTPTab)
            },
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
            showNavigation = (autofillData == null || autofillData is AutofillData.ChoosePwd)
                    && currentScreen != NCPScreen.PasswordEdit
                    && currentScreen != NCPScreen.FolderEdit
                    && currentScreen != NCPScreen.TrashedPasswords,
            primaryAction = { addElementButton(true) }
        ) { showsNavigationRail ->
            // The detail pane sits next to the Scaffold, so that the top bar only spans the list
            Row {
                Scaffold(
                    modifier = Modifier
                        .weight(1f)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .imePadding(),
                    topBar = {
                        if (autofillData == null || autofillData is AutofillData.ChoosePwd) {
                            if (currentScreen != NCPScreen.PasswordEdit && currentScreen != NCPScreen.FolderEdit && currentScreen != NCPScreen.TrashedPasswords) {
                                NCPSearchTopBar(
                                    username = server.username,
                                    serverAddress = server.url,
                                    title = when (currentScreen) {
                                        NCPScreen.Passwords -> stringResource(currentScreen.title) +
                                                (passwordsViewModel.totalPasswordCount.value?.let { " ($it)" } ?: "")
                                        NCPScreen.Favorites -> stringResource(currentScreen.title) +
                                                (passwordsViewModel.totalFavoritePasswordCount.value?.let { " ($it)" } ?: "")
                                        NCPScreen.Folders -> {
                                            (passwordsViewModel.visibleFolder.value?.let {
                                                if (it.id == FoldersApi.DEFAULT_FOLDER_UUID)
                                                    stringResource(currentScreen.title)
                                                else
                                                    it.label
                                            } ?: stringResource(currentScreen.title)) +
                                                    (passwordsViewModel.visibleFolderPasswordCount.value?.let { " ($it)" } ?: "")
                                        }
                                        NCPScreen.OTP -> stringResource(currentScreen.title) +
                                                (passwordsViewModel.totalOTPPasswordCount.value?.let { " ($it)" } ?: "")
                                    },
                                    userAvatar = { size ->
                                        Image(
                                            painter = passwordsViewModel.getPainterForAvatar(),
                                            contentDescription = "",
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .size(size)
                                        )
                                    },
                                    searchQuery = searchQuery,
                                    setSearchQuery = setSearchQuery,
                                    autofillData = autofillData,
                                    searchExpanded = searchExpanded,
                                    onSearchClick = { searchExpanded = true },
                                    onSearchCloseClick = {
                                        searchExpanded = false
                                        setSearchQuery("")
                                    },
                                    onTrashedPasswordsClick = {
                                        navController.navigate(NCPScreen.TrashedPasswords.name)
                                    },
                                    onLogoutClick = { showLogOutDialog = true },
                                    scrollBehavior = scrollBehavior,
                                )
                            } else {
                                TopAppBar(
                                    title = { Text(text = stringResource(id = currentScreen.title)) },
                                    navigationIcon = {
                                        IconButton(onClick = { navController.navigateUp() }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = stringResource(id = R.string.navigation_back)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    },
                    bottomBar = {
                        if (autofillData == null || autofillData is AutofillData.ChoosePwd) {
                            AnimatedVisibility(visible = !sessionOpen && showSessionOpenError && !isRefreshing) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.clickable { (passwordsViewModel.sync()) }
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.error_cannot_connect_to_server),
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        if (!showsNavigationRail) addElementButton(false)
                    }
                ) { innerPadding ->
                    NCPNavHost(
                        modifier = Modifier.padding(innerPadding),
                        navController = navController,
                        passwordsViewModel = passwordsViewModel,
                        searchQuery = searchQuery,
                        autofillData = autofillData,
                        modalSheetState = modalSheetState,
                        openPasswordDetails = { password, folderPath ->
                            passwordsViewModel.setVisiblePassword(password, folderPath)
                            keyboardController?.hide()
                            showPasswordDetails = true
                        },
                        replyAutofill = replyAutofill,
                        createPassword = { newPassword, onSuccess, onFailure ->
                            coroutineScope.launch {
                                val newPwd = newPassword.let {
                                    val currentKeychain = keychain
                                    if (currentKeychain != null && serverSettings.encryptionCse != 0) {
                                        it.encrypt(currentKeychain.current, currentKeychain)
                                    } else it
                                }

                                if (passwordsViewModel.createPassword(newPwd).await()) {
                                    onSuccess()
                                } else {
                                    onFailure()
                                }
                            }
                        },
                        updatePassword = { updatedPassword, shouldEncrypt, onSuccess, onFailure ->
                            coroutineScope.launch {
                                val updatedPwd = updatedPassword.let {
                                    val currentKeychain = keychain
                                    if (shouldEncrypt && currentKeychain != null) {
                                        it.encrypt(currentKeychain.current, currentKeychain)
                                    } else it
                                }

                                if (passwordsViewModel.updatePassword(updatedPwd).await()) {
                                    onSuccess()
                                } else {
                                    onFailure()
                                }
                            }
                        },
                        deletePassword = { deletedPassword, onSuccess, onFailure ->
                            coroutineScope.launch {
                                if (passwordsViewModel.deletePassword(deletedPassword).await()) {
                                    onSuccess()
                                } else {
                                    onFailure()
                                }
                            }
                        },
                        searchVisibility = searchExpanded,
                        closeSearch = {
                            searchExpanded = false
                            setSearchQuery("")
                        },
                    )

                    BackHandler(enabled = showsDetailPane && showPasswordDetails) {
                        showPasswordDetails = false
                    }

                    if (showLogOutDialog) {
                        LogOutDialog(
                            onDismissRequest = { showLogOutDialog = false },
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
                            errorText = if (masterPasswordInvalid) stringResource(R.string.error_invalid_password) else "",
                            onDismissRequest = { }
                        )
                    }

                    if (showLocalNetworkAccessDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                passwordsViewModel.dismissLocalNetworkAccessDialog()
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                                            localNetworkPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                                        }
                                        passwordsViewModel.dismissLocalNetworkAccessDialog()
                                    }
                                ) {
                                    Text(text = stringResource(id = R.string.grant_permission))
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        passwordsViewModel.dismissLocalNetworkAccessDialog()
                                    }
                                ) {
                                    Text(text = stringResource(id = android.R.string.cancel))
                                }
                            },
                            title = { Text(stringResource(id = R.string.permission_required)) },
                            text = { Text(text = stringResource(id = R.string.dialog_local_network_permission_message)) }
                        )
                    }

                    if (showPasswordDetails && !showsDetailPane) {
                        ModalBottomSheet(
                            onDismissRequest = { showPasswordDetails = false },
                            contentWindowInsets = { WindowInsets.navigationBars },
                            sheetState = modalSheetState
                        ) {
                            passwordDetails(
                                {
                                    coroutineScope.launch {
                                        modalSheetState.hide()
                                    }.invokeOnCompletion {
                                        if (!modalSheetState.isVisible) {
                                            showPasswordDetails = false
                                        }
                                    }
                                },
                                Modifier.padding(bottom = 16.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showsDetailPane && showPasswordDetails,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    PasswordDetailPane(onClose = { showPasswordDetails = false }) { contentModifier ->
                        passwordDetails({ showPasswordDetails = false }, contentModifier)
                    }
                }
            }
        }
    }
}