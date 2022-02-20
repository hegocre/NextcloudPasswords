package com.hegocre.nextcloudpasswords.ui.activities

import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.api.ApiController
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.data.user.UserController
import com.hegocre.nextcloudpasswords.data.viewmodels.PasswordsViewModel
import com.hegocre.nextcloudpasswords.services.NCPAutofillService
import com.hegocre.nextcloudpasswords.ui.components.NextcloudPasswordsApp
import com.hegocre.nextcloudpasswords.utils.AssistStructureParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
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
                    val structure: AssistStructure? =
                        intent.getParcelableExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE)
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

        setContent {
            NextcloudPasswordsApp(
                passwordsViewModel = passwordsViewModel,
                onLogOut = { logOut() },
                onPasswordClick = onPasswordClick,
                isAutofillRequest = autofillRequested,
                defaultSearchQuery = autofillSearchQuery
            )
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
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun autofillReply(password: Password, structure: AssistStructure) {
        val helper = AssistStructureParser(structure)

        val usernamePresentation =
            RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                setTextViewText(android.R.id.text1, password.label)
            }
        val passwordPresentation =
            RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                setTextViewText(android.R.id.text1, password.label)
            }

        val fillResponse = FillResponse.Builder()
            .addDataset(Dataset.Builder()
                .apply {
                    helper.nodes.forEach { node ->
                        if (node.isFocused) {
                            node.autofillId?.let { autofillId ->
                                setValue(
                                    autofillId,
                                    AutofillValue.forText(password.username),
                                    usernamePresentation
                                )
                            }
                        }
                    }
                    helper.usernameAutofillIds.forEach { autofillId ->
                        setValue(
                            autofillId,
                            AutofillValue.forText(password.username),
                            usernamePresentation
                        )
                    }
                    helper.passwordAutofillIds.forEach { autofillId ->
                        setValue(
                            autofillId,
                            AutofillValue.forText(password.password),
                            passwordPresentation
                        )
                    }
                }
                .build()
            ).build()

        val replyIntent = Intent().apply {
            putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, fillResponse)
        }

        setResult(Activity.RESULT_OK, replyIntent)

        finish()
    }
}

