package com.hegocre.nextcloudpasswords.data.password

import com.hegocre.nextcloudpasswords.api.encryption.CSEv1Keychain
import com.hegocre.nextcloudpasswords.utils.encryptValue
import kotlinx.serialization.Serializable

/**
 * Data class representing a New Password and containing all its required information.
 *
 * @property password The actual password.
 * @property label User defined label of the password.
 * @property username Username associated with the password.
 * @property url Url of the website.
 * @property notes Notes for the password. Can be formatted with Markdown.
 * @property customFields Custom fields created by the user. (See
 * [custom fields](https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Api/Password-Api#custom-fields)).
 * @property hash SHA1 hash of the password.
 * @property cseType Type of the used server side encryption.
 * @property cseKey UUID of the key used for client side encryption.
 * @property folder UUID of the current folder of the password.
 * @property edited Unix timestamp when the user last changed the password.
 * @property hidden Hides the password in list / find actions.
 * @property favorite True if the user has marked the password as favorite.
 */
@Serializable
data class NewPassword(
    val password: String,
    val label: String,
    val username: String,
    val url: String,
    val notes: String,
    val customFields: String,
    val hash: String,
    val cseType: String,
    val cseKey: String,
    val folder: String,
    val edited: Int,
    val hidden: Boolean,
    val favorite: Boolean,
) {
    fun encrypt(cseKey: String, csEv1Keychain: CSEv1Keychain): NewPassword {
        return this.copy(
            password = password.encryptValue(cseKey, csEv1Keychain),
            label = label.encryptValue(cseKey, csEv1Keychain),
            username = username.encryptValue(cseKey, csEv1Keychain),
            url = url.encryptValue(cseKey, csEv1Keychain),
            notes = notes.encryptValue(cseKey, csEv1Keychain),
            customFields = customFields.encryptValue(cseKey, csEv1Keychain),
            cseType = "CSEv1r1",
            cseKey = cseKey,
        )
    }
}