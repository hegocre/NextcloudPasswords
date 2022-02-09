package com.hegocre.nextcloudpasswords.api

/**
 * A data class representing an authenticated server where requests can be made. The credentials
 * can be obtained using the
 * [Nextcloud login flow](https://docs.nextcloud.com/server/latest/developer_manual/client_apis/LoginFlow/index.html).
 *
 * @property url The url of the server, without a trailing `/`.
 * @property username The username used to authenticate on the server.
 * @property password The password used to authenticate on the server. This is usually an app password.
 */
data class Server(
    val url: String,
    val username: String,
    val password: String
)
