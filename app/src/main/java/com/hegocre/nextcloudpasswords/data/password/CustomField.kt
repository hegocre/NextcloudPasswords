package com.hegocre.nextcloudpasswords.data.password

import kotlinx.serialization.Serializable

/**
 * Data class representing a custom field of a [Password].
 *
 * @property label The name of the field.
 * @property type The [field type](https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api/Password-Api#field-types).
 * @property value The value for the field.
 */
@Serializable
data class CustomField(
    val label: String,
    val type: String,
    val value: String
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_SECRET = "secret"
        const val TYPE_EMAIL = "email"
        const val TYPE_URL = "url"
        const val TYPE_FILE = "file"
        const val TYPE_DATA = "data"
    }
}
