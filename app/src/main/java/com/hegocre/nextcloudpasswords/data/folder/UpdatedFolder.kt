package com.hegocre.nextcloudpasswords.data.folder

import kotlinx.serialization.Serializable

/**
 * Data class representing an Updated Folder and containing all its required information.
 *
 * @property id The UUID of the folder.
 * @property revision UUID of the current revision.
 * @property label User defined label of the folder.
 * @property cseType Type of the used server side encryption.
 * @property cseKey UUID of the key used for client side encryption.
 * @property parent UUID of the current parent of the folder.
 * @property edited Unix timestamp when the user last changed the folder.
 * @property hidden Hides the folder in list / find actions.
 * @property favorite True if the user has marked the folder as favorite.
 */
@Serializable
data class UpdatedFolder(
    val id: String,
    val revision: String,
    val label: String,
    val cseType: String,
    val cseKey: String,
    val parent: String,
    val edited: Int,
    val hidden: Boolean,
    val favorite: Boolean,
)