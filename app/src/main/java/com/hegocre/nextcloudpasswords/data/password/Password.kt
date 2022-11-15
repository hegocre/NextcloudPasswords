package com.hegocre.nextcloudpasswords.data.password

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.room.*
import com.hegocre.nextcloudpasswords.api.encryption.CSEv1Keychain
import com.hegocre.nextcloudpasswords.data.favicon.FaviconController
import com.hegocre.nextcloudpasswords.utils.decryptValue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONException

/**
 * Data class representing a
 * [Folder Object](https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api/Password-Api#the-password-object)
 * and containing all its information.
 *
 * @property id The UUID of the password.
 * @property label User defined label of the password.
 * @property username Username associated with the password.
 * @property password The actual password.
 * @property url Url of the website.
 * @property revision UUID of the current revision.
 * @property cseType Type of the used server side encryption.
 * @property cseKey UUID of the key used for client side encryption.
 * @property sseType Type of the used server side encryption.
 * @property favorite True if the user has marked the password as favorite.
 * @property folder UUID of the current folder of the password.
 * @property status Security status level of the password. See
 * [Security Status](https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api/Password-Api#security-status).
 */
@Entity(tableName = "passwords", indices = [Index(value = ["id"], unique = true)])
data class Password(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "label")
    val label: String,
    @ColumnInfo(name = "username")
    val username: String,
    @ColumnInfo(name = "password")
    val password: String,
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "revision")
    val revision: String,
    @ColumnInfo(name = "cseType")
    val cseType: String = "none",
    @ColumnInfo(name = "cseKey")
    val cseKey: String = "",
    @ColumnInfo(name = "sseType")
    val sseType: String = "none",
    @ColumnInfo(name = "favorite")
    val favorite: Boolean = false,
    @ColumnInfo(name = "folder")
    val folder: String = "",
    @ColumnInfo(name = "status")
    val status: Int = 3
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

            copy(
                url = url,
                label = label,
                password = password,
                username = username
            )
        }

        return decryptedPassword.apply {
            CoroutineScope(Dispatchers.IO + Job()).launch {
                loadCachedFavicon(context)
            }
        }


    }

    companion object {
        /**
         * Create a list of passwords from a JSON object.
         *
         * @param data The JSON object to parse.
         * @return A list of the parsed passwords.
         */
        fun listFromJson(data: String): List<Password> {
            val passwordList = ArrayList<Password>()

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

                val username = try {
                    obj.getString("username")
                } catch (ex: JSONException) {
                    ""
                }

                val password = try {
                    obj.getString("password")
                } catch (ex: JSONException) {
                    ""
                }

                val url = try {
                    obj.getString("url")
                } catch (ex: JSONException) {
                    ""
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

                val favorite = try {
                    obj.getBoolean("favorite")
                } catch (ex: JSONException) {
                    false
                }

                val folder = try {
                    obj.getString("folder")
                } catch (ex: JSONException) {
                    ""
                }

                val status = try {
                    obj.getInt("status")
                } catch (ex: JSONException) {
                    3
                }

                passwordList.add(
                    Password(
                        id = id,
                        label = label,
                        username = username,
                        password = password,
                        url = url,
                        revision = revision,
                        cseType = cseType,
                        cseKey = cseKey,
                        sseType = sseType,
                        favorite = favorite,
                        folder = folder,
                        status = status
                    )
                )
            }

            return passwordList
        }
    }
}
