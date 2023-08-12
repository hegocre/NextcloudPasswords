package com.hegocre.nextcloudpasswords.data.password

import kotlinx.serialization.Serializable

@Serializable
data class NewPassword(
    val password: String,
    val label: String,
    val username: String,
    val url: String,
    val notes: String,
    val customFields: String,
    val hash: String,
    val cseType: String,
    val cseKey: String,
    val folder: String,
    val edited: Int,
    val hidden: Boolean,
    val favorite: Boolean,
)