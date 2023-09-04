package com.hegocre.nextcloudpasswords.utils

import android.content.Context
import com.goterl.lazysodium.interfaces.SecretBox
import com.goterl.lazysodium.utils.Key
import com.hegocre.nextcloudpasswords.api.encryption.CSEv1Keychain
import com.hegocre.nextcloudpasswords.api.exceptions.SodiumDecryptionException
import com.hegocre.nextcloudpasswords.data.folder.Folder
import com.hegocre.nextcloudpasswords.data.password.Password
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

suspend fun List<Password>.decryptPasswords(
    context: Context,
    csEv1Keychain: CSEv1Keychain? = null
): List<Password> =
    withContext(Dispatchers.Default) {
        val decryptedList: MutableList<Password> = mutableListOf()

        forEach { password ->
            //Decrypt password
            val decryptedPassword = password.decrypt(context, csEv1Keychain)
            if (decryptedPassword != null)
                decryptedList.add(decryptedPassword)
        }

        decryptedList.toList()
    }

suspend fun List<Folder>.decryptFolders(csEv1Keychain: CSEv1Keychain? = null): List<Folder> =
    withContext(Dispatchers.Default) {
        val decryptedList: MutableList<Folder> = mutableListOf()

        forEach { folder ->
            //Decrypt password
            val decryptedFolder = folder.decrypt(csEv1Keychain)
            if (decryptedFolder != null)
                decryptedList.add(decryptedFolder)
        }

        decryptedList.toList()
    }

/**
 * Decrypt and encrypted value using the provided key id from the keychain.
 *
 * @param cseKey The id of the key used to encrypt and decrypt the value.
 * @param csEv1Keychain The keychain containing the key.
 * @return The decrypted value.
 */
fun String.decryptValue(cseKey: String, csEv1Keychain: CSEv1Keychain): String {
    if (this.isEmpty() || cseKey.isEmpty()) return this

    val sodium = LazySodiumUtils.getSodium()

    val value = sodium.sodiumHex2Bin(this)
    val nonce = value.sliceArray(0 until SecretBox.NONCEBYTES)
    val cipher = value.sliceArray(SecretBox.NONCEBYTES until value.size)
    val decryptionKey = sodium.sodiumHex2Bin(csEv1Keychain.keys[cseKey]!!)

    val message = ByteArray(cipher.size - SecretBox.MACBYTES)
    if (!sodium.cryptoSecretBoxOpenEasy(
            message,
            cipher,
            cipher.size.toLong(),
            nonce,
            decryptionKey
        )
    ) throw SodiumDecryptionException("Could not decrypt value")

    return message.toString(Charsets.UTF_8)
}

fun String.encryptValue(cseKey: String, csEv1Keychain: CSEv1Keychain): String {
    if (this.isEmpty() || cseKey.isEmpty()) return this

    val sodium = LazySodiumUtils.getSodium()

    val nonce = sodium.randomBytesBuf(SecretBox.NONCEBYTES)
    val encryptionKey = Key.fromHexString(csEv1Keychain.keys[cseKey]!!)
    val encryptedContent = sodium.cryptoSecretBoxEasy(this, nonce, encryptionKey)

    return sodium.sodiumBin2Hex(nonce) + encryptedContent
}

fun String.sha1Hash(): String {
    val digest = MessageDigest.getInstance("SHA-1")
    val result = digest.digest(this.toByteArray())
    val sb = StringBuilder()
    for (b in result) {
        sb.append(String.format("%02x", b))
    }
    return sb.toString()
}