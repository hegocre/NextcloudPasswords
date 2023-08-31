package com.hegocre.nextcloudpasswords.api

import com.hegocre.nextcloudpasswords.data.serversettings.ServerSettings
import com.hegocre.nextcloudpasswords.utils.Error
import com.hegocre.nextcloudpasswords.utils.OkHttpRequest
import com.hegocre.nextcloudpasswords.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

class SettingsApi private constructor(private val server: Server) {

    /**
     * Sends a request to the api to obtain required user settings. No session is required to send this request.
     *
     * @return A result with the [ServerSettings] object if success, and an error code otherwise.
     */
    suspend fun get(): Result<ServerSettings> {
        return try {
            val apiResponse = withContext(Dispatchers.IO) {
                OkHttpRequest.getInstance().post(
                    sUrl = server.url + GET_URL,
                    body = ServerSettings.getRequestBody(),
                    mediaType = OkHttpRequest.JSON,
                    username = server.username,
                    password = server.password,
                )
            }

            val code = apiResponse.code
            val body = apiResponse.body?.string()

            withContext(Dispatchers.IO) {
                apiResponse.close()
            }

            if (code == 200 && body != null) {
                Result.Success(Json.decodeFromString(body))
            } else {
                Result.Error(Error.API_BAD_RESPONSE)
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
        private const val GET_URL = "/index.php/apps/passwords/api/1.0/settings/get"

        private var instance: SettingsApi? = null

        /**
         * Get the instance of the [ServiceApi], and create it if null.
         *
         * @param server The [Server] where the requests will be made.
         * @return The instance of the api.
         */
        fun getInstance(server: Server): SettingsApi {
            synchronized(this) {
                var tempInstance = instance

                if (tempInstance == null) {
                    tempInstance = SettingsApi(server)
                    instance = tempInstance
                }

                return tempInstance
            }
        }
    }
}