package com.hegocre.nextcloudpasswords.services.autofill

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import androidx.compose.runtime.collectAsState
import android.util.Log
import android.annotation.TargetApi
import androidx.lifecycle.asFlow
import com.hegocre.nextcloudpasswords.api.ApiController
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.data.password.PasswordController
import com.hegocre.nextcloudpasswords.data.password.NewPassword
import com.hegocre.nextcloudpasswords.data.password.UpdatedPassword
import com.hegocre.nextcloudpasswords.data.user.UserController
import com.hegocre.nextcloudpasswords.data.user.UserException
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import com.hegocre.nextcloudpasswords.utils.decryptPasswords
import com.hegocre.nextcloudpasswords.utils.AppLockHelper
import com.hegocre.nextcloudpasswords.ui.activities.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CancellationException
import android.content.Context
import android.content.IntentSender
import com.hegocre.nextcloudpasswords.utils.encryptValue
import com.hegocre.nextcloudpasswords.utils.sha1Hash
import com.hegocre.nextcloudpasswords.api.FoldersApi
import com.hegocre.nextcloudpasswords.utils.AutofillData
import com.hegocre.nextcloudpasswords.utils.PasswordAutofillData
import com.hegocre.nextcloudpasswords.utils.SaveData

data class ListDecryptionStateNonNullable<T>(
    val decryptedList: List<T> = emptyList(),
    val isLoading: Boolean = false
)

@TargetApi(Build.VERSION_CODES.O)
class NCPAutofillService : AutofillService() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private val preferencesManager by lazy { PreferencesManager.getInstance(applicationContext) }
    private val apiController by lazy { ApiController.getInstance(applicationContext) }
    private val passwordController by lazy { PasswordController.getInstance(applicationContext) }
    private val userController by lazy { UserController.getInstance(applicationContext) }
    private val appLockHelper by lazy { AppLockHelper.getInstance(applicationContext) }

    private val hasAppLock by lazy { preferencesManager.getHasAppLock() }
    private val isLocked by lazy { appLockHelper.isLocked }
    
    val orderBy by lazy { preferencesManager.getOrderBy() }
    val searchByUsername by lazy { preferencesManager.getSearchByUsername() }
    val strictUrlMatching by lazy { preferencesManager.getUseStrictUrlMatching() }

    private lateinit var decryptedPasswordsState: StateFlow<ListDecryptionStateNonNullable<Password>>
    
    private val passwordsDecrypted = MutableStateFlow(false)

    override fun onCreate() {
        super.onCreate()
        decryptedPasswordsState = combine(
            passwordController.getPasswords().asFlow(),
            apiController.csEv1Keychain.asFlow()
        ) { passwords, keychain ->
            ListDecryptionStateNonNullable<Password>(isLoading = true)
            ListDecryptionStateNonNullable(passwords.filter { !it.trashed && !it.hidden }.decryptPasswords(keychain), false)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = serviceScope, 
            started = SharingStarted.Eagerly, 
            initialValue = ListDecryptionStateNonNullable(isLoading = true)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() 
    }

    @SuppressLint("RestrictedApi")
    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val job = serviceScope.launch {
            try {
                val response = withContext(Dispatchers.Default) {
                    processFillRequest(request)
                }
                if (response != null) callback.onSuccess(response) 
                else callback.onFailure("Could not complete fill request")
            } catch (e: CancellationException) {
                throw e 
            } catch (e: Exception) {
                Log.e(TAG, "Error handling fill request: ${e.message}")
                callback.onSuccess(null)
            }
        }

        cancellationSignal.setOnCancelListener {
            job.cancel()
        }
    }

    private suspend fun processFillRequest(request: FillRequest): FillResponse? {
        Log.d(TAG, "Processing fill request")
        val context = request.fillContexts.last() ?: return null
        val helper = AssistStructureParser(context.structure)

        // Do not autofill this application
        if (helper.packageName == packageName) return null

        if (helper.usernameAutofillIds.isEmpty() && helper.passwordAutofillIds.isEmpty()) {
            Log.e(TAG, "No username or password fields detected, cannot autofill")
            return null
        }

        // TODO: when to sync with server?
        // Check Login Status
        //try {
        //    userController.getServer()
        //} catch (_: UserException) {
        //    Log.e(TAG, "User not logged in, cannot autofill")
        //    return null
        //}

        //Log.d(TAG, "User is logged in")

        // Try to open Session
        //if (!apiController.sessionOpen.value && !apiController.openSession(preferencesManager.getMasterPassword())) {
        //    Log.w(TAG, "Session is not open and cannot be opened")
        //}
        //Log.d(TAG, "Session is open")

        //if (apiController.sessionOpen.value) {
        //    passwordController.syncPasswords()
        //}

        // Determine Search Hint
        val searchHint = helper.webDomain ?: getAppLabel(helper.packageName)

        Log.d(TAG, "Search hint determined: $searchHint")

        // wait for passwords to be decrypted, then filter by search hint and sort them
        decryptedPasswordsState.first { !it.isLoading }

        val filteredList = decryptedPasswordsState.value.decryptedList.filter {
            it.matches(searchHint, strictUrlMatching.first()) || 
            (searchByUsername.first() && it.username.contains(searchHint, ignoreCase = true))
        }.let { list ->
            when (orderBy.first()) {
                PreferencesManager.ORDER_BY_TITLE_DESCENDING -> list.sortedByDescending { it.label.lowercase() }
                PreferencesManager.ORDER_BY_DATE_ASCENDING -> list.sortedBy { it.edited }
                PreferencesManager.ORDER_BY_DATE_DESCENDING -> list.sortedByDescending { it.edited }
                else -> list.sortedBy { it.label.lowercase() }
            }
        }

        Log.d(TAG, "Passwords filtered and sorted, count: ${filteredList.size}")

        val needsAuth = hasAppLock.first() && (isLocked.firstOrNull() ?: true)

        return buildFillResponse(
            filteredList,
            helper,
            request,
            searchHint,
            needsAuth
        )
    }

    private suspend fun buildFillResponse(
        passwords: List<Password>,
        helper: AssistStructureParser,
        request: FillRequest,
        searchHint: String,
        needsAuth: Boolean
    ): FillResponse {
        Log.d(TAG, "Building FillResponse with ${passwords.size} passwords, needsAuth: $needsAuth")
        val builder = FillResponse.Builder()
        val useInline = preferencesManager.getUseInlineAutofill()
        
        val inlineRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && useInline) {
            request.inlineSuggestionsRequest
        } else null

        // Add one Dataset for each password
        for (password in passwords) {
            builder.addDataset(
                AutofillHelper.buildDataset(
                    applicationContext,
                    PasswordAutofillData(
                        id = password.id,
                        label = "${password.label} - ${password.username}", 
                        username = password.username, 
                        password = password.password
                    ),
                    helper,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) inlineRequest?.inlinePresentationSpecs?.first() else null,
                    null,
                    needsAuth
                )
            )
        }

        Log.d(TAG, "Datasets added to FillResponse")

        // Button to create a new password in the app and autofill it
        if (passwords.isEmpty()) {
            val saveData = SaveData(
                label = searchHint,
                username = "",
                password = "",
                url = searchHint
            )
            builder.addDataset(
                    AutofillHelper.buildDataset(
                        applicationContext,
                        PasswordAutofillData(label = "Create new password", id = null, username = null, password = null), // TODO: translation
                        helper,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) inlineRequest?.inlinePresentationSpecs?.first() else null,
                        AutofillHelper.buildIntent(applicationContext, 1003, AutofillData.SaveAutofill(searchHint, saveData, helper.structure)),
                        false
                    )
                )
            }

        Log.d(TAG, "Button to create new password added to FillResponse")

        // Option to conclude the autofill in the app
        builder.addDataset(
            AutofillHelper.buildDataset(
                applicationContext,
                PasswordAutofillData(label = ">", id = null, username = null, password = null), // TODO use icon
                helper,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) inlineRequest?.inlinePresentationSpecs?.first() else null,
                AutofillHelper.buildIntent(applicationContext, 1004, AutofillData.ChoosePwd(searchHint, helper.structure)),
                false
            )
        )

        Log.d(TAG, "Button to open app added to FillResponse")
        
        // set Save Info, with an optional bundle if delaying the save
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            AutofillHelper.buildSaveInfo(helper)?.let { pair ->
                builder.setSaveInfo(pair.first) 
                pair.second?.let { bundle ->
                    builder.setClientState(bundle)  
                }
            }
        }

        Log.d(TAG, "SaveInfo set in FillResponse if applicable")

        return builder.build()
    }

    private suspend fun getAppLabel(packageName: String): String = withContext(Dispatchers.IO) {
        try {
            val app = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
            else
                packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)

            packageManager.getApplicationLabel(app).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val job = serviceScope.launch {
                try {
                    val intent: IntentSender? = withContext(Dispatchers.Default) {
                        processSaveRequest(request)
                    }
                    if (intent != null) callback.onSuccess(intent)
                    else callback.onFailure("Unable to complete Save Request")
                } catch (e: CancellationException) {
                    throw e 
                } catch (e: Exception) {
                    callback.onFailure("Error handling save request: ${e.message}")
                }
            }
        } else {
            callback.onFailure("Saving not supported on android < 9.0")
        }
    }

    private suspend fun processSaveRequest(request: SaveRequest): IntentSender? {
        val context = request.fillContexts.last() ?: return null
        val helper = AssistStructureParser(context.structure)

        // Do not autofill this application
        if (helper.packageName == packageName) return null

        val delayedUsername: String? = request.clientState?.getCharSequence(AutofillHelper.USERNAME)?.toString()

        val username: String = helper.usernameAutofillContent.firstOrNull { !it.isNullOrBlank() } ?: delayedUsername ?: ""
        val password: String = helper.passwordAutofillContent.firstOrNull { !it.isNullOrBlank() } ?: ""

        if (password.isBlank()) {
            throw Exception("Blank password, cannot save")
        }

        // Check Login Status
        try {
            userController.getServer()
        } catch (_: UserException) {
            throw Exception("User not logged in, cannot save")
        }

        // Ensure Session is open
        if (!apiController.sessionOpen.value && !apiController.openSession(preferencesManager.getMasterPassword())) {
            throw Exception("Session is not open and cannot be opened, cannot save")
        }

        // Determine Search Hint
        val searchHint = helper.webDomain ?: getAppLabel(helper.packageName)

        return AutofillHelper.buildIntent(applicationContext, 1005, AutofillData.Save(searchHint, SaveData(searchHint, username, password, searchHint)))
    }

    companion object {
        const val TAG = "NCPAutofillService"
        private const val TIMEOUT_MS = 2000L
        const val AUTOFILL_DATA = "autofill_data"
    }
}