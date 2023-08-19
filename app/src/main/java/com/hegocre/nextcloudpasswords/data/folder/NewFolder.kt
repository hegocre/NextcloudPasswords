package com.hegocre.nextcloudpasswords.data.folder

import kotlinx.serialization.Serializable

/**
 * Data class representing a New Folder and containing all its required information.
 *
 * @property label User defined label of the folder.
 * @property cseType Type of the used server side encryption.
 * @property cseKey UUID of the key used for client side encryption.
 * @property parent UUID of the current parent of the folder.
 * @property edited Unix timestamp when the user last changed the folder.
 * @property hidden Hides the folder in list / find actions.
 * @property favorite True if the user has marked the folder as favorite.
 */
@Serializable
data class NewFolder(
    val label: String,
    val parent: String,
    val cseType: String,
    val cseKey: String,
    val edited: Int,
    val hidden: Boolean,
    val favorite: Boolean,
)