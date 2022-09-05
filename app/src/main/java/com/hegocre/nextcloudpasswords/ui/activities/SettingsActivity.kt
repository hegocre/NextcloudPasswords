package com.hegocre.nextcloudpasswords.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.hegocre.nextcloudpasswords.ui.components.NCPSettingsScreen

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            NCPSettingsScreen(
                onNavigationUp = { finish() }
            )
        }
    }
}