package com.hegocre.nextcloudpasswords.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.hegocre.nextcloudpasswords.data.user.UserController
import com.hegocre.nextcloudpasswords.ui.components.LoginWebView
import com.hegocre.nextcloudpasswords.ui.theme.ThemeProvider
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class WebLoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val loginUrl = intent.getStringExtra("login_url")?.let {
            var url = it
            if (it.last() != '/') url += '/'
            "$url/index.php/login/flow"
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
            UserController.getInstance(this).logIn(
                server,
                user,
                password
            )
            intent.putExtra("loggedIn", true)
            setResult(RESULT_OK, intent)
        } else {
            intent.putExtra("loggedIn", false)
            setResult(RESULT_CANCELED, intent)
        }

        finish()
    }

    companion object {
        private val CRED_REGEX_1 = "nc:.*server:(.*)&user:(.*)&password:(.*)".toRegex()
        private val CRED_REGEX_2 = "nc:.*user:(.*)&password:(.*)&server:(.*)".toRegex()
    }
}

@Composable
fun NCPWebLoginScreen(
    onLoginUrl: (String) -> Unit,
    url: String = ""
) {
    val context = LocalContext.current

    val theme by ThemeProvider.getInstance(context).currentTheme.collectAsState()

    val (loading, setLoading) = remember { mutableStateOf(false) }

    theme.Theme {
        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LoginWebView(
                    onLoginUrl = onLoginUrl,
                    onLoadingChange = setLoading,
                    url = url
                )
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}