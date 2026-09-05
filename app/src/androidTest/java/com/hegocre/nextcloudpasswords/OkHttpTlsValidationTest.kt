package com.hegocre.nextcloudpasswords

import androidx.test.platform.app.InstrumentationRegistry
import com.hegocre.nextcloudpasswords.utils.OkHttpRequest
import org.junit.Assert
import org.junit.Test
import javax.net.ssl.SSLHandshakeException

/**
 * Unit test to check TLS Validation on different OkHttpRequest modes as well as LocalNetwork permission.
 */

class OkHttpTlsValidationTest {
    @Test
    fun secureGetConnectionTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val okHttpRequest = OkHttpRequest.getInstance()
        okHttpRequest.initClient(context)
        okHttpRequest.allowInsecureRequests = false
        val request = okHttpRequest.get("https://tls-v1-2.badssl.com:1012/")
        Assert.assertEquals(request.code, 200)
    }

    @Test(expected = SSLHandshakeException::class)
    fun insecureGetDisallowedConnectionTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val okHttpRequest = OkHttpRequest.getInstance()
        okHttpRequest.initClient(context)
        okHttpRequest.allowInsecureRequests = false
        okHttpRequest.get("https://self-signed.badssl.com/")
    }

    @Test
    fun insecureGetAllowedConnectionTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val okHttpRequest = OkHttpRequest.getInstance()
        okHttpRequest.initClient(context)
        okHttpRequest.allowInsecureRequests = true
        val request = okHttpRequest.get("https://self-signed.badssl.com/")
        Assert.assertEquals(request.code, 200)
    }
}