package com.hegocre.nextcloudpasswords.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hegocre.nextcloudpasswords.BuildConfig
import com.hegocre.nextcloudpasswords.ui.components.NCPAboutScreen
import com.hegocre.nextcloudpasswords.ui.components.NCPAppLockWrapper
import com.hegocre.nextcloudpasswords.utils.LogHelper
import com.hegocre.nextcloudpasswords.utils.copyToClipboard
import com.hegocre.nextcloudpasswords.utils.enableEdgeToEdgeWithoutNavigationBarScrim

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdgeWithoutNavigationBarScrim()
        setContent {
            NCPAppLockWrapper {
                NCPAboutScreen(
                    onBackPressed = this::finish,
                    onLogoLongPressed = {
                        if (BuildConfig.DEBUG) {
                            copyToClipboard(LogHelper.getInstance().appLog)
                            Toast.makeText(this, "Log copied!", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
    }
}