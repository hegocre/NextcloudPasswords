package com.hegocre.nextcloudpasswords.data.password

import kotlinx.serialization.Serializable

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
