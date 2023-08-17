package com.hegocre.nextcloudpasswords.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.hegocre.nextcloudpasswords.ui.components.NCPAboutScreen

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            NCPAboutScreen(onBackPressed = this::finish)
        }
    }
}