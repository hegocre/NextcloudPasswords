package com.hegocre.nextcloudpasswords.utils

import android.net.Uri
import androidx.core.net.toUri
import com.hegocre.nextcloudpasswords.R
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
    val counter: Long = 0,
    val period: Int = 30,
    val issuer: String? = null,
    val accountName: String? = null,
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
            return Pair(generator.generate(counter), null)
        }

        return Pair(null, null)
    }

    fun getNext(): OTP {
        return this.copy(counter = counter + 1)
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
            val uri = url.toUri()

            if (uri.scheme?.equals("otpauth", ignoreCase = true) != true) {
                throw OtpParseException.InvalidUrl()
            }

            val type = when (uri.host?.lowercase()) {
                "totp" -> Type.TOTP
                "hotp" -> Type.HOTP
                else -> throw OtpParseException.InvalidType()
            }

            val label = uri.encodedPath?.removePrefix("/") ?: ""
            val labelParts = label.split(":", limit = 2).map { Uri.decode(it).trim() }
            val labelIssuer = labelParts.getOrNull(1)?.let { labelParts[0] }
            val accountName = (labelParts.getOrNull(1) ?: labelParts[0]).let { it.ifBlank { null } }

            val secret = uri.getQueryParameter("secret")
                ?: throw OtpParseException.MissingSecret()

            if (!Base32().isInAlphabet(secret)) {
                throw OtpParseException.InvalidSecret()
            }

            val issuer = uri.getQueryParameter("issuer") ?: labelIssuer

            val algorithm = when(uri.getQueryParameter("algorithm")?.lowercase()) {
                "sha1" -> Algorithm.SHA1
                "sha256" -> Algorithm.SHA256
                "sha512" -> Algorithm.SHA512
                null -> Algorithm.SHA1
                else -> throw OtpParseException.InvalidAlgorithm()
            }

            val digits = uri.getQueryParameter("digits")?.toIntOrNull()
                ?.takeIf { it in 6..9 }

            val counter = uri.getQueryParameter("counter")?.toLongOrNull()
                ?.takeUnless { it < 0 }

            if (type == Type.HOTP && counter == null) {
                throw OtpParseException.MissingCounter()
            }

            val period = uri.getQueryParameter("period")?.toIntOrNull()
                ?.takeIf { it > 0 }

            return OTP(
                secret = secret,
                type = type,
                algorithm = algorithm,
                digits = digits ?: 6,
                counter = counter ?: 0L,
                period = period ?: 30,
                issuer = issuer,
                accountName = accountName
            )
        }

    }
}

sealed class OtpParseException(val stringResId: Int) : IllegalArgumentException() {
    class InvalidUrl(resId: Int = R.string.error_invalid_otp_url) : OtpParseException(resId)
    class InvalidType(resId: Int = R.string.error_invalid_otp_type) : OtpParseException(resId)
    class MissingSecret(resId: Int = R.string.error_missing_secret) : OtpParseException(resId)
    class InvalidSecret(resId: Int = R.string.error_invalid_secret) : OtpParseException(resId)
    class InvalidAlgorithm(resId: Int = R.string.error_invalid_algorithm) : OtpParseException(resId)
    class MissingCounter(resId: Int = R.string.error_missing_counter) : OtpParseException(resId)
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