package com.hegocre.nextcloudpasswords.data.user

import android.content.Context
import com.hegocre.nextcloudpasswords.api.Server
import com.hegocre.nextcloudpasswords.databases.AppDatabase
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import dev.spght.encryptedprefs.EncryptedSharedPreferences
import dev.spght.encryptedprefs.MasterKey
import dev.spght.encryptedprefs.MasterKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import androidx.core.content.edit

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

    private val servers = mutableSetOf<Server>()

    fun init() {
        val propertyVal = _preferencesManager.getServers()
        if (propertyVal != null) {
            this.servers.addAll(unmarshal(propertyVal))
        }
    }

    val isLoggedIn: Boolean
        get() = getServers().find { it.isLoggedIn() } != null

    /**
     * Method to store the server URl and credentials on the storage.
     *
     * @param server The URL of the server to log in
     * @param username The username used to authenticate on the server.
     * @param password The password used to authenticate on the server.
     */
    suspend fun logIn(server: String, username: String, password: String) {
        try {
            val currentServer = getServer()
            currentServer.logOut()
        } catch(e: UserException) {
            // user not logged in
        }
        val newServer = Server(server, username, password)
        this.servers.add(newServer)
        setActiveServer(newServer)
    }

    private fun updateServers(servers: Set<Server>) {
        _preferencesManager.setServers(marshal(servers))
    }

    fun getServers() : Set<Server> {
        return this.servers;
    }

    fun removeServer(server: Server) {
        this.servers.remove(server)
        updateServers(this.servers)
    }

    suspend fun clearAllDB() {
        withContext(Dispatchers.IO) {
            passwordDatabase.passwordDao.deleteDatabase()
            folderDatabase.folderDao.deleteDatabase()
        }
    }

    /**
     * Method to delete the server credentials from the storage, as well as clearing the saved master
     * password and keychain if present.
     *
     */
    suspend fun logOut() {
        clearAllDB()
        _preferencesManager.clear()
        this.servers.clear()
    }

    /**
     * Returns the current logged in server stored on the device.
     *
     * @return The [Server] with the current URL and credentials.
     * @throws UserException If there are no credentials stored.
     */
    @Throws(UserException::class)
    fun getServer(): Server {
        val loggedInServer = servers.find { it.isLoggedIn() }
        // Check if a logged-in server was found
        if (loggedInServer != null) {
            return loggedInServer
        } else {
            // If no server has loggedIn = true, or if the servers list itself might be empty
            // and you consider that an exceptional case for this method.
            throw UserException("No logged-in server found.")
        }
    }

    suspend fun setActiveServer(serverToActivate: Server) {
        // Deactivate all other servers
        servers.forEach {
            if (it != serverToActivate) {
                it.logOut()
            }
        }
        clearAllDB()
        // Activate the selected server
        serverToActivate.logIn()
        updateServers(servers)
    }

    fun marshal(configs: Set<Server>): String {
        return Json.encodeToString(configs)
    }

    fun unmarshal(propertyVal: String): Set<Server>  {
        return Json.decodeFromString(propertyVal)
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
                    tempInstance.init()
                    instance = tempInstance
                }

                return tempInstance
            }
        }
    }
}
