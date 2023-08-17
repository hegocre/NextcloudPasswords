package com.hegocre.nextcloudpasswords.data.folder

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hegocre.nextcloudpasswords.api.FoldersApi
import com.hegocre.nextcloudpasswords.api.encryption.CSEv1Keychain
import com.hegocre.nextcloudpasswords.utils.decryptValue
import kotlinx.serialization.Serializable

/**
 * Data class representing a
 * [Folder Object](https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api/Folder-Api#the-folder-object)
 * and containing all its information.
 *
 * @property id The UUID of the folder.
 * @property label User defined label of the folder.
 * @property parent UUID of the parent folder.
 * @property revision UUID of the current revision.
 * @property cseType Type of the used server side encryption.
 * @property cseKey UUID of the key used for client side encryption.
 * @property sseType Type of the used server side encryption.
 * @property client Name of the client which created this revision.
 * @property hidden Hides the folder in list / find actions.
 * @property trashed True if the folder is in the trash.
 * @property favorite True if the user has marked the folder as favorite.
 * @property created Unix timestamp when the folder was created.
 * @property updated Unix timestamp when the folder was updated.
 * @property edited Unix timestamp when the user last changed the folder name.
 */
@Serializable
@Entity(tableName = "folders", indices = [Index(value = ["id"], unique = true)])
data class Folder(
    @PrimaryKey
    val id: String,
    val label: String,
    val parent: String = FoldersApi.DEFAULT_FOLDER_UUID,
    val revision: String,
    val cseType: String,
    val cseKey: String,
    val sseType: String,
    val client: String,
    val hidden: Boolean,
    val trashed: Boolean,
    val favorite: Boolean,
    val created: Int,
    val updated: Int,
    val edited: Int
) {
    /**
     * Returns a copy of this object with the encrypted fields decrypted using the keychain.
     *
     * @param csEv1Keychain The keychain used to decrypt the values.
     * @return The object with the decrypted values.
     */
    fun decrypt(csEv1Keychain: CSEv1Keychain? = null): Folder? {
        //Not encrypted
        if (cseType == "none") return this

        //Encrypted but no keychain provided
        if (csEv1Keychain == null) return null

        //We don't have they key to decrypt
        if (!csEv1Keychain.keys.containsKey(cseKey)) return null

        //We can decrypt
        val label = label.decryptValue(cseKey, csEv1Keychain)

        return copy(
            label = label
        )
    }
}
