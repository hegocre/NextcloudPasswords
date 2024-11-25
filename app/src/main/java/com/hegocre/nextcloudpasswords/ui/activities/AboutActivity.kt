package com.hegocre.nextcloudpasswords.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.hegocre.nextcloudpasswords.BuildConfig
import com.hegocre.nextcloudpasswords.ui.components.NCPAboutScreen
import com.hegocre.nextcloudpasswords.ui.components.NCPAppLockWrapper
import com.hegocre.nextcloudpasswords.utils.LogHelper
import com.hegocre.nextcloudpasswords.utils.copyToClipboard

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
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