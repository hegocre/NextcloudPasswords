package com.hegocre.nextcloudpasswords.data.password

import kotlinx.serialization.Serializable

@Serializable
data class RequestedPassword(
    val strength: Int,
    val numbers: Boolean,
    val special: Boolean
) {
    companion object {
        const val STRENGTH_ULTRA = 4
        const val STRENGTH_HIGH = 3
        const val STRENGTH_MEDIUM = 2
        const val STRENGTH_STANDARD = 1
        const val STRENGTH_LOW = 0
    }
}
