package com.hegocre.nextcloudpasswords.api

import com.hegocre.nextcloudpasswords.utils.Error
import com.hegocre.nextcloudpasswords.utils.OkHttpRequest
import com.hegocre.nextcloudpasswords.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import javax.net.ssl.SSLHandshakeException

/**
 * Class with methods used to interact with the
 * [Service API](https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api/Service-Api).
 * This is a Singleton class and will have only one instance.
 *
 * @param server The [Server] where the requests will be made.
 */
@Suppress("BlockingMethodInNonBlockingContext")
class ServiceApi private constructor(private val server: Server) {

    /**
     * Sends a request to the api to obtain a favicon. No session is required to send this request.
     *
     * @param url The url of the requested site favicon.
     * @return A result with the Bitmap encoded as a [ByteArray] if success, and an error code otherwise.
     */
    suspend fun favicon(url: String): Result<ByteArray> {
        val domain = try {
            URL(url).host
        } catch (e: MalformedURLException) {
            url
        }

        return try {
            val apiResponse = try {
                withContext(Dispatchers.IO) {
                    OkHttpRequest.getInstance().get(
                        sUrl = server.url + String.format(FAVICON_URL, domain, 256),
                        username = server.username,
                        password = server.password
                    )
                }
            } catch (ex: SSLHandshakeException) {
                return Result.Error(Error.SSL_HANDSHAKE_EXCEPTION)
            }

            val code = apiResponse.code
            val body = apiResponse.body?.bytes()

            withContext(Dispatchers.IO) {
                apiResponse.close()
            }

            if (code == 200 && body != null) {
                Result.Success(body)
            } else {
                Result.Error(Error.API_BAD_RESPONSE)
            }

        } catch (e: SocketTimeoutException) {
            Result.Error(Error.API_TIMEOUT)
        }

    }

    companion object {
        private const val FAVICON_URL = "/index.php/apps/passwords/api/1.0/service/favicon/%s/%d"

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