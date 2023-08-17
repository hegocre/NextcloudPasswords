package com.hegocre.nextcloudpasswords.data.password

import kotlinx.serialization.Serializable

@Serializable
data class GeneratedPassword(
    val password: String,
    val words: List<String>,
    val strength: Int,
    val numbers: Boolean,
    val special: Boolean
)
