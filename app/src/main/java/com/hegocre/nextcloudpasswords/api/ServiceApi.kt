package com.hegocre.nextcloudpasswords.api

import android.util.Log
import com.hegocre.nextcloudpasswords.BuildConfig
import com.hegocre.nextcloudpasswords.data.password.GeneratedPassword
import com.hegocre.nextcloudpasswords.utils.Error
import com.hegocre.nextcloudpasswords.utils.OkHttpRequest
import com.hegocre.nextcloudpasswords.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.SocketTimeoutException
import java.net.URLEncoder
import javax.net.ssl.SSLHandshakeException

/**
 * Class with methods used to interact with the
 * [Service API](https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api/Service-Api).
 * This is a Singleton class and will have only one instance.
 *
 * @param server The [Server] where the requests will be made.
 */
class ServiceApi private constructor(private val server: Server) {

    /**
     * Sends a request to the api to obtain a generated password using user settings.
     *
     * @return A result with the password as aString if success, and an error code otherwise.
     */
    suspend fun password(sessionCode: String?): Result<String> {
        return try {
            val apiResponse = withContext(Dispatchers.IO) {
                OkHttpRequest.getInstance().get(
                    sUrl = server.url + PASSWORD_URL,
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

            if (code != 200 || body == null) {
                Log.d("SERVICE API", "Code response $code")
                return Result.Error(Error.API_BAD_RESPONSE)
            }

            withContext(Dispatchers.Default) {
                Result.Success(Json.decodeFromString<GeneratedPassword>(body).password)
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

    fun getFaviconUrl(url: String): String =
        server.url + String.format(FAVICON_URL, URLEncoder.encode(url, "utf-8"), 256)

    companion object {
        private const val FAVICON_URL = "/index.php/apps/passwords/api/1.0/service/favicon/%s/%d"
        private const val PASSWORD_URL = "/index.php/apps/passwords/api/1.0/service/password"

        private var instance: ServiceApi? = null

        /**
         * Get the instance of the [ServiceApi], and create it if null.
         *
         * @param server The [Server] where the requests will be made.
         * @return The instance of the api.
         */
        fun getInstance(server: Server): ServiceApi {
            synchronized(this) {
                var tempInstance = instance

                if (tempInstance == null) {
                    tempInstance = ServiceApi(server)
                    instance = tempInstance
                }

                return tempInstance
            }
        }
    }
}