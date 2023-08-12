package com.hegocre.nextcloudpasswords.api

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.hegocre.nextcloudpasswords.api.encryption.CSEv1Keychain
import com.hegocre.nextcloudpasswords.api.encryption.exceptions.PWDv1ChallengeMasterKeyInvalidException
import com.hegocre.nextcloudpasswords.api.encryption.exceptions.PWDv1ChallengeMasterKeyNeededException
import com.hegocre.nextcloudpasswords.data.folder.Folder
import com.hegocre.nextcloudpasswords.data.password.DeletedPassword
import com.hegocre.nextcloudpasswords.data.password.NewPassword
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.data.password.UpdatedPassword
import com.hegocre.nextcloudpasswords.data.user.UserController
import com.hegocre.nextcloudpasswords.utils.Error
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import com.hegocre.nextcloudpasswords.utils.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * Class with methods used to interact with [the API](https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api)
 * classes. This is a Singleton class and will have only one instance.
 *
 * @param context Context of the application.
 */
class ApiController private constructor(context: Context) {
    private val server = UserController.getInstance(context).getServer()

    private val preferencesManager = PreferencesManager.getInstance(context)

    private val passwordsApi = PasswordsApi.getInstance(server)
    private val foldersApi = FoldersApi.getInstance(server)
    private val sessionApi = SessionApi.getInstance(server)
    private val serviceApi = ServiceApi.getInstance(server)

    private var sessionCode: String? = null

    var csEv1Keychain: MutableLiveData<CSEv1Keychain?> = MutableLiveData<CSEv1Keychain?>(
        preferencesManager.getCSEv1Keychain()?.let { csEv1KeychainJson ->
            CSEv1Keychain.fromJson(csEv1KeychainJson)
        }
    )
        private set

    private val _sessionOpen = MutableStateFlow(false)
    val sessionOpen: StateFlow<Boolean>
        get() = _sessionOpen.asStateFlow()

    /**
     * Requests and opens a session via the [SessionApi] class.
     *
     * @param masterPassword Master password to request the session, if provided, and not needed
     * if no CSE used.
     * @param saveKeychain A boolean indicating if the decrypted keychain should be saved.
     * @return A boolean indicating if the session was successfully opened.
     * @throws PWDv1ChallengeMasterKeyNeededException If there is no master key provided, but one is
     * needed.
     * @throws PWDv1ChallengeMasterKeyInvalidException If a master key was provided, but is not valid.
     */
    @Throws(
        PWDv1ChallengeMasterKeyNeededException::class,
        PWDv1ChallengeMasterKeyInvalidException::class
    )
    suspend fun openSession(masterPassword: String?, saveKeychain: Boolean = false): Boolean {
        val requestResult = sessionApi.requestSession()

        val secretResult = if (requestResult is Result.Success) {
            requestResult.data.solve(masterPassword)
        } else {
            // Error opening session
            if (requestResult is Result.Error) {
                when (requestResult.code) {
                    Error.API_TIMEOUT -> Log.e(
                        "API Controller",
                        "Timeout requesting session, user ${server.username}"
                    )
                    Error.API_BAD_RESPONSE -> Log.e(
                        "API Controller",
                        "Bad response on session request, user ${server.username}"
                    )
                }
            }
            return false
        }

        val secret = if (secretResult is Result.Success) {
            secretResult.data
        } else {
            return if (secretResult is Result.Error && secretResult.code == Error.API_NO_CSE) {
                // No encryption, we need no session
                _sessionOpen.emit(true)
                true
            } else {
                // Error opening session
                false
            }
        }

        val openedSessionRequest = sessionApi.openSession(secret)

        val openedSession = if (openedSessionRequest is Result.Success) {
            openedSessionRequest.data
        } else {
            if (openedSessionRequest is Result.Error) {
                when (openedSessionRequest.code) {
                    Error.API_TIMEOUT -> Log.e(
                        "API Controller",
                        "Timeout opening session, user ${server.username}"
                    )
                    Error.API_BAD_RESPONSE -> Log.e(
                        "API Controller",
                        "Bad response on session open, user ${server.username}"
                    )
                }
            }
            return false
        }

        openedSession.second.let { encryptedJson ->
            masterPassword?.let { masterPassword ->
                val keysJson = CSEv1Keychain.decryptJson(encryptedJson, masterPassword)
                if (saveKeychain) preferencesManager.setCSEv1Keychain(keysJson)
                csEv1Keychain.postValue(CSEv1Keychain.fromJson(keysJson))
            }
        }
        sessionCode = openedSession.first

        // Send keep alive request on background
        CoroutineScope(coroutineContext).launch {
            sessionCode?.let { sessionApi.keepAlive(it) }
        }

        _sessionOpen.emit(true)
        return true
    }

    /**
     * Closes the current session and deletes the saved keychain from the app storage.
     *
     * @return A boolean indicating if the session was successfully closed.
     */
    suspend fun closeSession(): Boolean {
        return if (sessionCode == null || sessionCode?.let { code -> sessionApi.closeSession(code) } == true) {
            _sessionOpen.emit(false)
            preferencesManager.setCSEv1Keychain(null)
            true
        } else {
            // Session was not closed, some error happened
            false
        }
    }

    /**
     * Gets a list of the user passwords via the [PasswordsApi] class. This can only be called when a
     * session is open, otherwise an error is thrown.
     *
     * @return A result with the list of passwords if success, or an error code otherwise.
     */
    suspend fun listPasswords(): Result<List<Password>> {
        if (!_sessionOpen.value) return Result.Error(Error.API_NO_SESSION)
        return passwordsApi.list(sessionCode)
    }

    /**
     * Gets a list of the user folders via the [FoldersApi] class. This can only be called when a session
     * is open, otherwise an error is thrown.
     *
     * @return A result with the list of folders if success, or an error code otherwise.
     */
    suspend fun listFolders(): Result<List<Folder>> {
        if (!_sessionOpen.value) return Result.Error(Error.API_NO_SESSION)
        return foldersApi.list(sessionCode)
    }

    /**
     * Gets a favicon from a url via the [ServiceApi] class. An open session is not needed for
     * this method to be called.
     *
     * @param url The url of the requested site favicon.
     * @return A [ByteArray] with the encoded bitmap.
     */
    suspend fun getFavicon(url: String): ByteArray? {
        val result = serviceApi.favicon(url)
        return if (result is Result.Success) {
            result.data
        } else {
            if (result is Result.Error)
                Log.d("API Controller", "Error ${result.code} requesting favicon")
            null
        }
    }

    /**
     * Creates a new password via the [PasswordsApi] class. This can only be called when a
     * session is open, otherwise an error is thrown.
     *
     * @param newPassword [NewPassword] object to be created.
     * @return A boolean stating whether the password was successfully created.
     */
    suspend fun createPassword(newPassword: NewPassword): Boolean {
        if (!_sessionOpen.value) return false
        val result = passwordsApi.create(newPassword, sessionCode)
        return result is Result.Success
    }

    /**
     * Updates an existing password via the [PasswordsApi] class. This can only be called when a
     * session is open, otherwise an error is thrown.
     *
     * @param updatedPassword [UpdatedPassword] object to be updated.
     * @return A boolean stating whether the password was successfully updated.
     */
    suspend fun updatePassword(updatedPassword: UpdatedPassword): Boolean {
        if (!_sessionOpen.value) return false
        val result = passwordsApi.update(updatedPassword, sessionCode)
        return result is Result.Success
    }

    /**
     * Deletes an existing password via the [PasswordsApi] class. This can only be called when a
     * session is open, otherwise an error is thrown.
     *
     * @param deletedPassword [DeletedPassword] object to be deleted.
     * @return A boolean stating whether the password was successfully deleted.
     */
    suspend fun deletePassword(deletedPassword: DeletedPassword): Boolean {
        if (!_sessionOpen.value) return false
        val result = passwordsApi.delete(deletedPassword, sessionCode)
        return result is Result.Success
    }

    companion object {
        private var instance: ApiController? = null

        /**
         * Get the instance of the [ApiController], and create it if null.
         *
         * @param context Context of the application.
         * @return The instance of the controller.
         */
        fun getInstance(context: Context): ApiController {
            synchronized(this) {
                var tempInstance = instance

                if (tempInstance == null) {
                    tempInstance = ApiController(context)
                    instance = tempInstance
                }

                return tempInstance
            }
        }
    }

}

