package com.hegocre.nextcloudpasswords.ui.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.hegocre.nextcloudpasswords.ui.components.NCPAppLockWrapper
import com.hegocre.nextcloudpasswords.ui.components.NCPSettingsScreen
import com.hegocre.nextcloudpasswords.utils.enableEdgeToEdgeWithoutNavigationBarScrim

class SettingsActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdgeWithoutNavigationBarScrim()

        setContent {
            NCPAppLockWrapper {
                NCPSettingsScreen(
                    onNavigationUp = { finish() }
                )
            }
        }
    }
}