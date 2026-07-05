package com.hegocre.nextcloudpasswords.api

import com.hegocre.nextcloudpasswords.BuildConfig
import com.hegocre.nextcloudpasswords.api.encryption.PWDv1Challenge
import com.hegocre.nextcloudpasswords.api.exceptions.ClientDeauthorizedException
import com.hegocre.nextcloudpasswords.api.exceptions.PWDv1ChallengeMasterKeyInvalidException
import com.hegocre.nextcloudpasswords.utils.Error
import com.hegocre.nextcloudpasswords.utils.OkHttpRequest
import com.hegocre.nextcloudpasswords.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

/**
 * Outcome of an initial `session/request` call, classified by HTTP status.
 *
 * A `401` is treated as [RECOVERABLE_AUTH_ERROR] rather than a permanent
 * deauthorization. The server returns `401` for any failed Nextcloud
 * authentication — an expired or server-side-invalidated app token, but equally
 * a `401` injected by something in front of Nextcloud (captive portal, reverse
 * proxy, WAF) on a flaky connection. The body is empty in every case, so a
 * transient `401` cannot be told apart from a genuine revocation, and a password
 * manager must not destroy the locally cached (offline) vault over an ambiguous,
 * possibly transient failure. Core brute-force throttling surfaces as `429`
 * (handled as [BAD_RESPONSE]); only the Passwords app's explicit login-attempt
 * lockout returns `403`, which is treated as [DEAUTHORIZED].
 */
enum class SessionRequestOutcome { SUCCESS, RECOVERABLE_AUTH_ERROR, DEAUTHORIZED, BAD_RESPONSE }

fun classifySessionRequest(code: Int): SessionRequestOutcome = when (code) {
    200 -> SessionRequestOutcome.SUCCESS
    401 -> SessionRequestOutcome.RECOVERABLE_AUTH_ERROR
    403 -> SessionRequestOutcome.DEAUTHORIZED
    else -> SessionRequestOutcome.BAD_RESPONSE
}

/**
 * Class with methods used to interact with the
 * [Session API](https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api/Session-Api).
 * This is a Singleton class and will have only one instance.
 *
 * @param server The [Server] where the requests will be made.
 */
class SessionApi private constructor(private var server: Server) {

    /**
     * Sends a request to the api to open a session. If the user uses client-side encryption,
     * it returns a challenge with 3 salts. If no CSE used, the challenge is empty.
     *
     * @return Result with the [PWDv1Challenge] if success, or with an error code otherwise.
     */
    suspend fun requestSession(): Result<PWDv1Challenge> {
        return try {
            val apiResponse = try {
                withContext(Dispatchers.IO) {
                    OkHttpRequest.getInstance().get(
                        sUrl = server.url + REQUEST_URL,
                        username = server.username,
                        password = server.password
                    )
                }
            } catch (e: SSLHandshakeException) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace()
                }
                return Result.Error(Error.SSL_HANDSHAKE_EXCEPTION)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace()
                }
                return Result.Error(0)
            }

            val code = apiResponse.code
            val body = withContext(Dispatchers.IO) { apiResponse.body.string() }

            withContext(Dispatchers.IO) {
                apiResponse.close()
            }

            when (classifySessionRequest(code)) {
                SessionRequestOutcome.SUCCESS -> Result.Success(PWDv1Challenge.fromJson(body))
                // Keep the user logged in on a transient auth failure: the caller
                // falls back to the cached keychain instead of wiping local data
                SessionRequestOutcome.RECOVERABLE_AUTH_ERROR -> Result.Error(Error.API_AUTH_ERROR)
                SessionRequestOutcome.DEAUTHORIZED -> throw ClientDeauthorizedException()
                SessionRequestOutcome.BAD_RESPONSE -> Result.Error(Error.API_BAD_RESPONSE)
            }

        } catch (e: SocketTimeoutException) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            Result.Error(Error.API_TIMEOUT)
        }
    }

    /**
     * Sends a request to the Session API to open a session. This only needs to be called if the user
     * uses CSE encryption.
     *
     * @param solvedChallenge The solved PWDv1Challenge via [libsodium](https://doc.libsodium.org/)
     * using the master password.
     * @return Result with a pair with the session code and the encrypted keychain JSON if success,
     * or an error code otherwise.
     * @throws PWDv1ChallengeMasterKeyInvalidException If a master key was provided, but is not valid.
     * @throws ClientDeauthorizedException If too many incorrect attempts were made and
     * the client has been deauthorized.
     */
    suspend fun openSession(solvedChallenge: String): Result<Pair<String, String>> {
        val jsonChallenge = JSONObject()
            .put("challenge", solvedChallenge)
            .toString()

        return try {
            val apiResponse = withContext(Dispatchers.IO) {
                OkHttpRequest.getInstance().post(
                    sUrl = server.url + OPEN_URL,
                    body = jsonChallenge,
                    mediaType = OkHttpRequest.JSON,
                    username = server.username,
                    password = server.password
                )
            }

            val body = withContext(Dispatchers.IO) { apiResponse.body.string() }
            val code = apiResponse.code

            val xSessionCode = apiResponse.header("x-api-session", null)

            withContext(Dispatchers.IO) {
                apiResponse.close()
            }

            if (code == 401)
                throw PWDv1ChallengeMasterKeyInvalidException()

            if (code == 403)
                throw ClientDeauthorizedException()

            if (xSessionCode == null || code != 200)
                return Result.Error(Error.API_BAD_RESPONSE)

            Result.Success(Pair(xSessionCode, body))
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
     * Sends a request to the api to keep the session alive. For this to be called, a session
     * needs to be open.
     *
     * @param sessionCode The session code of the current session.
     * @return A boolean indicating if the request was successful.
     */
    suspend fun keepAlive(sessionCode: String): Boolean {
        return try {
            val apiResponse = withContext(Dispatchers.IO) {
                OkHttpRequest.getInstance().get(
                    sUrl = server.url + KEEPALIVE_URL,
                    sessionCode = sessionCode,
                    username = server.username,
                    password = server.password
                )
            }

            val code = apiResponse.code
            withContext(Dispatchers.IO) {
                apiResponse.close()
            }

            code == 200
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            false
        }
    }

    /**
     * Sends a request to the api to close the current session. For this to be called, a session
     * needs to be open.
     *
     * @param sessionCode The session code of the current session.
     * @return A boolean indicating if the request was successful.
     */
    suspend fun closeSession(sessionCode: String): Boolean {
        return try {
            val apiResponse = withContext(Dispatchers.IO) {
                OkHttpRequest.getInstance().get(
                    sUrl = server.url + CLOSE_URL,
                    sessionCode = sessionCode,
                    username = server.username,
                    password = server.password
                )
            }

            val code = apiResponse.code
            withContext(Dispatchers.IO) {
                apiResponse.close()
            }

            code == 200
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            false
        }
    }

    companion object {
        private const val REQUEST_URL = "/index.php/apps/passwords/api/1.0/session/request"
        private const val OPEN_URL = "/index.php/apps/passwords/api/1.0/session/open"
        private const val CLOSE_URL = "/index.php/apps/passwords/api/1.0/session/close"
        private const val KEEPALIVE_URL = "/index.php/apps/passwords/api/1.0/session/keepalive"

        private var instance: SessionApi? = null

        /**
         * Get the instance of the [SessionApi], and create it if null.
         *
         * @param server The [Server] where the requests will be made.
         * @return The instance of the api.
         */
        fun getInstance(server: Server): SessionApi {
            synchronized(this) {
                var tempInstance = instance

                if (tempInstance == null) {
                    tempInstance = SessionApi(server)
                    instance = tempInstance
                }

                return tempInstance
            }
        }
    }
}