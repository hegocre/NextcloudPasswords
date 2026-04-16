package com.hegocre.nextcloudpasswords.api

import kotlinx.serialization.Serializable

/**
 * A data class representing an authenticated server where requests can be made. The credentials
 * can be obtained using the
 * [Nextcloud login flow](https://docs.nextcloud.com/server/latest/developer_manual/client_apis/LoginFlow/index.html).
 *
 * @property url The url of the server, without a trailing `/`.
 * @property username The username used to authenticate on the server.
 * @property password The password used to authenticate on the server. This is usually an app password.
 */
@Serializable
data class Server(
    val url: String,
    val username: String,
    val password: String
) {
    private var loggedIn: Boolean = false

    fun isLoggedIn(): Boolean {
        return loggedIn
    }

    fun logIn() {
        this.loggedIn = true
    }

    fun logOut() {
        this.loggedIn = false
    }
}
