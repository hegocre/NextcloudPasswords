package com.hegocre.nextcloudpasswords.api

import android.util.Log
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.utils.Error
import com.hegocre.nextcloudpasswords.utils.OkHttpRequest
import com.hegocre.nextcloudpasswords.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONObject
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
     * @param details Detail level of the response. Defaults to DETAIL_LEVEL_MODEL.
     * @return A result with a list of passwords if success, or with an error code otherwise
     */
    suspend fun list(
        sessionCode: String? = null,
        details: String = DETAIL_LEVEL_MODEL
    ): Result<List<Password>> {
        val jsonDetails = JSONObject()
            .put("details", details)
            .toString()

        return try {
            val apiResponse = try {
                withContext(Dispatchers.IO) {
                    OkHttpRequest.getInstance().post(
                        sUrl = server.url + LIST_URL,
                        sessionCode = sessionCode,
                        body = jsonDetails,
                        mediaType = OkHttpRequest.JSON,
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

            if (code != 200 || body == null) {
                Log.d("PASSWORDS API", "Code response $code")
                return Result.Error(Error.API_BAD_RESPONSE)
            }

            withContext(Dispatchers.Default) {
                Result.Success(Json.decodeFromString(body))
            }
        } catch (e: SocketTimeoutException) {
            Result.Error(Error.API_TIMEOUT)
        }
    }

    companion object {
        const val DETAIL_LEVEL_MODEL = "model"
        private const val LIST_URL = "/index.php/apps/passwords/api/1.0/password/list"

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