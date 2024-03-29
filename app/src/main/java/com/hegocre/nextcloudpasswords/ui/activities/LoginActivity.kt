package com.hegocre.nextcloudpasswords.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.components.NCPLoginScreen
import com.hegocre.nextcloudpasswords.utils.PreferencesManager

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val loginIntent = Intent(this, WebLoginActivity::class.java)

        setContent {
            NCPLoginScreen(
                loginIntent = loginIntent,
                onLoginSuccess = {
                    val intent = Intent("com.hegocre.nextcloudpasswords.action.main")
                        .setPackage(packageName)
                    startActivity(intent)
                    finish()
                },
                onLoginFailed = {
                    PreferencesManager.getInstance(this).setSkipCertificateValidation(false)
                    Toast.makeText(this, getString(R.string.error_logging_in), Toast.LENGTH_LONG)
                        .show()
                }
            )
        }
    }
}