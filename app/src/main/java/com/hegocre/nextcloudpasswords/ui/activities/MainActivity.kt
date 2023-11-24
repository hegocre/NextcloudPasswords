package com.hegocre.nextcloudpasswords.ui.activities

import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.hegocre.nextcloudpasswords.BuildConfig
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.api.ApiController
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.data.user.UserController
import com.hegocre.nextcloudpasswords.services.autofill.AutofillHelper
import com.hegocre.nextcloudpasswords.services.autofill.NCPAutofillService
import com.hegocre.nextcloudpasswords.ui.components.NextcloudPasswordsApp
import com.hegocre.nextcloudpasswords.ui.components.NextcloudPasswordsAppLock
import com.hegocre.nextcloudpasswords.ui.viewmodels.PasswordsViewModel
import com.hegocre.nextcloudpasswords.utils.LogHelper
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.DEBUG) LogHelper.getInstance()

        super.onCreate(savedInstanceState)
        if (!UserController.getInstance(this).isLoggedIn) {
            login()
            return
        }

        val passwordsViewModel by viewModels<PasswordsViewModel>()

        val autofillRequested = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.getBooleanExtra(NCPAutofillService.AUTOFILL_REQUEST, false)
        } else {
            false
        }

        val autofillSearchQuery =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && autofillRequested) {
                intent.getStringExtra(NCPAutofillService.AUTOFILL_SEARCH_HINT) ?: ""
            } else {
                ""
            }

        val onPasswordClick: ((Password) -> Unit)? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && autofillRequested
            ) {
                { password ->
                    val structure = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        intent.getParcelableExtra(
                            AutofillManager.EXTRA_ASSIST_STRUCTURE,
                            AssistStructure::class.java
                        )
                    else
                        @Suppress("DEPRECATION") intent.getParcelableExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE)

                    if (structure == null) {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    } else {
                        autofillReply(password, structure)
                    }
                }
            } else null


        passwordsViewModel.clientDeauthorized.observe(this) { deauthorized ->
            if (deauthorized) {
                Toast.makeText(this, R.string.client_deauthorized_toast, Toast.LENGTH_LONG).show()
                logOut()
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val hasAppLock by PreferencesManager.getInstance(this).getHasAppLock()
                .collectAsState(null)
            val isLocked by passwordsViewModel.isLocked.collectAsState()

            hasAppLock?.let {
                Crossfade(targetState = it && isLocked, label = "locked") { locked ->
                    if (locked) {
                        NextcloudPasswordsAppLock(
                            onCheckPasscode = passwordsViewModel::checkPasscode,
                            onCorrectPasscode = passwordsViewModel::disableLock
                        )
                    } else {
                        passwordsViewModel.disableLock()
                        NextcloudPasswordsApp(
                            passwordsViewModel = passwordsViewModel,
                            onLogOut = { logOut() },
                            onPasswordClick = onPasswordClick,
                            isAutofillRequest = autofillRequested,
                            defaultSearchQuery = autofillSearchQuery
                        )
                    }
                }
            }
        }
    }

    private fun logOut() {
        val logOutJob = SupervisorJob()
        val logOutScope = CoroutineScope(Dispatchers.IO + logOutJob)
        logOutScope.launch {
            ApiController.getInstance(this@MainActivity).closeSession()
            UserController.getInstance(this@MainActivity).logOut()
            triggerRebirth()
        }
    }

    private fun triggerRebirth() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val componentName = intent?.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }

    private fun login() {
        val intent = Intent("com.hegocre.nextcloudpasswords.action.login")
            .setPackage(packageName)
        startActivity(intent)
        finish()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun autofillReply(password: Password, structure: AssistStructure) {
        val dataset = AutofillHelper.buildDataset(this, password, structure, null)

        val replyIntent = Intent().apply {
            putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset)
        }

        setResult(Activity.RESULT_OK, replyIntent)

        finish()
    }
}

