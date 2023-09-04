package com.hegocre.nextcloudpasswords.data.serversettings

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ServerSettings(
    @SerialName(value = "user.password.security.hash")
    val passwordSecurityHash: Int = 40,
    @SerialName(value = "user.encryption.cse")
    val encryptionCse: Int = 0,
    @SerialName(value = "user.session.lifetime")
    val sessionLifetime: Int = 600
) {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun getRequestBody(): String {
            val names = serializer().descriptor.elementNames.toList()
            return Json.encodeToString(names)
        }
    }
}