package com.hegocre.nextcloudpasswords.utils

import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.HmacOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.HmacOneTimePasswordGenerator
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import org.apache.commons.codec.binary.Base32
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalSerializationApi::class)
@JsonIgnoreUnknownKeys
@Serializable
data class OTP(
    val secret: String,
    val type: String = Type.TOTP,
    val algorithm: String = Algorithm.SHA1,
    val digits: Int = 6,
    val counter: Int = 0,
    val period: Int = 30,
    //val issuer: String? = null,
    //val accountName: String? = null,
) {
    fun getCurrent(): Pair<String?, Long?> {
        val secret = Base32().decode(secret)

        if (type == Type.TOTP) {
            val config = TimeBasedOneTimePasswordConfig(
                timeStep = period.toLong(),
                timeStepUnit = TimeUnit.SECONDS,
                codeDigits = digits,
                hmacAlgorithm = Algorithm.toHmacAlgorithm(algorithm)
            )
            val generator = TimeBasedOneTimePasswordGenerator(
                secret = secret,
                config = config
            )
            val counter = generator.counter()
            val endEpoch = generator.timeslotStart(counter + 1) - 1

            return Pair(generator.generate(), endEpoch)
        }
        else if (type == Type.HOTP) {
            val config = HmacOneTimePasswordConfig(
                codeDigits = digits,
                hmacAlgorithm = Algorithm.toHmacAlgorithm(algorithm)
            )
            val generator = HmacOneTimePasswordGenerator(
                secret = secret,
                config = config
            )
            return Pair(generator.generate(counter.toLong()), null)
        }

        return Pair(null, null)
    }

    companion object {
        const val CUSTOM_FIELD_LABEL = "client.ios.otp"

        object Type {
            const val HOTP = "hotp"
            const val TOTP = "totp"
        }

        object Algorithm {
            const val SHA1 = "sha1"
            const val SHA256 = "sha256"
            const val SHA512 = "sha512"

            fun toHmacAlgorithm(algorithm: String): HmacAlgorithm = when (algorithm) {
                "sha1" -> HmacAlgorithm.SHA1
                "sha256" -> HmacAlgorithm.SHA256
                "sha512" -> HmacAlgorithm.SHA512
                else -> HmacAlgorithm.SHA1
            }
        }

        fun fromUrl(url: String): OTP {
            return OTP(secret = url)
        }

    }
}

fun String.formatOtp(chunkSize: Int? = null): String {
    val digitsOnly = this.filter { it.isDigit() }
    val n = digitsOnly.length
    val size = chunkSize ?: when {
        n % 4 == 0 && n % 3 != 0 -> 4
        n % 3 == 0 -> 3
        else -> 3
    }

    return digitsOnly.chunked(size).joinToString(" ")
}