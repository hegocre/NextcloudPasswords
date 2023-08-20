package com.hegocre.nextcloudpasswords.data.folder

import kotlinx.serialization.Serializable

/**
 * Data class representing a Deleted Folder and containing all its required information.
 *
 * @property id The UUID of the folder.
 * @property revision UUID of the current revision.
 */
@Serializable
data class DeletedFolder(
    val id: String,
    val revision: String
)
