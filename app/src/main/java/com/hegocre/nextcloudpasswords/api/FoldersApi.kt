package com.hegocre.nextcloudpasswords.api

import com.hegocre.nextcloudpasswords.data.folder.Folder
import com.hegocre.nextcloudpasswords.utils.Error
import com.hegocre.nextcloudpasswords.utils.OkHttpRequest
import com.hegocre.nextcloudpasswords.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

/**
 * Class with methods used to interact with the
 * [Folder API](https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api/Folder-Api).
 * This is a Singleton class and will have only one instance.
 *
 * @property server The [Server] where the requests will be made.
 */
@Suppress("BlockingMethodInNonBlockingContext")
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
            val apiResponse = try {
                withContext(Dispatchers.IO) {
                    OkHttpRequest.getInstance().get(
                        sUrl = server.url + LIST_URL,
                        sessionCode = sessionCode,
                        username = server.username,
                        password = server.password
                    )
                }
            } catch (ex: SSLHandshakeException) {
                return Result.Error(Error.SSL_HANDSHAKE_EXCEPTION)
            }

            val code = apiResponse.code
            val body = apiResponse.body?.string()
            withContext(Dispatchers.IO) {
                apiResponse.close()
            }

            if (code != 200 || body == null) return Result.Error(Error.API_BAD_RESPONSE)

            Result.Success(Folder.listFromJson(body))
        } catch (e: SocketTimeoutException) {
            Result.Error(Error.API_TIMEOUT)
        }
    }

    companion object {
        private const val LIST_URL = "/index.php/apps/passwords/api/1.0/folder/list"
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