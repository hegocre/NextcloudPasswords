package com.hegocre.nextcloudpasswords

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.hegocre.nextcloudpasswords.api.ApiController
import com.hegocre.nextcloudpasswords.utils.OkHttpRequest
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ApiControllerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        with(PreferencesManager.getInstance(context)) {
            setLoggedInServer("")
            setLoggedInUser("")
            setLoggedInPassword("")
        }
    }

    @Test
    fun ignoreCertificateErrorsTest() {
        PreferencesManager.getInstance(context).setSkipCertificateValidation(true)
        ApiController.getInstance(context)
        val okHttpRequest = OkHttpRequest.getInstance(true)
        val request = okHttpRequest.get("https://self-signed.badssl.com/")
        Assert.assertEquals(request.code, 200)
    }
}