package com.hegocre.nextcloudpasswords.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class OTPTest {

    @Test
    fun parseValidTotpUrlDefaultValues() {
        val url = "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example"
        val otp = OTP.fromUrl(url)

        assertEquals("JBSWY3DPEHPK3PXP", otp.secret)
        assertEquals("totp", otp.type)
        assertEquals("sha1", otp.algorithm)
        assertEquals(6, otp.digits)
        assertEquals(30, otp.period)
        assertEquals(0L, otp.counter)
        assertEquals("Example", otp.issuer)
        assertEquals("alice@google.com", otp.accountName)
    }

    @Test
    fun parseValidTotpUrlCustomParameters() {
        val url = "otpauth://totp/My%20Service:user123?secret=GEZDGNBVGY3TQOJQ&issuer=My%20Service&algorithm=SHA256&digits=8&period=60"
        val otp = OTP.fromUrl(url)

        assertEquals("GEZDGNBVGY3TQOJQ", otp.secret)
        assertEquals("totp", otp.type)
        assertEquals("sha256", otp.algorithm)
        assertEquals(8, otp.digits)
        assertEquals(60, otp.period)
        assertEquals("My Service", otp.issuer)
        assertEquals("user123", otp.accountName)
    }

    @Test
    fun parseValidHotpUrl() {
        val url = "otpauth://hotp/Bank:alice@bank.com?secret=JBSWY3DPEHPK3PXP&counter=5"
        val otp = OTP.fromUrl(url)

        assertEquals("JBSWY3DPEHPK3PXP", otp.secret)
        assertEquals("hotp", otp.type)
        assertEquals(5L, otp.counter)
        assertEquals("Bank", otp.issuer)
        assertEquals("alice@bank.com", otp.accountName)
    }

    @Test(expected = OtpParseException.InvalidUrl::class)
    fun invalidSchemeThrowsException() {
        OTP.fromUrl("http://totp/example?secret=JBSWY3DPEHPK3PXP")
    }

    @Test(expected = OtpParseException.InvalidType::class)
    fun invalidTypeThrowsException() {
        OTP.fromUrl("otpauth://unknown/example?secret=JBSWY3DPEHPK3PXP")
    }

    @Test(expected = OtpParseException.MissingSecret::class)
    fun missingSecretThrowsException() {
        OTP.fromUrl("otpauth://totp/example")
    }

    @Test(expected = OtpParseException.InvalidSecret::class)
    fun invalidSecretAlphabetThrowsException() {
        OTP.fromUrl("otpauth://totp/example?secret=INVALID_SECRET_123!")
    }

    @Test(expected = OtpParseException.InvalidAlgorithm::class)
    fun invalidAlgorithmThrowsException() {
        OTP.fromUrl("otpauth://totp/example?secret=JBSWY3DPEHPK3PXP&algorithm=MD5")
    }

    @Test(expected = OtpParseException.MissingCounter::class)
    fun hotpMissingCounterThrowsException() {
        OTP.fromUrl("otpauth://hotp/example?secret=JBSWY3DPEHPK3PXP")
    }

    @Test(expected = OtpParseException.MissingCounter::class)
    fun hotpNegativeCounterThrowsException() {
        OTP.fromUrl("otpauth://hotp/example?secret=JBSWY3DPEHPK3PXP&counter=-1")
    }

    @Test(expected = OtpParseException.InvalidDigits::class)
    fun invalidDigitsTooSmallThrowsException() {
        OTP.fromUrl("otpauth://totp/example?secret=JBSWY3DPEHPK3PXP&digits=5")
    }

    @Test(expected = OtpParseException.InvalidDigits::class)
    fun invalidDigitsTooLargeThrowsException() {
        OTP.fromUrl("otpauth://totp/example?secret=JBSWY3DPEHPK3PXP&digits=10")
    }

    @Test(expected = OtpParseException.InvalidDigits::class)
    fun invalidDigitsNonNumericThrowsException() {
        OTP.fromUrl("otpauth://totp/example?secret=JBSWY3DPEHPK3PXP&digits=abc")
    }

    @Test(expected = OtpParseException.InvalidPeriod::class)
    fun invalidPeriodZeroThrowsException() {
        OTP.fromUrl("otpauth://totp/example?secret=JBSWY3DPEHPK3PXP&period=0")
    }

    @Test(expected = OtpParseException.InvalidPeriod::class)
    fun invalidPeriodNegativeThrowsException() {
        OTP.fromUrl("otpauth://totp/example?secret=JBSWY3DPEHPK3PXP&period=-30")
    }

    @Test(expected = OtpParseException.InvalidPeriod::class)
    fun invalidPeriodNonNumericThrowsException() {
        OTP.fromUrl("otpauth://totp/example?secret=JBSWY3DPEHPK3PXP&period=abc")
    }
}
