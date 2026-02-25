package com.hegocre.nextcloudpasswords.ui.activities

import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.launch
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.hegocre.nextcloudpasswords.data.user.UserController
import com.hegocre.nextcloudpasswords.ui.components.NCPWebLoginScreen
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class WebLoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val loginUrl = intent.getStringExtra("login_url")?.let {
            var url = it
            if (it.last() != '/') url += '/'
            "${url}index.php/login/flow"
        } ?: ""

        setContent {
            NCPWebLoginScreen(
                onLoginUrl = { url -> processCredentials(url) },
                url = loginUrl
            )
        }
    }

    private fun processCredentials(url: String) {
        val uri = URLDecoder.decode(url, StandardCharsets.UTF_8.toString())
        val match = CRED_REGEX_1.matchEntire(uri)?.groups

        val (user, password, server) = if (match != null) {
            listOf(match[2]?.value, match[3]?.value, match[1]?.value)
        } else {
            val match2 = CRED_REGEX_2.matchEntire(uri)?.groups
            listOf(match2?.get(1)?.value, match2?.get(2)?.value, match2?.get(3)?.value)
        }

        val intent = Intent()
        if (user != null && password != null && server != null) {
            val userController = UserController.getInstance(this)
            lifecycleScope.launch {
                userController.logIn(
                    server,
                    user,
                    password
                )
            }
            intent.putExtra("loggedIn", true)
            setResult(RESULT_OK, intent)

        } else {
            intent.putExtra("loggedIn", false)
            setResult(RESULT_CANCELED, intent)
        }

        finish()
    }

    override fun onDestroy() {
        WebStorage.getInstance().deleteAllData()

        // Clear all the cookies
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        val webView = WebView(this)
        webView.clearCache(true)
        webView.clearFormData()
        webView.clearHistory()
        webView.clearSslPreferences()

        super.onDestroy()
    }

    companion object {
        private val CRED_REGEX_1 = "nc:.*server:(.*)&user:(.*)&password:(.*)".toRegex()
        private val CRED_REGEX_2 = "nc:.*user:(.*)&password:(.*)&server:(.*)".toRegex()
    }
}