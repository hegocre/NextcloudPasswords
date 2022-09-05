package com.hegocre.nextcloudpasswords.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.components.LoginView
import com.hegocre.nextcloudpasswords.ui.theme.ThemeProvider
import com.hegocre.nextcloudpasswords.ui.theme.isLight

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val loginIntent = Intent(this, WebLoginActivity::class.java)

        setContent {
            NCPLoginScreen(
                loginIntent = loginIntent,
                onLoginSuccess = {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                },
                onLoginFailed = {
                    Toast.makeText(this, getString(R.string.error_logging_in), Toast.LENGTH_LONG)
                        .show()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NCPLoginScreen(
    loginIntent: Intent,
    onLoginSuccess: () -> Unit,
    onLoginFailed: () -> Unit
) {
    val context = LocalContext.current

    val theme by ThemeProvider.getInstance(context).currentTheme.collectAsState()

    theme.Theme {
        val systemUiController = rememberSystemUiController()
        val useDarkIcons = MaterialTheme.colorScheme.isLight()
        SideEffect {
            systemUiController.setSystemBarsColor(Color.Transparent, useDarkIcons)
        }

        Scaffold(
            topBar = {
                Spacer(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                )
            },
            bottomBar = {
                Spacer(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                )
            }
        ) { innerPadding ->
            LoginView(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                loginIntent = loginIntent,
                onLoginSuccess = onLoginSuccess,
                onLoginFailed = onLoginFailed
            )
        }
    }
}