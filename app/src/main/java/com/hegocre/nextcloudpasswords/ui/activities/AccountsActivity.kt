package com.hegocre.nextcloudpasswords.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.hegocre.nextcloudpasswords.BuildConfig
import com.hegocre.nextcloudpasswords.ui.components.NCPAccountsScreen
import com.hegocre.nextcloudpasswords.ui.components.NCPAppLockWrapper
import com.hegocre.nextcloudpasswords.ui.viewmodels.PasswordsViewModel
import com.hegocre.nextcloudpasswords.utils.LogHelper
import com.hegocre.nextcloudpasswords.utils.copyToClipboard

class AccountsActivity : FragmentActivity()  {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            NCPAppLockWrapper {
                NCPAccountsScreen(
                    onBackPressed = this::finish,
                    lifecycleScope
                )
            }
        }
    }
}