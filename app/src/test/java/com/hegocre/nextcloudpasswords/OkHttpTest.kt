package com.hegocre.nextcloudpasswords

import com.hegocre.nextcloudpasswords.utils.OkHttpRequest
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.net.ssl.SSLHandshakeException

/**
 * Unit test to check TLS Validation on different OkHttpRequest modes.
 */

class OkHttpTest {
    @Test
    fun secureGetConnectionTest() {
        val okHttpRequest = OkHttpRequest.getInstance(false)
        val request = okHttpRequest.get("https://tls-v1-2.badssl.com:1012/")
        assertEquals(request.code, 200)
    }

    @Test(expected = SSLHandshakeException::class)
    fun insecureGetDisallowedConnectionTest() {
        val okHttpRequest = OkHttpRequest.getInstance(false)
        okHttpRequest.get("https://self-signed.badssl.com/")
    }

    @Test
    fun insecureGetAllowedConnectionTest() {
        val okHttpRequest = OkHttpRequest.getInstance(true)
        val request = okHttpRequest.get("https://self-signed.badssl.com/")
        assertEquals(request.code, 200)
    }
}