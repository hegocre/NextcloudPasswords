package com.hegocre.nextcloudpasswords.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.api.FoldersApi
import com.hegocre.nextcloudpasswords.data.folder.Folder
import com.hegocre.nextcloudpasswords.data.password.DeletedPassword
import com.hegocre.nextcloudpasswords.data.password.NewPassword
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.data.password.UpdatedPassword
import com.hegocre.nextcloudpasswords.data.viewmodels.PasswordsViewModel
import com.hegocre.nextcloudpasswords.ui.NCPScreen
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import com.hegocre.nextcloudpasswords.utils.decryptFolders
import com.hegocre.nextcloudpasswords.utils.decryptPasswords
import com.hegocre.nextcloudpasswords.utils.encryptValue
import com.hegocre.nextcloudpasswords.utils.sha1Hash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
@Composable
fun NCPNavHost(
    navController: NavHostController,
    passwordsViewModel: PasswordsViewModel,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    modalSheetState: SheetState? = null,
    searchVisibility: Boolean? = null,
    closeSearch: (() -> Unit)? = null,
    onPasswordClick: ((Password) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(all = 0.dp)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val passwords by passwordsViewModel.passwords.observeAsState()
    val folders by passwordsViewModel.folders.observeAsState()
    val keychain by passwordsViewModel.csEv1Keychain.observeAsState()
    val isRefreshing by passwordsViewModel.isRefreshing.collectAsState()
    val isUpdating by passwordsViewModel.isUpdating.collectAsState()

    val passwordsDecryptionState by produceState(
        initialValue = ListDecryptionState(isLoading = true),
        key1 = passwords, key2 = keychain
    ) {
        value = ListDecryptionState(decryptedList = passwords?.let { passwordList ->
            passwordList.decryptPasswords(context, keychain).sortedBy { it.label.lowercase() }
        } ?: emptyList())
    }

    val foldersDecryptionState by produceState(
        initialValue = ListDecryptionState(isLoading = true),
        key1 = folders, key2 = keychain
    ) {
        value = ListDecryptionState(decryptedList = folders?.let { folderList ->
            folderList.decryptFolders(keychain).sortedBy { it.label.lowercase() }
        } ?: emptyList())
    }

    val onFolderClick: (Folder) -> Unit = { folder ->
        navController.navigate("${NCPScreen.Folders.name}/${folder.id}")
    }

    val startDestination by PreferencesManager.getInstance(context).getStartScreen()
        .collectAsState(NCPScreen.Passwords.name, context = Dispatchers.IO)

    val filteredPasswordList = remember(passwordsDecryptionState.decryptedList, searchQuery) {
        passwordsDecryptionState.decryptedList?.filter {
            !it.hidden && !it.trashed && (it.label.lowercase().contains(searchQuery.lowercase()) ||
                    it.url.lowercase().contains(searchQuery.lowercase()))
        }
    }
    val filteredFolderList = remember(foldersDecryptionState.decryptedList, searchQuery) {
        foldersDecryptionState.decryptedList?.filter {
            !it.hidden && !it.trashed && it.label.lowercase().contains(searchQuery.lowercase())
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NCPScreen.Passwords.name) {
            NCPNavHostComposable(
                modalSheetState = modalSheetState,
                searchVisibility = searchVisibility,
                closeSearch = closeSearch
            ) {
                when {
                    passwordsDecryptionState.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    passwordsDecryptionState.decryptedList != null -> {
                        RefreshListBody(
                            isRefreshing = isRefreshing,
                            onRefresh = { passwordsViewModel.sync() },
                            indicatorPadding = contentPadding
                        ) {
                            MixedLazyColumn(
                                passwords = filteredPasswordList,
                                onPasswordClick = onPasswordClick,
                                contentPadding = contentPadding
                            )
                        }
                    }
                }
            }
        }

        composable(NCPScreen.Favorites.name) {
            val filteredFavoritePasswords = remember(filteredPasswordList) {
                filteredPasswordList?.filter { it.favorite }
            }
            NCPNavHostComposable(
                modalSheetState = modalSheetState,
                searchVisibility = searchVisibility,
                closeSearch = closeSearch
            ) {
                when {
                    passwordsDecryptionState.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    passwordsDecryptionState.decryptedList != null -> {
                        RefreshListBody(
                            isRefreshing = isRefreshing,
                            onRefresh = { passwordsViewModel.sync() },
                            indicatorPadding = contentPadding
                        ) {
                            MixedLazyColumn(
                                passwords = filteredFavoritePasswords,
                                onPasswordClick = onPasswordClick,
                                contentPadding = contentPadding
                            )
                        }
                    }
                }
            }
        }

        composable(NCPScreen.Folders.name) {
            NCPNavHostComposable(
                modalSheetState = modalSheetState,
                searchVisibility = searchVisibility,
                closeSearch = closeSearch
            ) {
                val filteredPasswordsParentFolder = remember(filteredPasswordList) {
                    filteredPasswordList?.filter {
                        it.folder == FoldersApi.DEFAULT_FOLDER_UUID &&
                                (it.label.lowercase().contains(searchQuery.lowercase()) ||
                                        it.url.lowercase().contains(searchQuery.lowercase()))
                    }
                }
                val filteredFoldersParentFolder = remember(filteredFolderList) {
                    filteredFolderList?.filter {
                        it.parent == FoldersApi.DEFAULT_FOLDER_UUID
                    }
                }
                SideEffect {
                    passwordsViewModel.setVisibleFolder(foldersDecryptionState.decryptedList
                        ?.find { it.id == FoldersApi.DEFAULT_FOLDER_UUID })
                }
                when {
                    foldersDecryptionState.isLoading || passwordsDecryptionState.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    foldersDecryptionState.decryptedList != null
                            && passwordsDecryptionState.decryptedList != null -> {
                        RefreshListBody(
                            isRefreshing = isRefreshing,
                            onRefresh = { passwordsViewModel.sync() },
                            indicatorPadding = contentPadding
                        ) {
                            MixedLazyColumn(
                                passwords = filteredPasswordsParentFolder,
                                folders = filteredFoldersParentFolder,
                                onPasswordClick = onPasswordClick,
                                onFolderClick = onFolderClick,
                                contentPadding = contentPadding
                            )
                        }
                    }
                }
            }
        }

        composable(
            route = "${NCPScreen.Folders.name}/{folder_uuid}",
            arguments = listOf(
                navArgument("folder_uuid") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val folderUuid =
                entry.arguments?.getString("folder_uuid") ?: FoldersApi.DEFAULT_FOLDER_UUID
            val filteredPasswordsSelectedFolder = remember(filteredPasswordList) {
                filteredPasswordList?.filter {
                    it.folder == folderUuid &&
                            (it.label.lowercase().contains(searchQuery.lowercase()) ||
                                    it.url.lowercase().contains(searchQuery.lowercase()))
                }
            }
            val filteredFoldersSelectedFolder = remember(filteredFolderList) {
                filteredFolderList?.filter {
                    it.parent == folderUuid
                }
            }
            NCPNavHostComposable(
                modalSheetState = modalSheetState,
                searchVisibility = searchVisibility,
                closeSearch = closeSearch
            ) {
                when {
                    passwordsDecryptionState.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    passwordsDecryptionState.decryptedList != null -> {
                        LaunchedEffect(folderUuid, passwordsDecryptionState) {
                            if (foldersDecryptionState.decryptedList?.isEmpty() == false) {
                                passwordsViewModel.setVisibleFolder(foldersDecryptionState.decryptedList
                                    ?.find { it.id == folderUuid })
                            }
                        }

                        RefreshListBody(
                            isRefreshing = isRefreshing,
                            onRefresh = { passwordsViewModel.sync() },
                            indicatorPadding = contentPadding
                        ) {
                            MixedLazyColumn(
                                passwords = filteredPasswordsSelectedFolder,
                                folders = filteredFoldersSelectedFolder,
                                onPasswordClick = onPasswordClick,
                                onFolderClick = onFolderClick,
                                contentPadding = contentPadding
                            )
                        }
                    }
                }
            }
        }

        composable(
            route = "${NCPScreen.Edit.name}/{password_uuid}",
            arguments = listOf(
                navArgument("password_uuid") {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            BackHandler(enabled = isUpdating) {
                // Block back gesture when updating to avoid data loss
                return@BackHandler
            }

            val passwordUuid = entry.arguments?.getString("password_uuid")
            val selectedPassword = remember(filteredPasswordList, passwordUuid) {
                if (passwordUuid == "none") {
                    null
                } else {
                    filteredPasswordList?.firstOrNull {
                        it.id == passwordUuid
                    }
                }
            }
            NCPNavHostComposable(
                modalSheetState = modalSheetState,
                searchVisibility = searchVisibility,
                closeSearch = closeSearch
            ) {
                when {
                    passwordsDecryptionState.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }

                    passwordsDecryptionState.decryptedList != null -> {
                        val editablePasswordState = rememberEditablePasswordState(selectedPassword)

                        EditablePasswordView(
                            editablePasswordState = editablePasswordState,
                            onSavePassword = {
                                if (selectedPassword == null) {
                                    val newPassword = keychain?.let {
                                        NewPassword(
                                            password = editablePasswordState.password.encryptValue(
                                                it.current,
                                                it
                                            ),
                                            label = editablePasswordState.label.encryptValue(
                                                it.current,
                                                it
                                            ),
                                            username = editablePasswordState.username.encryptValue(
                                                it.current,
                                                it
                                            ),
                                            url = editablePasswordState.url.encryptValue(
                                                it.current,
                                                it
                                            ),
                                            notes = editablePasswordState.notes.encryptValue(
                                                it.current,
                                                it
                                            ),
                                            customFields = editablePasswordState.customFields.encryptValue(
                                                it.current,
                                                it
                                            ),
                                            hash = editablePasswordState.password.sha1Hash(),
                                            cseType = "CSEv1r1",
                                            cseKey = it.current,
                                            folder = FoldersApi.DEFAULT_FOLDER_UUID,
                                            edited = 0,
                                            hidden = false,
                                            favorite = editablePasswordState.favorite
                                        )
                                    } ?: NewPassword(
                                        password = editablePasswordState.password,
                                        label = editablePasswordState.label,
                                        username = editablePasswordState.username,
                                        url = editablePasswordState.url,
                                        notes = editablePasswordState.notes,
                                        customFields = editablePasswordState.customFields,
                                        hash = editablePasswordState.password.sha1Hash(),
                                        cseType = "none",
                                        cseKey = "",
                                        folder = FoldersApi.DEFAULT_FOLDER_UUID,
                                        edited = 0,
                                        hidden = false,
                                        favorite = editablePasswordState.favorite
                                    )
                                    coroutineScope.launch {
                                        if (passwordsViewModel.createPassword(newPassword)
                                                .await()
                                        ) {
                                            navController.navigateUp()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                R.string.password_saving_failed,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                } else {
                                    val updatedPassword = keychain?.let {
                                        UpdatedPassword(
                                            id = selectedPassword.id,
                                            revision = selectedPassword.revision,
                                            password = editablePasswordState.password.encryptValue(
                                                it.current,
                                                it
                                            ),
                                            label = editablePasswordState.label.encryptValue(
                                                it.current,
                                                it
                                            ),
                                            username = editablePasswordState.username.encryptValue(
                                                it.current,
                                                it
                                            ),
                                            url = editablePasswordState.url.encryptValue(
                                                it.current,
                                                it
                                            ),
                                            notes = editablePasswordState.notes.encryptValue(
                                                it.current,
                                                it
                                            ),
                                            customFields = editablePasswordState.customFields.encryptValue(
                                                it.current,
                                                it
                                            ),
                                            hash = editablePasswordState.password.sha1Hash(),
                                            cseType = "CSEv1r1",
                                            cseKey = it.current,
                                            folder = selectedPassword.folder,
                                            edited = if (editablePasswordState.password == selectedPassword.password) selectedPassword.edited else 0,
                                            hidden = selectedPassword.hidden,
                                            favorite = editablePasswordState.favorite
                                        )
                                    } ?: UpdatedPassword(
                                        id = selectedPassword.id,
                                        revision = selectedPassword.revision,
                                        password = editablePasswordState.password,
                                        label = editablePasswordState.label,
                                        username = editablePasswordState.username,
                                        url = editablePasswordState.url,
                                        notes = editablePasswordState.notes,
                                        customFields = editablePasswordState.customFields,
                                        hash = editablePasswordState.password.sha1Hash(),
                                        cseType = "none",
                                        cseKey = "",
                                        folder = selectedPassword.folder,
                                        edited = if (editablePasswordState.password == selectedPassword.password) selectedPassword.edited else 0,
                                        hidden = selectedPassword.hidden,
                                        favorite = editablePasswordState.favorite
                                    )
                                    coroutineScope.launch {
                                        if (passwordsViewModel.updatePassword(updatedPassword)
                                                .await()
                                        ) {
                                            navController.navigateUp()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                R.string.password_saving_failed,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            },
                            onDeletePassword = if (selectedPassword == null) null
                            else {
                                {
                                    val deletedPassword = DeletedPassword(
                                        id = selectedPassword.id,
                                        revision = selectedPassword.revision
                                    )
                                    coroutineScope.launch {
                                        if (passwordsViewModel.deletePassword(deletedPassword)
                                                .await()
                                        ) {
                                            navController.navigateUp()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                R.string.password_deleting_failed,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            },
                            isUpdating = isUpdating
                        )
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
    searchVisibility: Boolean? = null,
    closeSearch: (() -> Unit)? = null,
    content: @Composable () -> Unit = { }
) {
    BackHandler(enabled = searchVisibility == true) {
        closeSearch?.invoke()
    }
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