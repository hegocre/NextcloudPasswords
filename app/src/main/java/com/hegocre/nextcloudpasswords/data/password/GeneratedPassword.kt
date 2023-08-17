package com.hegocre.nextcloudpasswords.data.password

import kotlinx.serialization.Serializable

/**
 * Data class representing a random generated password by the
 * [Service API](https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api/Service-Api#the-password-endpoint).
 *
 * @property password The generated password.
 * @property words The words used in the password.
 * @property strength The strength setting used.
 * @property numbers Whether or not numbers were used in the password.
 * @property special Whether or not special characters were used in the password.
 */
@Serializable
data class GeneratedPassword(
    val password: String,
    val words: List<String>,
    val strength: Int,
    val numbers: Boolean,
    val special: Boolean
)
