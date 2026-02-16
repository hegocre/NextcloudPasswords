package com.hegocre.nextcloudpasswords.ui.activities

import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import com.hegocre.nextcloudpasswords.BuildConfig
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.api.ApiController
import com.hegocre.nextcloudpasswords.data.user.UserController
import com.hegocre.nextcloudpasswords.services.autofill.AutofillHelper
import com.hegocre.nextcloudpasswords.services.autofill.SaveData
import com.hegocre.nextcloudpasswords.services.autofill.PasswordAutofillData
import com.hegocre.nextcloudpasswords.services.autofill.NCPAutofillService
import com.hegocre.nextcloudpasswords.services.autofill.AssistStructureParser
import com.hegocre.nextcloudpasswords.ui.components.NCPAppLockWrapper
import com.hegocre.nextcloudpasswords.ui.components.NextcloudPasswordsApp
import com.hegocre.nextcloudpasswords.ui.components.NextcloudPasswordsAppForAutofill
import com.hegocre.nextcloudpasswords.ui.viewmodels.PasswordsViewModel
import com.hegocre.nextcloudpasswords.utils.LogHelper
import com.hegocre.nextcloudpasswords.utils.OkHttpRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import android.util.Log

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.DEBUG) LogHelper.getInstance()

        super.onCreate(savedInstanceState)
        if (!UserController.getInstance(this).isLoggedIn) {
            login()
            return
        }

        val passwordsViewModel by viewModels<PasswordsViewModel>()

        val autofillRequested: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.getBooleanExtra(NCPAutofillService.AUTOFILL_REQUEST, false)
        } else {
            false
        }

        val passwordId: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.getStringExtra(NCPAutofillService.PASSWORD_ID) ?: null
        } else {
            null
        }

        val saveData: SaveData? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableExtra(
                    NCPAutofillService.SAVE_DATA,
                    SaveData::class.java
                )
            else
                @Suppress("DEPRECATION") intent.getParcelableExtra(NCPAutofillService.SAVE_DATA)
        } else {
            null
        }

        val autofillSearchQuery =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && autofillRequested) {
                intent.getStringExtra(NCPAutofillService.AUTOFILL_SEARCH_HINT) ?: ""
            } else {
                ""
            }

        val replyAutofill: ((String, String, String) -> Unit)? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && autofillRequested
            ) {
                { label, username, password ->
                    val structure = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        intent.getParcelableExtra(
                            AutofillManager.EXTRA_ASSIST_STRUCTURE,
                            AssistStructure::class.java
                        )
                    else
                        @Suppress("DEPRECATION") intent.getParcelableExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE)

                    Log.d("MainActivity", "Replying to autofill request with label: $label, structure: $structure")

                    if (structure == null) {
                        setResult(RESULT_CANCELED)
                        finish()
                    } else {
                        autofillReply(PasswordAutofillData(
                            id = null, 
                            label = label, 
                            username = username, 
                            password = password
                        ), structure)
                    }
                }
            } else null


        passwordsViewModel.clientDeauthorized.observe(this) { deauthorized ->
            if (deauthorized) {
                Toast.makeText(this, R.string.client_deauthorized_toast, Toast.LENGTH_LONG).show()
                logOut()
            }
        }

        Coil.setImageLoader {
            ImageLoader.Builder(this)
                .okHttpClient { OkHttpRequest.getInstance().client }
                .diskCache {
                    DiskCache.Builder()
                        .directory(this.cacheDir.resolve("image_cache"))
                        .build()
                }.build()
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (passwordId != null || saveData != null) {
            setContent {
                NCPAppLockWrapper {
                    NextcloudPasswordsAppForAutofill(
                        passwordsViewModel = passwordsViewModel,
                        onLogOut = { logOut() },
                        replyAutofill = replyAutofill,
                        passwordId = passwordId,
                        isAutofillRequest = autofillRequested,
                        saveData = saveData,
                        defaultSearchQuery = autofillSearchQuery
                    )
                }
            }
        } else {
            setContent {
                NCPAppLockWrapper {
                    NextcloudPasswordsApp(
                        passwordsViewModel = passwordsViewModel,
                        onLogOut = { logOut() },
                        replyAutofill = replyAutofill,
                        isAutofillRequest = autofillRequested,
                        defaultSearchQuery = autofillSearchQuery
                    )
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
    private fun autofillReply(
        password: PasswordAutofillData,
        structure: AssistStructure
    ) {
        val dataset = AutofillHelper.buildDataset(this, password, AssistStructureParser(structure), null, null, false)

        val replyIntent = Intent().apply {
            putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset)
        }

        setResult(RESULT_OK, replyIntent)

        finish()
    }
}

