package com.hegocre.nextcloudpasswords.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.api.FoldersApi
import com.hegocre.nextcloudpasswords.data.folder.DeletedFolder
import com.hegocre.nextcloudpasswords.data.folder.Folder
import com.hegocre.nextcloudpasswords.data.folder.NewFolder
import com.hegocre.nextcloudpasswords.data.folder.UpdatedFolder
import com.hegocre.nextcloudpasswords.data.password.DeletedPassword
import com.hegocre.nextcloudpasswords.data.password.NewPassword
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.data.password.UpdatedPassword
import com.hegocre.nextcloudpasswords.data.serversettings.ServerSettings
import com.hegocre.nextcloudpasswords.ui.NCPScreen
import com.hegocre.nextcloudpasswords.ui.viewmodels.PasswordsViewModel
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import com.hegocre.nextcloudpasswords.utils.decryptFolders
import com.hegocre.nextcloudpasswords.utils.decryptPasswords
import com.hegocre.nextcloudpasswords.utils.encryptValue
import com.hegocre.nextcloudpasswords.utils.sha1Hash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.hegocre.nextcloudpasswords.services.autofill.SaveData

@ExperimentalMaterial3Api
@Composable
fun NCPNavHostForAutofill(
    navController: NavHostController,
    passwordsViewModel: PasswordsViewModel,
    modifier: Modifier = Modifier,
    passwordId: String? = null,
    searchQuery: String = "",
    isAutofillRequest: Boolean,
    saveData: SaveData? = null,
    replyAutofill: ((String, String, String) -> Unit)? = null,
    modalSheetState: SheetState? = null,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val passwords by passwordsViewModel.passwords.observeAsState()
    val folders by passwordsViewModel.folders.observeAsState()
    val keychain by passwordsViewModel.csEv1Keychain.observeAsState()
    val isRefreshing by passwordsViewModel.isRefreshing.collectAsState()
    val isUpdating by passwordsViewModel.isUpdating.collectAsState()
    val serverSettings by passwordsViewModel.serverSettings.observeAsState(initial = ServerSettings())
    val sessionOpen by passwordsViewModel.sessionOpen.collectAsState()

    val passwordsDecryptionState by produceState(
        initialValue = ListDecryptionState(isLoading = true),
        key1 = passwords, key2 = keychain
    ) {
        value = ListDecryptionState(decryptedList = passwords?.decryptPasswords(keychain) ?: emptyList())
    }

    val baseFolderName = stringResource(R.string.top_level_folder_name)
    val reply: (Password) -> Unit = { password ->
        if (isAutofillRequest && replyAutofill != null && passwordId != null) {
            replyAutofill(password.label, password.username, password.password)
        }
    }

    val userStartDestination by PreferencesManager.getInstance(context).getStartScreen()
        .collectAsState(NCPScreen.Passwords.name, context = Dispatchers.IO)

    val startDestination = remember(isAutofillRequest, userStartDestination) {
        if (isAutofillRequest) NCPScreen.Passwords.name else userStartDestination
    }

    val filteredPasswordList = remember(passwordsDecryptionState.decryptedList) {
        passwordsDecryptionState.decryptedList?.filter {
            !it.hidden && !it.trashed && it.id == passwordId
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
    ) {
        composable(NCPScreen.Passwords.name) {
            NCPNavHostComposable(
                modalSheetState = modalSheetState,
            ) {
                when {
                    passwordsDecryptionState.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    passwordsDecryptionState.decryptedList != null -> {
                        PullToRefreshBody(
                            isRefreshing = isRefreshing,
                            onRefresh = { passwordsViewModel.sync() },
                        ) {
                            if (filteredPasswordList?.isEmpty() == true) {
                                if (searchQuery.isBlank()) NoContentText() else NoResultsText()
                            } else {
                                reply(filteredPasswordList!![0])
                            }
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun NCPNavHostComposable(
    modifier: Modifier = Modifier,
    modalSheetState: SheetState? = null,
    content: @Composable () -> Unit = { }
) {
    val scope = rememberCoroutineScope()
    BackHandler(enabled = modalSheetState?.isVisible ?: false) {
        scope.launch {
            modalSheetState?.hide()
        }
    }
    Box(modifier = modifier) {
        content()
    }
}