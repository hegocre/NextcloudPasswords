package com.hegocre.nextcloudpasswords.api.encryption

import com.goterl.lazysodium.interfaces.Box
import com.goterl.lazysodium.interfaces.PwHash
import com.goterl.lazysodium.interfaces.SecretBox
import com.hegocre.nextcloudpasswords.api.encryption.exceptions.SodiumDecryptionException
import com.hegocre.nextcloudpasswords.utils.LazySodiumUtils
import okio.internal.commonToUtf8String
import org.json.JSONException
import org.json.JSONObject

/**
 * Object containing all the keys in the user keychain, used to decrypt data. See the
 * [API reference](https://git.mdns.eu/nextcloud/passwords/-/wikis/Developers/Encryption/CSEv1Keychain).
 *
 * @property keys Encoded keys, each one identified by an id.
 * @property current Id of the current key used to encrypt new data.
 */
data class CSEv1Keychain(
    val keys: Map<String, String>,
    val current: String
) {

    companion object {
        /**
         * Return a JSON string from an encrypted API response. The string must be decrypted
         * using the same password as the one used to open the session.
         *
         * @param data The encrypted data from the response.
         * @param password Master password used to decrypt the response.
         * @return A JSON object with the actual keychain.
         */
        fun decryptJson(data: String, password: String): String {
            val obj = JSONObject(data)

            val encryptedJson = try {
                val keysObj = obj.getJSONObject("keys")
                keysObj.getString("CSEv1r1")
            } catch (ex: JSONException) {
                return ""
            }

            val sodium = LazySodiumUtils.getSodium()

            val p = sodium.bytes(password)
            val key = sodium.sodiumHex2Bin(encryptedJson)
            val keySalt = key.sliceArray(0 until PwHash.SALTBYTES)
            val keyPayload = key.sliceArray(PwHash.SALTBYTES until key.size)

            val decryptionKey = ByteArray(Box.SEEDBYTES)
            if (!sodium.cryptoPwHash(
                    decryptionKey,
                    decryptionKey.size,
                    p,
                    p.size,
                    keySalt,
                    PwHash.OPSLIMIT_INTERACTIVE,
                    PwHash.MEMLIMIT_INTERACTIVE,
                    PwHash.Alg.PWHASH_ALG_ARGON2ID13
                )
            ) throw SodiumDecryptionException("Could not create decryption key")

            val nonce = keyPayload.sliceArray(0 until Box.NONCEBYTES)
            val cipher = keyPayload.sliceArray(Box.NONCEBYTES until keyPayload.size)

            val message = ByteArray(cipher.size - SecretBox.MACBYTES)
            if (!sodium.cryptoSecretBoxOpenEasy(
                    message,
                    cipher,
                    cipher.size.toLong(),
                    nonce,
                    decryptionKey
                )
            ) throw SodiumDecryptionException("Could not open box")

            return message.commonToUtf8String()
        }

        /**
         * Creates a [CSEv1Keychain] from a JSON object.
         *
         * @param data The keychain as a JSON object.
         * @return The keychain created from the JSON.
         */
        fun fromJson(data: String): CSEv1Keychain {
            val obj = JSONObject(data)

            val keyObject = try {
                obj.getJSONObject("keys")
            } catch (ex: JSONException) {
                JSONObject()
            }

            val keys = HashMap<String, String>()
            val jsonKeys = keyObject.keys()
            while (jsonKeys.hasNext()) {
                val uuid = jsonKeys.next()
                val value = keyObject.getString(uuid)
                keys[uuid] = value
            }

            val current = try {
                obj.getString("current")
            } catch (ex: JSONException) {
                ""
            }

            return CSEv1Keychain(keys, current)
        }
    }
}