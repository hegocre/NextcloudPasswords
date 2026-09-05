package com.hegocre.nextcloudpasswords

import androidx.test.platform.app.InstrumentationRegistry
import com.hegocre.nextcloudpasswords.utils.OkHttpRequest
import com.hegocre.nextcloudpasswords.utils.OkHttpRequest.Companion.LocalNetworkAccessPermissionRequiredException
import org.junit.Test

/**
 * Unit test to check TLS Validation on different OkHttpRequest modes as well as LocalNetwork permission.
 */

class OkHttpDeniedLocalNetworkAccessTest {

    @Test(expected = LocalNetworkAccessPermissionRequiredException::class)
    fun deniedSecureLocalNetworkConnectionTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val okHttpRequest = OkHttpRequest.getInstance()
        okHttpRequest.initClient(context)
        okHttpRequest.get("http://10.0.2.2:9999")
    }

    @Test(expected = LocalNetworkAccessPermissionRequiredException::class)
    fun deniedInsecureLocalNetworkConnectionTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val okHttpRequest = OkHttpRequest.getInstance()
        okHttpRequest.initClient(context)
        okHttpRequest.allowInsecureRequests = true
        okHttpRequest.get("http://10.0.2.2:9999")
    }
}