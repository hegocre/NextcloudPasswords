package com.hegocre.nextcloudpasswords.data.password

import kotlinx.serialization.Serializable

/**
 * Data class representing a Deleted Password and containing all its required information.
 *
 * @property id The UUID of the password.
 * @property revision UUID of the current revision.
 */
@Serializable
data class DeletedPassword(
    val id: String,
    val revision: String
)
