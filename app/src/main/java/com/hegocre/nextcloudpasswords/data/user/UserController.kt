package com.hegocre.nextcloudpasswords.data.user

import android.content.Context
import com.hegocre.nextcloudpasswords.api.Server
import com.hegocre.nextcloudpasswords.databases.AppDatabase
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Class used to manage to log in and log out, as well as providing the current server to the API
 * Controller. This is a Singleton class and will have only one instance.
 *
 * @param context Context of the application.
 */
class UserController private constructor(context: Context) {
    private val _preferencesManager = PreferencesManager.getInstance(context)
    private val passwordDatabase = AppDatabase.getInstance(context)
    private val folderDatabase = AppDatabase.getInstance(context)

    val isLoggedIn: Boolean
        get() = _preferencesManager.getLoggedInServer() != null

    /**
     * Method to store the server URl and credentials on the storage.
     *
     * @param server The URL of the server to log in
     * @param username The username used to authenticate on the server.
     * @param password The password used to authenticate on the server.
     */
    fun logIn(server: String, username: String, password: String) {
        with(_preferencesManager) {
            setLoggedInServer(server)
            setLoggedInUser(username)
            setLoggedInPassword(password)
        }
    }

    /**
     * Method to delete the server credentials from the storage, as well as clearing the saved master
     * password and keychain if present.
     *
     */
    suspend fun logOut() {
        withContext(Dispatchers.IO) {
            passwordDatabase.passwordDao.deleteDatabase()
            folderDatabase.folderDao.deleteDatabase()
        }
        with(_preferencesManager) {
            setLoggedInServer(null)
            setLoggedInUser(null)
            setLoggedInPassword(null)
            setMasterPassword(null)
            setCSEv1Keychain(null)
        }
    }

    /**
     * Returns the current logged in server stored on the device.
     *
     * @return The [Server] with the current URL and credentials.
     * @throws UserException If there are no credentials stored.
     */
    @Throws(UserException::class)
    fun getServer(): Server {
        return with(_preferencesManager) {
            val url = getLoggedInServer() ?: throw UserException("Not logged in")
            val username = getLoggedInUser() ?: throw UserException("Not logged in")
            val password = getLoggedInPassword() ?: throw UserException("Not logged in")
            Server(url, username, password)
        }
    }

    companion object {
        private var instance: UserController? = null

        /**
         * Get the instance of the [UserController], and create it if null.
         *
         * @param context Context of the application.
         * @return The instance of the controller.
         */
        fun getInstance(context: Context): UserController {
            synchronized(this) {
                var tempInstance = instance

                if (tempInstance == null) {
                    tempInstance = UserController(context)
                    instance = tempInstance
                }

                return tempInstance
            }
        }
    }
}