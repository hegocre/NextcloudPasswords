package com.hegocre.nextcloudpasswords.data.password

import kotlinx.serialization.Serializable

@Serializable
data class DeletedPassword(
    val id: String,
    val revision: String
)
