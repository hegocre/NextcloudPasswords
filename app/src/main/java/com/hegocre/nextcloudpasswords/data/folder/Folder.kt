package com.hegocre.nextcloudpasswords.data.folder

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hegocre.nextcloudpasswords.api.FoldersApi
import com.hegocre.nextcloudpasswords.api.encryption.CSEv1Keychain
import com.hegocre.nextcloudpasswords.utils.decryptValue
import org.json.JSONArray
import org.json.JSONException

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
 */
@Entity(tableName = "folders", indices = [Index(value = ["id"], unique = true)])
data class Folder(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "label")
    val label: String,
    @ColumnInfo(name = "parent")
    val parent: String = FoldersApi.DEFAULT_FOLDER_UUID,
    @ColumnInfo(name = "revision")
    val revision: String,
    @ColumnInfo(name = "cseType")
    val cseType: String = "none",
    @ColumnInfo(name = "cseKey")
    val cseKey: String = "",
    @ColumnInfo(name = "sseType")
    val sseType: String = "none"
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

    companion object {
        /**
         * Create a list of folders from a JSON object.
         *
         * @param data The JSON object to parse.
         * @return A list of the parsed folders.
         */
        fun listFromJson(data: String): List<Folder> {
            val folderList = ArrayList<Folder>()

            val array = try {
                JSONArray(data)
            } catch (ex: JSONException) {
                JSONArray()
            }

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                val id = try {
                    obj.getString("id")
                } catch (ex: JSONException) {
                    ""
                }

                val label = try {
                    obj.getString("label")
                } catch (ex: JSONException) {
                    ""
                }

                val parent = try {
                    obj.getString("parent")
                } catch (ex: JSONException) {
                    FoldersApi.DEFAULT_FOLDER_UUID
                }

                val revision = try {
                    obj.getString("revision")
                } catch (ex: JSONException) {
                    ""
                }

                val cseType = try {
                    obj.getString("cseType")
                } catch (ex: JSONException) {
                    "none"
                }

                val cseKey = try {
                    obj.getString("cseKey")
                } catch (ex: JSONException) {
                    ""
                }

                val sseType = try {
                    obj.getString("sseType")
                } catch (ex: JSONException) {
                    "none"
                }

                folderList.add(
                    Folder(
                        id,
                        label,
                        parent,
                        revision,
                        cseType,
                        cseKey,
                        sseType
                    )
                )
            }

            return folderList
        }
    }
}
