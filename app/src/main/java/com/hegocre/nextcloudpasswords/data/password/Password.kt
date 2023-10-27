package com.hegocre.nextcloudpasswords.data.password

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hegocre.nextcloudpasswords.api.encryption.CSEv1Keychain
import com.hegocre.nextcloudpasswords.data.favicon.FaviconController
import com.hegocre.nextcloudpasswords.utils.decryptValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Data class representing a
 * [Password Object](https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api/Password-Api#the-password-object)
 * and containing all its information.
 *
 * @property id The UUID of the password.
 * @property label User defined label of the password.
 * @property username Username associated with the password.
 * @property password The actual password.
 * @property url Url of the website.
 * @property notes Notes for the password. Can be formatted with Markdown.
 * @property customFields Custom fields created by the user. (See
 * [custom fields](https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api/Password-Api#custom-fields)).
 * @property status Security status level of the password. (See
 * [Security Status](https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api/Password-Api#security-status)).
 * @property statusCode Specific code for the current security status. (See
 * [Security Status](https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api/Password-Api#security-status)).
 * @property hash SHA1 hash of the password.
 * @property folder UUID of the current folder of the password.
 * @property revision UUID of the current revision.
 * @property share UUID of the share if the password was shared by someone else with the user.
 * @property shared True if the password is shared with other users.
 * @property cseType Type of the used server side encryption.
 * @property cseKey UUID of the key used for client side encryption.
 * @property sseType Type of the used server side encryption.
 * @property client Name of the client which created this revision.
 * @property hidden Hides the password in list / find actions.
 * @property trashed True if the password is in the trash.
 * @property favorite True if the user has marked the password as favorite.
 * @property editable Specifies if the encrypted properties can be changed. Might be false for shared passwords.
 * @property edited Unix timestamp when the user last changed the password.
 * @property created Unix timestamp when the password was created.
 * @property updated Unix timestamp when the password was updated.
 */
@Serializable
@Entity(tableName = "passwords", indices = [Index(value = ["id"], unique = true)])
data class Password(
    @PrimaryKey
    val id: String,
    val label: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val customFields: String,
    val status: Int,
    val statusCode: String,
    val hash: String,
    val folder: String,
    val revision: String,
    val share: String?,
    val shared: Boolean,
    val cseType: String,
    val cseKey: String,
    val sseType: String,
    val client: String,
    val hidden: Boolean,
    val trashed: Boolean,
    val favorite: Boolean,
    val editable: Boolean,
    val edited: Int,
    val created: Int,
    val updated: Int
) {
    @Ignore
    private val _faviconBitmap = MutableStateFlow<ImageBitmap?>(null)
    val faviconBitmap: StateFlow<ImageBitmap?>
        get() = _faviconBitmap.asStateFlow()

    @Ignore
    private var _isFaviconLoading = false
    val isFaviconLoading: Boolean
        get() = _isFaviconLoading

    /**
     * Trigger a request to load the site's favicon. The result is emitted into [_faviconBitmap] and
     * observed as needed.
     *
     * @param context Context of the application.
     */
    suspend fun loadFavicon(context: Context) {
        _isFaviconLoading = true
        val faviconBitmap = FaviconController.getInstance(context).getOnlineFavicon(url)
        if (faviconBitmap != null) {
            val bitmap = BitmapFactory.decodeByteArray(faviconBitmap, 0, faviconBitmap.size)
            _faviconBitmap.emit(bitmap.asImageBitmap())
        }
        _isFaviconLoading = false
    }

    private suspend fun loadCachedFavicon(context: Context) {
        _isFaviconLoading = true
        val faviconBitmap = FaviconController.getInstance(context).getCachedFavicon(url)
        if (faviconBitmap != null) {
            val bitmap = BitmapFactory.decodeByteArray(faviconBitmap, 0, faviconBitmap.size)
            _faviconBitmap.emit(bitmap.asImageBitmap())
        }
        _isFaviconLoading = false
    }

    /**
     * Returns a copy of this object with the encrypted fields decrypted using the keychain.
     *
     * @param csEv1Keychain The keychain used to decrypt the values.
     * @return The object with the decrypted values.
     */
    suspend fun decrypt(context: Context, csEv1Keychain: CSEv1Keychain? = null): Password? {
        //Not encrypted
        if (cseType == "none") return this

        //Encrypted but no keychain provided
        if (csEv1Keychain == null) return null

        //We don't have they key to decrypt
        if (!csEv1Keychain.keys.containsKey(cseKey)) return null

        //We can decrypt
        val decryptedPassword = withContext(Dispatchers.IO) {
            val url = url.decryptValue(cseKey, csEv1Keychain)
            val label = label.decryptValue(cseKey, csEv1Keychain)
            val password = password.decryptValue(cseKey, csEv1Keychain)
            val username = username.decryptValue(cseKey, csEv1Keychain)
            val notes = notes.decryptValue(cseKey, csEv1Keychain)
            val customFields = customFields.decryptValue(cseKey, csEv1Keychain)

            copy(
                label = label,
                password = password,
                username = username,
                url = url,
                notes = notes,
                customFields = customFields
            )
        }

        return decryptedPassword.apply {
            CoroutineScope(Dispatchers.IO + Job()).launch {
                loadCachedFavicon(context)
            }
        }


    }

    fun matches(query: String): Boolean {
        if (label.lowercase().contains(query.lowercase())) {
            return true
        }
        if (url.lowercase().contains(query.lowercase())) {
            return true
        }

        val queryDomain = Uri.parse(query).host?.removePrefix("www.")
            ?: Uri.parse("https://$query").host?.removePrefix("www.") ?: return false

        return url.lowercase().contains(queryDomain.lowercase())
    }
}
