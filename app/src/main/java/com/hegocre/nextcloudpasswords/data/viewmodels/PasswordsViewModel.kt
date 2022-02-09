package com.hegocre.nextcloudpasswords.data.viewmodels

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.*
import com.hegocre.nextcloudpasswords.api.ApiController
import com.hegocre.nextcloudpasswords.api.encryption.CSEv1Keychain
import com.hegocre.nextcloudpasswords.api.encryption.exceptions.PWDv1ChallengeClientDeauthorizedException
import com.hegocre.nextcloudpasswords.api.encryption.exceptions.PWDv1ChallengeMasterKeyInvalidException
import com.hegocre.nextcloudpasswords.api.encryption.exceptions.PWDv1ChallengeMasterKeyNeededException
import com.hegocre.nextcloudpasswords.api.encryption.exceptions.PWDv1ChallengePasswordException
import com.hegocre.nextcloudpasswords.data.folder.Folder
import com.hegocre.nextcloudpasswords.data.folder.FolderController
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.data.password.PasswordController
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PasswordsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager.getInstance(application)

    private var masterPassword: MutableLiveData<String?> = MutableLiveData<String?>(null).also {
        it.value = preferencesManager.getMasterPassword()
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing.asStateFlow()

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

    private val sessionOpen = apiController.sessionOpen
    val csEv1Keychain: LiveData<CSEv1Keychain?>
        get() = apiController.csEv1Keychain

    val passwords: LiveData<List<Password>>
        get() = PasswordController.getInstance(getApplication()).getPasswords()
    val folders: LiveData<List<Folder>>
        get() = FolderController.getInstance(getApplication()).getFolders()

    var visiblePassword = mutableStateOf<Password?>(null)
        private set
    var visibleFolder = mutableStateOf<Folder?>(null)
        private set

    init {
        if (!apiController.sessionOpen.value)
            openSession(masterPassword.value)
    }

    private fun openSession(password: String?, saveKeychain: Boolean = false) {
        viewModelScope.launch {
            _isRefreshing.emit(true)
            try {
                if (apiController.openSession(password, saveKeychain)) {
                    sync()
                    return@launch
                }
            } catch (ex: PWDv1ChallengeMasterKeyNeededException) {
                _needsMasterPassword.emit(true)
            } catch (ex: PWDv1ChallengeClientDeauthorizedException) {
                _clientDeauthorized.postValue(true)
            } catch (ex: Exception) {
                when (ex) {
                    is PWDv1ChallengeMasterKeyInvalidException, is PWDv1ChallengePasswordException -> {
                        _needsMasterPassword.emit(true)
                        _masterPasswordInvalid.emit(true)
                        preferencesManager.setMasterPassword(null)
                    }
                    else -> {
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

    override fun onCleared() {
        apiController
        super.onCleared()
    }
}