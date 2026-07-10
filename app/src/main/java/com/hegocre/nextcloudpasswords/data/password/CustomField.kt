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
    /**
     * Whether this field holds a secret value. Secret fields are masked in the UI and
     * should be marked as sensitive when copied, so the system keeps them out of the
     * clipboard preview and history (Android 13+), just like the account password.
     */
    val isSensitive: Boolean
        get() = type == TYPE_SECRET

    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_SECRET = "secret"
        const val TYPE_EMAIL = "email"
        const val TYPE_URL = "url"
        const val TYPE_FILE = "file"
        const val TYPE_DATA = "data"
    }
}
