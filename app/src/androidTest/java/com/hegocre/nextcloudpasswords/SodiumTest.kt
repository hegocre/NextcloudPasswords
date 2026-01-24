package com.hegocre.nextcloudpasswords

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.AEAD
import com.goterl.lazysodium.interfaces.Box
import com.goterl.lazysodium.interfaces.GenericHash
import com.goterl.lazysodium.interfaces.PwHash
import com.hegocre.nextcloudpasswords.api.encryption.CSEv1Keychain
import com.hegocre.nextcloudpasswords.utils.LazySodiumUtils
import com.hegocre.nextcloudpasswords.utils.decryptValue
import com.hegocre.nextcloudpasswords.utils.encryptValue
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import java.util.Locale

class SodiumTest {

    @Ignore("passwordHash returns something. Needs to be checked.")
    @Test
    fun testSodiumSolve() {
        val salts = Array(3) { "" }
        salts[0] = ""
        salts[1] = ""
        salts[2] = ""

        val password = ""

        val sodium = LazySodiumAndroid(SodiumAndroid())

        val passwordSalt = sodium.sodiumHex2Bin(salts[0])
        val genericHashKey = sodium.sodiumHex2Bin(salts[1])
        val passwordHashSalt = sodium.sodiumHex2Bin(salts[2])
        val input = sodium.bytes(password) + passwordSalt

        val genericHash = ByteArray(GenericHash.BYTES_MAX)
        !sodium.cryptoGenericHash(
            genericHash,
            genericHash.size,
            input,
            input.size.toLong(),
            genericHashKey,
            genericHashKey.size
        )

        val passwordHash = ByteArray(Box.SEEDBYTES)
        !sodium.cryptoPwHash(
            passwordHash,
            passwordHash.size,
            genericHash,
            genericHash.size,
            passwordHashSalt,
            PwHash.OPSLIMIT_INTERACTIVE,
            PwHash.MEMLIMIT_INTERACTIVE,
            PwHash.Alg.PWHASH_ALG_ARGON2ID13
        )

        val secret = sodium.sodiumBin2Hex(passwordHash)
        assertEquals("", secret.lowercase(Locale.getDefault()))
    }

    @Test
    fun decryption_isCorrect() {
        val sodium = LazySodiumUtils.getSodium()
        val key = sodium.keygen(AEAD.Method.AES256GCM)
        val csEv1Keychain = CSEv1Keychain(
            mapOf("test_key" to key.asHexString),
            "test_key"
        )

        val testString = "abcdefg_12345678"
        val encryptedString = testString.encryptValue("test_key", csEv1Keychain)
        val decryptedString = encryptedString.decryptValue("test_key", csEv1Keychain)

        assertEquals(testString, decryptedString)
    }
}