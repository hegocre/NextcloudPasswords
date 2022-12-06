package com.hegocre.nextcloudpasswords.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.components.LoginView
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme

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
    NextcloudPasswordsTheme {
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