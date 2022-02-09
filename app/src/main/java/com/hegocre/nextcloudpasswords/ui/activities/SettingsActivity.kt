package com.hegocre.nextcloudpasswords.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hegocre.nextcloudpasswords.ui.components.NCPSettingsScreen

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NCPSettingsScreen(
                onNavigationUp = { finish() }
            )
        }
    }
}