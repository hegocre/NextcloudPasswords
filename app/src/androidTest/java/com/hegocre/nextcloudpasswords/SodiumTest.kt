package com.hegocre.nextcloudpasswords

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.Box
import com.goterl.lazysodium.interfaces.GenericHash
import com.goterl.lazysodium.interfaces.PwHash
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

class SodiumTest {

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
        assertTrue(secret.lowercase(Locale.getDefault()) == "")
    }
}