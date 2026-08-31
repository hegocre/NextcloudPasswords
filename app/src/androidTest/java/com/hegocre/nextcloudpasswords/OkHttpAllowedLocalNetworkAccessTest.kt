package com.hegocre.nextcloudpasswords

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.hegocre.nextcloudpasswords.utils.OkHttpRequest
import org.junit.Rule
import org.junit.Test
import java.net.ConnectException

/**
 * Unit test to check TLS Validation on different OkHttpRequest modes as well as LocalNetwork permission.
 */

class OkHttpAllowedLocalNetworkAccessTest {
    @Rule
    @JvmField
    val localNetworkPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_LOCAL_NETWORK)

    @Test(expected = ConnectException::class)
    fun allowedSecureLocalNetworkConnectionTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val okHttpRequest = OkHttpRequest.getInstance()
        okHttpRequest.initClient(context)
        okHttpRequest.get("http://10.0.2.2:9999")
    }

    @Test(expected = ConnectException::class)
    fun allowedInsecureLocalNetworkConnectionTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val okHttpRequest = OkHttpRequest.getInstance()
        okHttpRequest.initClient(context)
        okHttpRequest.allowInsecureRequests = true
        okHttpRequest.get("http://10.0.2.2:9999")
    }
}