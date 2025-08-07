package com.hegocre.nextcloudpasswords.api

import com.hegocre.nextcloudpasswords.BuildConfig
import com.hegocre.nextcloudpasswords.data.password.DeletedPassword
import com.hegocre.nextcloudpasswords.data.password.NewPassword
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.data.password.UpdatedPassword
import com.hegocre.nextcloudpasswords.utils.Error
import com.hegocre.nextcloudpasswords.utils.OkHttpRequest
import com.hegocre.nextcloudpasswords.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

/**
 * Class with methods used to interact with the
 * [Password API](https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api/Password-Api).
 * This is a Singleton class and will have only one instance.
 *
 * @param server The [Server] where the requests will be made.
 */
class PasswordsApi private constructor(private var server: Server) {

    /**
     * Sends a request to the api to list all the user passwords. If the user uses CSE, a
     * session code needs to be provided in order for the request to succeed.
     *
     * @param sessionCode Code of the current session, only needed if CSE enabled.
     * @return A result with a list of passwords if success, or with an error code otherwise
     */
    suspend fun list(
        sessionCode: String? = null,
    ): Result<List<Password>> {
        return try {
            val apiResponse = withContext(Dispatchers.IO) {
                OkHttpRequest.getInstance().get(
                    sUrl = server.url + LIST_URL,
                    sessionCode = sessionCode,
                    username = server.username,
                    password = server.password
                )
            }

            val code = apiResponse.code
            val body = withContext(Dispatchers.IO) { apiResponse.body.string() }
            withContext(Dispatchers.IO) {
                apiResponse.close()
            }

            if (code != 200) {
                return Result.Error(Error.API_BAD_RESPONSE)
            }

            withContext(Dispatchers.Default) {
                Result.Success(Json.decodeFromString(body))
            }
        } catch (e: SocketTimeoutException) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            Result.Error(Error.API_TIMEOUT)
        } catch (e: SSLHandshakeException) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            Result.Error(Error.SSL_HANDSHAKE_EXCEPTION)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            Result.Error(Error.UNKNOWN)
        }
    }

    /**
     * Sends a request to the api to create a new password. If the user uses CSE, the
     * password needs to be encrypted.
     *
     * @param newPassword The [NewPassword] object of the created password.
     * @param sessionCode Code of the current session, only needed if CSE enabled.
     * @return A result if success, or an error code otherwise
     */
    suspend fun create(
        newPassword: NewPassword,
        sessionCode: String? = null
    ): Result<Unit> {
        return try {
            val apiResponse = withContext(Dispatchers.IO) {
                OkHttpRequest.getInstance().post(
                    sUrl = server.url + CREATE_URL,
                    sessionCode = sessionCode,
                    body = Json.encodeToString(newPassword),
                    mediaType = OkHttpRequest.JSON,
                    username = server.username,
                    password = server.password
                )
            }

            val code = apiResponse.code
            withContext(Dispatchers.IO) {
                apiResponse.close()
            }

            if (code != 201) {
                return Result.Error(Error.API_BAD_RESPONSE)
            }

            Result.Success(Unit)
        } catch (e: SocketTimeoutException) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            Result.Error(Error.API_TIMEOUT)
        } catch (e: SSLHandshakeException) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            Result.Error(Error.SSL_HANDSHAKE_EXCEPTION)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            Result.Error(Error.UNKNOWN)
        }
    }

    /**
     * Sends a request to the api to update a password. If the user uses CSE, the
     * password needs to be encrypted.
     *
     * @param updatedPassword The [UpdatedPassword] object of the edited password.
     * @param sessionCode Code of the current session, only needed if CSE enabled.
     * @return A result if success, or an error code otherwise
     */
    suspend fun update(
        updatedPassword: UpdatedPassword,
        sessionCode: String? = null
    ): Result<Unit> {
        return try {
            val apiResponse = withContext(Dispatchers.IO) {
                OkHttpRequest.getInstance().patch(
                    sUrl = server.url + UPDATE_URL,
                    sessionCode = sessionCode,
                    body = Json.encodeToString(updatedPassword),
                    mediaType = OkHttpRequest.JSON,
                    username = server.username,
                    password = server.password
                )
            }

            val code = apiResponse.code
            withContext(Dispatchers.IO) {
                apiResponse.close()
            }

            if (code != 200) {
                return Result.Error(Error.API_BAD_RESPONSE)
            }

            Result.Success(Unit)
        } catch (e: SocketTimeoutException) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            Result.Error(Error.API_TIMEOUT)
        } catch (e: SSLHandshakeException) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            Result.Error(Error.SSL_HANDSHAKE_EXCEPTION)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            Result.Error(Error.UNKNOWN)
        }
    }

    /**
     * Sends a request to the api to delete a password.
     *
     * @param deletedPassword The [DeletedPassword] object of the password to be deleted.
     * @param sessionCode Code of the current session, only needed if CSE enabled.
     * @return A result if success, or an error code otherwise
     */
    suspend fun delete(
        deletedPassword: DeletedPassword,
        sessionCode: String? = null
    ): Result<Unit> {
        return try {
            val apiResponse = withContext(Dispatchers.IO) {
                OkHttpRequest.getInstance().delete(
                    sUrl = server.url + DELETE_URL,
                    sessionCode = sessionCode,
                    body = Json.encodeToString(deletedPassword),
                    mediaType = OkHttpRequest.JSON,
                    username = server.username,
                    password = server.password
                )
            }

            val code = apiResponse.code
            withContext(Dispatchers.IO) {
                apiResponse.close()
            }

            if (code != 200) {
                return Result.Error(Error.API_BAD_RESPONSE)
            }

            Result.Success(Unit)
        } catch (e: SocketTimeoutException) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            Result.Error(Error.API_TIMEOUT)
        } catch (e: SSLHandshakeException) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            Result.Error(Error.SSL_HANDSHAKE_EXCEPTION)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            Result.Error(Error.UNKNOWN)
        }
    }

    companion object {
        private const val LIST_URL = "/index.php/apps/passwords/api/1.0/password/list"
        private const val CREATE_URL = "/index.php/apps/passwords/api/1.0/password/create"
        private const val UPDATE_URL = "/index.php/apps/passwords/api/1.0/password/update"
        private const val DELETE_URL = "/index.php/apps/passwords/api/1.0/password/delete"

        private var instance: PasswordsApi? = null

        /**
         * Get the instance of the [PasswordsApi], and create it if null.
         *
         * @param server The [Server] where the requests will be made.
         * @return The instance of the api.
         */
        fun getInstance(server: Server): PasswordsApi {
            synchronized(this) {
                var tempInstance = instance

                if (tempInstance == null) {
                    tempInstance = PasswordsApi(server)
                    instance = tempInstance
                }

                return tempInstance
            }
        }
    }
}