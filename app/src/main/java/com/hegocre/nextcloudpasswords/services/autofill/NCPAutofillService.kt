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

@TargetApi(Build.VERSION_CODES.O)
class NCPAutofillService : AutofillService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private val preferencesManager by lazy { PreferencesManager.getInstance(applicationContext) }
    private val apiController by lazy { ApiController.getInstance(applicationContext) }
    private val passwordController by lazy { PasswordController.getInstance(applicationContext) }
    private val userController by lazy { UserController.getInstance(applicationContext) }

    private val hasAppLock by lazy { preferencesManager.getHasAppLock() }

    val orderBy by lazy { preferencesManager.getOrderBy() }
    val searchByUsername by lazy { preferencesManager.getSearchByUsername() }
    val strictUrlMatching by lazy { preferencesManager.getUseStrictUrlMatching() }

    private lateinit var decryptedPasswordsState: StateFlow<List<Password>>

    override fun onCreate() {
        super.onCreate()
        decryptedPasswordsState = combine(
            passwordController.getPasswords().asFlow(),
            apiController.csEv1Keychain.asFlow()
        ) { passwords, keychain ->
            passwords.filter { !it.trashed && !it.hidden }.decryptPasswords(keychain)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = serviceScope, 
            started = SharingStarted.Eagerly, 
            initialValue = emptyList()
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
                callback.onSuccess(response)
            } catch (e: CancellationException) {
                throw e 
            } catch (e: Exception) {
                Log.e(TAG, "Error handling fill request ${e.message}")
                callback.onSuccess(null)
            }
        }

        cancellationSignal.setOnCancelListener {
            job.cancel()
        }
    }

    private suspend fun processFillRequest(request: FillRequest): FillResponse? {
        val context = request.fillContexts.last() ?: return null
        val helper = AssistStructureParser(context.structure)

        // Do not autofill this application
        if (helper.packageName == packageName) return null

        if (helper.usernameAutofillIds.isEmpty() && helper.passwordAutofillIds.isEmpty()) {
            return null
        }

        // Check Login Status
        try {
            userController.getServer()
        } catch (_: UserException) {
            Log.e(TAG, "User not logged in, cannot autofill")
            return null
        }

        // Try to open Session
        if (!apiController.sessionOpen.value) {
            if (!apiController.openSession(preferencesManager.getMasterPassword())) {
                Log.w(TAG, "Session is not open and cannot be opened")
            }
        }

        if (apiController.sessionOpen.value) {
            passwordController.syncPasswords()
        }

        // Determine Search Hint
        val searchHint = helper.webDomain ?: getAppLabel(helper.packageName)

        // wait for passwords to be decrypted, then filter by search hint and sort them
        val filteredList = decryptedPasswordsState.value.filter {
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

        return buildFillResponse(
            filteredList,
            helper,
            request,
            searchHint
        )
    }

    private suspend fun buildFillResponse(
        passwords: List<Password>,
        helper: AssistStructureParser,
        request: FillRequest,
        searchHint: String
    ): FillResponse {
        val builder = FillResponse.Builder()
        val useInline = preferencesManager.getUseInlineAutofill()
        
        val inlineRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && useInline) {
            request.inlineSuggestionsRequest
        } else null

        val mainAppIntent = buildMainAppIntent(applicationContext, searchHint)

        val needsAuth = hasAppLock.first()

        // Add one Dataset for each password
        for (password in passwords) {
            builder.addDataset(
                AutofillHelper.buildDataset(
                    applicationContext,
                    Triple("${password.label} - ${password.username}", password.username, password.password),
                    helper,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) inlineRequest?.inlinePresentationSpecs?.first() else null,
                    null,
                    needsAuth
                )
            )
        }

        // Add "Generate Password" option (only if there are no passwords?)
        if (passwords.isEmpty() && apiController.sessionOpen.value) {
            val options = preferencesManager.getPasswordGenerationOptions()?.split(";") ?: listOf()
            apiController.generatePassword(
                options.getOrNull(0)?.toIntOrNull() ?: 4,
                options.getOrNull(1)?.toBooleanStrictOrNull() ?: true,
                options.getOrNull(2)?.toBooleanStrictOrNull() ?: true
            )?.let {
            builder.addDataset(
                    AutofillHelper.buildDataset(
                        applicationContext,
                        Triple("Generate new password", null, it),
                        helper,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) inlineRequest?.inlinePresentationSpecs?.first() else null,
                        null,
                        false
                    )
                )
            }
        }

        // Option to conclude the autofill in the app
        builder.addDataset(
            AutofillHelper.buildDataset(
                applicationContext,
                null,
                helper,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) inlineRequest?.inlinePresentationSpecs?.first() else null,
                mainAppIntent,
                false
            )
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            builder.setSaveInfo(AutofillHelper.buildSaveInfo(helper))
        }

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

    private fun buildMainAppIntent(context: Context, searchHint: String): IntentSender {
        val appIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(AUTOFILL_REQUEST, true)
            putExtra(AUTOFILL_SEARCH_HINT, searchHint)
        }

        val intentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_CANCEL_CURRENT
        }

        return PendingIntent.getActivity(
            context, 1001, appIntent, intentFlags
        ).intentSender
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val job = serviceScope.launch {
            try {
                val response: Boolean = withContext(Dispatchers.Default) {
                    processSaveRequest(request)
                }
                if (response) callback.onSuccess()
                else callback.onFailure("Unable to complete Save Request")
            } catch (e: CancellationException) {
                throw e 
            } catch (e: Exception) {
                callback.onFailure("Error handling save request: ${e.message}")
            }
        }
    }

    private suspend fun processSaveRequest(request: SaveRequest): Boolean {
        val context = request.fillContexts.last() ?: return false
        val helper = AssistStructureParser(context.structure)

        // Do not autofill this application
        if (helper.packageName == packageName) return false

        val username: String = helper.usernameAutofillContent.firstOrNull { !it.isNullOrBlank() } ?: ""
        val password: String = helper.passwordAutofillContent.firstOrNull { !it.isNullOrBlank() } ?: ""

        if (password.isBlank()) {
            val usernameIds = helper.usernameAutofillIds.map { it.toString() }
            val passwordIds = helper.passwordAutofillIds.map { it.toString() }
            throw Exception("Blank password, cannot save")
        }

        // Check Login Status
        try {
            userController.getServer()
        } catch (_: UserException) {
            throw Exception("User not logged in, cannot save")
        }

        // Ensure Session is open
        if (!apiController.sessionOpen.value) {
            if (!apiController.openSession(preferencesManager.getMasterPassword())) {
                throw Exception("Session is not open and cannot be opened, Cannot save")
            }
        }

        // Determine Search Hint
        val searchHint = helper.webDomain ?: getAppLabel(helper.packageName)

        // wait for passwords to be decrypted, then filter by search hint and sort them
        val filteredList = decryptedPasswordsState.value.filter {
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

        if (filteredList.isEmpty()) {
            // prompt to choose label?
            if (!createPassword(searchHint, username, password, searchHint)) {
                throw Exception("Failed to create password")
            }
        } else {
            // should prompt too choose which one(s)
            filteredList.forEach {
                if (!it.equals(username, password, searchHint)) {
                    if (!updatePassword(it, searchHint, username, password, searchHint)) {  
                        throw Exception("Failed to update password")
                    }
                }
            }
        }

        return true
    }

    // check equality ignoring label
    private fun Password.equals(username: String, password: String, url: String): Boolean {
        return (this.username == username) 
                && (this.password == password)
                && (this.url == url)
    }

    private suspend fun createPassword(label: String, username: String, password: String, url: String): Boolean {
        val keychain = apiController.csEv1Keychain.asFlow().first()
        val serverSettings = apiController.serverSettings.asFlow().first()
        
        lateinit var newPassword: NewPassword
        if(keychain != null && serverSettings.encryptionCse != 0) {
            newPassword = NewPassword(
                password = password.encryptValue(keychain.current, keychain),
                label = label.encryptValue(keychain.current, keychain),
                username = username.encryptValue(keychain.current, keychain),
                url = url.encryptValue(keychain.current, keychain),
                notes = "".encryptValue(keychain.current, keychain),
                customFields = "[]".encryptValue(keychain.current, keychain),
                hash = password.sha1Hash(),
                cseType = "CSEv1r1",
                cseKey = keychain.current,
                folder = FoldersApi.DEFAULT_FOLDER_UUID,
                edited = (System.currentTimeMillis() / 1000).toInt(),
                hidden = false,
                favorite = false
            )
        } else {
            newPassword = NewPassword(
                password = password,
                label = label,
                username = username,
                url =  url,
                notes = "",
                customFields = "[]",
                hash = password.sha1Hash(),
                cseType = "none",
                cseKey = "",
                folder = FoldersApi.DEFAULT_FOLDER_UUID,
                edited = (System.currentTimeMillis() / 1000).toInt(),
                hidden = false,
                favorite = false
            )
        }
        return apiController.createPassword(newPassword)
    }

    suspend fun updatePassword(oldPassword: Password, label: String, username: String, password: String, url: String): Boolean {
        val keychain = apiController.csEv1Keychain.asFlow().first()
        val serverSettings = apiController.serverSettings.asFlow().first()

        val _label = oldPassword.label // do not change labels (we are just using searchHint for the label)
        val _username = if (username.isBlank()) oldPassword.username else username
        val _password = if (password.isBlank()) oldPassword.password else password
        val _url = if (url.isBlank()) oldPassword.url else url
        
        lateinit var updatedPassword: UpdatedPassword
        if(keychain != null && serverSettings.encryptionCse != 0) {
            updatedPassword = UpdatedPassword(
                id = oldPassword.id,
                revision = oldPassword.revision,
                password = _password.encryptValue(keychain.current, keychain) ,
                label = _label.encryptValue(keychain.current, keychain) ,
                username = _username.encryptValue(keychain.current, keychain) ,
                url = _url.encryptValue(keychain.current, keychain) ,
                notes = oldPassword.notes.encryptValue(keychain.current, keychain),
                customFields = oldPassword.customFields.encryptValue(keychain.current, keychain),
                hash = password.sha1Hash(),
                cseType = "CSEv1r1",
                cseKey = keychain.current,
                folder = oldPassword.folder,
                edited = (System.currentTimeMillis() / 1000).toInt(),
                hidden = oldPassword.hidden,
                favorite = oldPassword.favorite
            )
        } else {
            updatedPassword = UpdatedPassword(
                id = oldPassword.id,
                revision = oldPassword.revision,
                password = _password,
                label = _label,
                username = _username,
                url = _url,
                notes = oldPassword.notes,
                customFields = oldPassword.customFields,
                hash = password.sha1Hash(),
                cseType = "none",
                cseKey = "",
                folder = oldPassword.folder,
                edited = (System.currentTimeMillis() / 1000).toInt(),
                hidden = oldPassword.hidden,
                favorite = oldPassword.favorite
            )
        }
        return apiController.updatePassword(updatedPassword)
    }

    companion object {
        const val TAG = "NCPAutofillService"
        private const val TIMEOUT_MS = 2000L
        const val AUTOFILL_REQUEST = "autofill_request"
        const val AUTOFILL_SEARCH_HINT = "autofill_query"
        const val SELECTED_DATASET = "selected_dataset"
    }
}