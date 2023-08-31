package com.hegocre.nextcloudpasswords.api

import com.hegocre.nextcloudpasswords.data.folder.DeletedFolder
import com.hegocre.nextcloudpasswords.data.folder.Folder
import com.hegocre.nextcloudpasswords.data.folder.NewFolder
import com.hegocre.nextcloudpasswords.data.folder.UpdatedFolder
import com.hegocre.nextcloudpasswords.utils.Error
import com.hegocre.nextcloudpasswords.utils.OkHttpRequest
import com.hegocre.nextcloudpasswords.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

/**
 * Class with methods used to interact with the
 * [Folder API](https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api/Folder-Api).
 * This is a Singleton class and will have only one instance.
 *
 * @property server The [Server] where the requests will be made.
 */
class FoldersApi private constructor(private var server: Server) {

    /**
     * Sends a request to the api to list all the user passwords. If the user uses CSE, a
     * session code needs to be provided in order for the request to succeed.
     *
     * @param sessionCode Code of the current session, only needed if CSE enabled.
     * @return A result with a list of folders if success, or with an error code otherwise
     */
    suspend fun list(
        sessionCode: String? = null
    ): Result<List<Folder>> {
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
            val body = apiResponse.body?.string()
            withContext(Dispatchers.IO) {
                apiResponse.close()
            }

            if (code != 200 || body == null) return Result.Error(Error.API_BAD_RESPONSE)

            withContext(Dispatchers.Default) {
                Result.Success(Json.decodeFromString(body))
            }
        } catch (e: SocketTimeoutException) {
            Result.Error(Error.API_TIMEOUT)
        } catch (ex: SSLHandshakeException) {
            Result.Error(Error.SSL_HANDSHAKE_EXCEPTION)
        } catch (ex: Exception) {
            Result.Error(Error.UNKNOWN)
        }
    }

    /**
     * Sends a request to the api to create a new folder. If the user uses CSE, the
     * folder needs to be encrypted.
     *
     * @param newFolder The [NewFolder] object of the created folder.
     * @param sessionCode Code of the current session, only needed if CSE enabled.
     * @return A result if success, or an error code otherwise
     */
    suspend fun create(
        newFolder: NewFolder,
        sessionCode: String? = null
    ): Result<Unit> {
        return try {
            val apiResponse = withContext(Dispatchers.IO) {
                OkHttpRequest.getInstance().post(
                    sUrl = server.url + CREATE_URL,
                    sessionCode = sessionCode,
                    body = Json.encodeToString(newFolder),
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

            withContext(Dispatchers.Default) {
                Result.Success(Unit)
            }
        } catch (e: SocketTimeoutException) {
            Result.Error(Error.API_TIMEOUT)
        } catch (ex: SSLHandshakeException) {
            Result.Error(Error.SSL_HANDSHAKE_EXCEPTION)
        } catch (ex: Exception) {
            Result.Error(Error.UNKNOWN)
        }
    }

    /**
     * Sends a request to the api to update a folder. If the user uses CSE, the
     * folder needs to be encrypted.
     *
     * @param updatedFolder The [UpdatedFolder] object of the edited folder.
     * @param sessionCode Code of the current session, only needed if CSE enabled.
     * @return A result if success, or an error code otherwise
     */
    suspend fun update(
        updatedFolder: UpdatedFolder,
        sessionCode: String? = null
    ): Result<Unit> {
        return try {
            val apiResponse = withContext(Dispatchers.IO) {
                OkHttpRequest.getInstance().patch(
                    sUrl = server.url + UPDATE_URL,
                    sessionCode = sessionCode,
                    body = Json.encodeToString(updatedFolder),
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

            withContext(Dispatchers.Default) {
                Result.Success(Unit)
            }
        } catch (e: SocketTimeoutException) {
            Result.Error(Error.API_TIMEOUT)
        } catch (ex: SSLHandshakeException) {
            Result.Error(Error.SSL_HANDSHAKE_EXCEPTION)
        } catch (ex: Exception) {
            Result.Error(Error.UNKNOWN)
        }
    }

    /**
     * Sends a request to the api to delete a folder.
     *
     * @param deletedFolder The [DeletedFolder] object of the folder to be deleted.
     * @param sessionCode Code of the current session, only needed if CSE enabled.
     * @return A result if success, or an error code otherwise
     */
    suspend fun delete(
        deletedFolder: DeletedFolder,
        sessionCode: String? = null
    ): Result<Unit> {
        return try {
            val apiResponse = withContext(Dispatchers.IO) {
                OkHttpRequest.getInstance().delete(
                    sUrl = server.url + DELETE_URL,
                    sessionCode = sessionCode,
                    body = Json.encodeToString(deletedFolder),
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

            withContext(Dispatchers.Default) {
                Result.Success(Unit)
            }
        } catch (e: SocketTimeoutException) {
            Result.Error(Error.API_TIMEOUT)
        } catch (ex: SSLHandshakeException) {
            Result.Error(Error.SSL_HANDSHAKE_EXCEPTION)
        } catch (ex: Exception) {
            Result.Error(Error.UNKNOWN)
        }
    }

    companion object {
        private const val LIST_URL = "/index.php/apps/passwords/api/1.0/folder/list"
        private const val CREATE_URL = "/index.php/apps/passwords/api/1.0/folder/create"
        private const val UPDATE_URL = "/index.php/apps/passwords/api/1.0/folder/update"
        private const val DELETE_URL = "/index.php/apps/passwords/api/1.0/folder/delete"
        const val DEFAULT_FOLDER_UUID = "00000000-0000-0000-0000-000000000000"

        private var instance: FoldersApi? = null

        /**
         * Get the instance of the [FoldersApi], and create it if null.
         *
         * @param server The [Server] where the requests will be made.
         * @return The instance of the api.
         */
        fun getInstance(server: Server): FoldersApi {
            synchronized(this) {
                var tempInstance = instance

                if (tempInstance == null) {
                    tempInstance = FoldersApi(server)
                    instance = tempInstance
                }

                return tempInstance
            }
        }
    }
}