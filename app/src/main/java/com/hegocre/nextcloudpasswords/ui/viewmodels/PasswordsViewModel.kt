package com.hegocre.nextcloudpasswords.ui.viewmodels

import android.app.Application
import android.content.res.ColorStateList
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.api.ApiController
import com.hegocre.nextcloudpasswords.api.encryption.CSEv1Keychain
import com.hegocre.nextcloudpasswords.api.exceptions.ClientDeauthorizedException
import com.hegocre.nextcloudpasswords.api.exceptions.PWDv1ChallengeMasterKeyInvalidException
import com.hegocre.nextcloudpasswords.api.exceptions.PWDv1ChallengeMasterKeyNeededException
import com.hegocre.nextcloudpasswords.api.exceptions.PWDv1ChallengePasswordException
import com.hegocre.nextcloudpasswords.data.folder.DeletedFolder
import com.hegocre.nextcloudpasswords.data.folder.Folder
import com.hegocre.nextcloudpasswords.data.folder.FolderController
import com.hegocre.nextcloudpasswords.data.folder.NewFolder
import com.hegocre.nextcloudpasswords.data.folder.UpdatedFolder
import com.hegocre.nextcloudpasswords.data.password.DeletedPassword
import com.hegocre.nextcloudpasswords.data.password.NewPassword
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.data.password.PasswordController
import com.hegocre.nextcloudpasswords.data.password.UpdatedPassword
import com.hegocre.nextcloudpasswords.data.serversettings.ServerSettings
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Credentials
import java.net.MalformedURLException
import java.net.URL

class PasswordsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager.getInstance(application)

    private var masterPassword: MutableLiveData<String?> = MutableLiveData<String?>(null).also {
        it.value = preferencesManager.getMasterPassword()
    }

    private var _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean>
        get() = _isLocked.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing.asStateFlow()


    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean>
        get() = _isUpdating.asStateFlow()

    private val _needsMasterPassword = MutableStateFlow(false)
    val needsMasterPassword: StateFlow<Boolean>
        get() = _needsMasterPassword.asStateFlow()

    private val _masterPasswordInvalid = MutableStateFlow(false)
    val masterPasswordInvalid: StateFlow<Boolean>
        get() = _masterPasswordInvalid.asStateFlow()

    private val _clientDeauthorized = MutableLiveData(false)
    val clientDeauthorized: LiveData<Boolean>
        get() = _clientDeauthorized

    private val apiController = ApiController.getInstance(application)

    val sessionOpen
        get() = apiController.sessionOpen

    private val _showSessionOpenError = MutableStateFlow(false)
    val showSessionOpenError: StateFlow<Boolean>
        get() = _showSessionOpenError.asStateFlow()

    val csEv1Keychain: LiveData<CSEv1Keychain?>
        get() = apiController.csEv1Keychain

    val serverSettings: LiveData<ServerSettings>
        get() = apiController.serverSettings

    val passwords: LiveData<List<Password>>
        get() = PasswordController.getInstance(getApplication()).getPasswords()
    val folders: LiveData<List<Folder>>
        get() = FolderController.getInstance(getApplication()).getFolders()

    var visiblePassword = mutableStateOf<Password?>(null)
        private set
    var visibleFolder = mutableStateOf<Folder?>(null)
        private set

    init {
        if (!sessionOpen.value)
            openSession(masterPassword.value)
    }

    fun checkPasscode(passcode: String): Deferred<Boolean> {
        return viewModelScope.async {
            val correctPasscode = preferencesManager.getAppLockPasscode() ?: "0000"
            passcode == correctPasscode
        }
    }

    fun disableLock() {
        viewModelScope.launch {
            _isLocked.emit(false)
        }
    }

    private fun openSession(password: String?, saveKeychain: Boolean = false) {
        viewModelScope.launch {
            _isRefreshing.emit(true)
            try {
                if (apiController.openSession(password, saveKeychain)) {
                    sync()
                    _showSessionOpenError.emit(true)
                    return@launch
                }
                _showSessionOpenError.emit(true)
            } catch (ex: PWDv1ChallengeMasterKeyNeededException) {
                _needsMasterPassword.emit(true)
            } catch (ex: ClientDeauthorizedException) {
                _clientDeauthorized.postValue(true)
            } catch (ex: Exception) {
                when (ex) {
                    is PWDv1ChallengeMasterKeyInvalidException, is PWDv1ChallengePasswordException -> {
                        _needsMasterPassword.emit(true)
                        _masterPasswordInvalid.emit(true)
                        preferencesManager.setMasterPassword(null)
                    }
                    else -> {
                        _showSessionOpenError.emit(true)
                        ex.printStackTrace()
                    }
                }
            }
            _isRefreshing.emit(false)
        }
    }

    fun setMasterPassword(password: String, save: Boolean = false) {
        openSession(password, save)
        viewModelScope.launch {
            _needsMasterPassword.emit(false)
            _masterPasswordInvalid.emit(false)
        }
        if (save)
            preferencesManager.setMasterPassword(password)
    }

    fun sync() {
        if (sessionOpen.value) {
            viewModelScope.launch {
                _isRefreshing.emit(true)
                PasswordController.getInstance(getApplication()).syncPasswords()
                FolderController.getInstance(getApplication()).syncFolders()
                _isRefreshing.emit(false)
            }
        } else {
            openSession(masterPassword.value)
        }
    }

    fun dismissMasterPasswordDialog() {
        viewModelScope.launch {
            _needsMasterPassword.emit(false)
        }
    }

    fun setVisiblePassword(password: Password) {
        visiblePassword.value = password
    }

    fun setVisibleFolder(folder: Folder?) {
        visibleFolder.value = folder
    }

    suspend fun createPassword(newPassword: NewPassword): Deferred<Boolean> {
        return viewModelScope.async {
            _isUpdating.value = true
            if (!apiController.createPassword(newPassword)) {
                _isUpdating.value = false
                return@async false
            }
            sync()
            _isUpdating.value = false
            true
        }
    }

    suspend fun updatePassword(updatedPassword: UpdatedPassword): Deferred<Boolean> {
        return viewModelScope.async {
            _isUpdating.value = true
            if (!apiController.updatePassword(updatedPassword)) {
                _isUpdating.value = false
                return@async false
            }
            sync()
            _isUpdating.value = false
            true
        }
    }

    suspend fun deletePassword(deletedPassword: DeletedPassword): Deferred<Boolean> {
        return viewModelScope.async {
            _isUpdating.value = true
            if (!apiController.deletePassword(deletedPassword)) {
                _isUpdating.value = false
                return@async false
            }
            sync()
            _isUpdating.value = false
            true
        }
    }

    suspend fun generatePassword(): Deferred<String?> {
        return viewModelScope.async {
            return@async apiController.generatePassword()
        }
    }

    suspend fun createFolder(newFolder: NewFolder): Deferred<Boolean> {
        return viewModelScope.async {
            _isUpdating.value = true
            if (!apiController.createFolder(newFolder)) {
                _isUpdating.value = false
                return@async false
            }
            sync()
            _isUpdating.value = false
            true
        }
    }

    suspend fun updateFolder(updatedFolder: UpdatedFolder): Deferred<Boolean> {
        return viewModelScope.async {
            _isUpdating.value = true
            if (!apiController.updateFolder(updatedFolder)) {
                _isUpdating.value = false
                return@async false
            }
            sync()
            _isUpdating.value = false
            true
        }
    }

    suspend fun deleteFolder(deletedFolder: DeletedFolder): Deferred<Boolean> {
        return viewModelScope.async {
            _isUpdating.value = true
            if (!apiController.deleteFolder(deletedFolder)) {
                _isUpdating.value = false
                return@async false
            }
            sync()
            _isUpdating.value = false
            true
        }
    }

    @Composable
    fun getPainterForUrl(url: String): Painter {
        val context = LocalContext.current
        val domain = try {
            URL(url).host
        } catch (e: MalformedURLException) {
            url
        }
        val (requestUrl, server) = apiController.getFaviconServiceRequest(domain)
        return rememberAsyncImagePainter(
            ImageRequest.Builder(context).apply {
                data(requestUrl)
                addHeader("OCS-APIRequest", "true")
                addHeader("Authorization", Credentials.basic(server.username, server.password))
                crossfade(true)
                val lockDrawable = context.getDrawable(R.drawable.ic_lock)?.apply {
                    setTintList(
                        ColorStateList.valueOf(
                            MaterialTheme.colorScheme.onSurface.toArgb()
                        )
                    )
                }
                placeholder(lockDrawable)
            }.build()
        )
    }

    override fun onCleared() {
        apiController
        super.onCleared()
    }
}