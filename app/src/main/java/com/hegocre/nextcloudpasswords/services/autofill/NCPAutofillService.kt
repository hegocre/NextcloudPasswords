package com.hegocre.nextcloudpasswords.services.autofill

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.util.Log
import android.annotation.TargetApi
import androidx.lifecycle.asFlow
import com.hegocre.nextcloudpasswords.api.ApiController
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.data.password.PasswordController
import com.hegocre.nextcloudpasswords.data.user.UserController
import com.hegocre.nextcloudpasswords.data.user.UserException
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import com.hegocre.nextcloudpasswords.utils.decryptPasswords
import com.hegocre.nextcloudpasswords.utils.AppLockHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.CancellationException
import android.content.IntentSender
import com.hegocre.nextcloudpasswords.utils.AutofillData
import com.hegocre.nextcloudpasswords.utils.PasswordAutofillData
import com.hegocre.nextcloudpasswords.utils.SaveData
import com.hegocre.nextcloudpasswords.utils.ListDecryptionStateNonNullable
import com.hegocre.nextcloudpasswords.R

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
    val strictUrlMatching by lazy { preferencesManager.getUseStrictUrlMatching() }

    private lateinit var decryptedPasswordsState: StateFlow<ListDecryptionStateNonNullable<Password>>

    private var loginException: Throwable? = null
    
    override fun onCreate() {
        super.onCreate()

        try {
            decryptedPasswordsState = combine(
                passwordController.getPasswords().asFlow(),
                apiController.csEv1Keychain.asFlow()
            ) { passwords, keychain ->
                passwords.decryptPasswords(keychain).let { decryptedPasswords ->
                    ListDecryptionStateNonNullable(decryptedPasswords, false, decryptedPasswords.size < passwords.size)
                }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = serviceScope, 
                started = SharingStarted.Lazily, 
                initialValue = ListDecryptionStateNonNullable(isLoading = true)
            )
        } catch(e: Throwable) {
            loginException = e
            decryptedPasswordsState = MutableStateFlow(ListDecryptionStateNonNullable(isLoading = false))
        }
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
            } catch (e: Throwable) {
                callback.onFailure("Error handling fill request: ${e.message}")
            }
        }

        cancellationSignal.setOnCancelListener {
            job.cancel()
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            serviceScope.launch {
                try {
                    val intent: IntentSender? = withContext(Dispatchers.Default) {
                        processSaveRequest(request)
                    }
                    if (intent != null) callback.onSuccess(intent)
                    else callback.onFailure("Unable to complete Save Request")
                } catch (e: CancellationException) {
                    throw e 
                } catch (e: Throwable) {
                    callback.onFailure("Error handling save request: ${e.message}")
                }
            }
        } else {
            callback.onFailure("Saving not supported on android < 9.0")
        }
    }

    private suspend fun processFillRequest(request: FillRequest): FillResponse? {
        loginException?.let { 
            throw it 
        }

        Log.d(TAG, "Processing fill request with ${request.fillContexts.size} contexts")
        if (request.fillContexts.isEmpty())
            return null

        val helper = AssistStructureParser(listOf(request.fillContexts.last().structure))
        val delayed_helper = AssistStructureParser(request.fillContexts.map { it.structure })

        // Do not autofill this application
        if (helper.packageName == packageName) return null

        if (helper.usernameAutofillData.isEmpty() && helper.passwordAutofillData.isEmpty()) {
            Log.e(TAG, "No username or password fields detected, cannot autofill")
            return null
        }

        // Check Login Status
        try {
            userController.getServer()
        } catch (_: UserException) {
            throw IllegalStateException("User not logged in, cannot autofill")
        }

        // Determine Search Hint
        val searchHint = helper.webDomain ?: getAppLabel(helper.packageName)

        Log.d(TAG, "Search hint determined: $searchHint")

        // wait for passwords to be decrypted, then filter by search hint and sort them
        val currentState = decryptedPasswordsState.first { !it.isLoading }

        val filteredList = currentState.decryptedList.filter {
            !it.hidden && !it.trashed && it.matches(searchHint, strictUrlMatching.first())
        }.let { list ->
            when (orderBy.first()) {
                PreferencesManager.ORDER_BY_TITLE_DESCENDING -> list.sortedByDescending { it.label.lowercase() }
                PreferencesManager.ORDER_BY_DATE_ASCENDING -> list.sortedBy { it.edited }
                PreferencesManager.ORDER_BY_DATE_DESCENDING -> list.sortedByDescending { it.edited }
                else -> list.sortedBy { it.label.lowercase() }
            }
        }

        // must go to the main app only if there are no passwords to show, and some were not decrypted
        val needsAppForMasterPassword = if (filteredList.isEmpty()) currentState.notAllDecrypted
                                        else false

        Log.d(TAG, "Passwords filtered and sorted")

        val needsAuth = hasAppLock.first() && isLocked.value

        // use username from any of the past contexts as candidate for saving
        val candidateUsername = delayed_helper.usernameAutofillContent.firstOrNull { !it.isNullOrBlank() }

        return buildFillResponse(
            filteredList,
            helper,
            request,
            searchHint,
            needsAuth,
            needsAppForMasterPassword,
            candidateUsername 
        )
    }

    private suspend fun buildFillResponse(
        passwords: List<Password>,
        helper: AssistStructureParser,
        request: FillRequest,
        searchHint: String,
        needsAuth: Boolean,
        needsAppForMasterPassword: Boolean,
        candidateUsername: String?
    ): FillResponse {
        Log.d(TAG, "Building FillResponse, needsAuth: $needsAuth")
        val builder = FillResponse.Builder()
        val useInline = preferencesManager.getUseInlineAutofill()
        
        val inlinePresentationSpec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && useInline) {
            request.inlineSuggestionsRequest?.inlinePresentationSpecs?.first()
        } else null

        if (!needsAppForMasterPassword) {
            // Add one Dataset for each password
            for ((idx, password) in passwords.withIndex()) {
                builder.addDataset(
                    AutofillHelper.buildDataset(
                        applicationContext,
                        helper,
                        inlinePresentationSpec,
                        PasswordAutofillData(
                            id = password.id,
                            label = "${password.label} - ${password.username}", 
                            username = password.username, 
                            password = password.password
                        ),
                        null,
                        needsAuth,
                        idx
                    )
                )
            }

            Log.d(TAG, "Using Inline suggestions: ${inlinePresentationSpec != null}")

            Log.d(TAG, "Datasets added to FillResponse")

            // Button to create a new password in the app and autofill it
            if (passwords.isEmpty()) {
                val saveData = SaveData(
                    label = searchHint,
                    username = candidateUsername ?: "",
                    password = "",
                    url = searchHint
                )
                builder.addDataset(
                        AutofillHelper.buildDataset(
                            applicationContext,
                            helper,
                            inlinePresentationSpec,
                            PasswordAutofillData(
                                label = applicationContext.getString(R.string.new_password), 
                                id = null, 
                                username = null, 
                                password = null
                            ),
                            AutofillHelper.buildIntent(
                                applicationContext, 
                                1002, 
                                AutofillData.SaveAutofill(searchHint, saveData, helper.structures)
                            ),
                            false
                        )
                    )
                }

            Log.d(TAG, "Button to create new password added to FillResponse")
        }

        // Option to conclude the autofill in the app
        builder.addDataset(
            AutofillHelper.buildDataset(
                applicationContext,
                helper,
                inlinePresentationSpec,
                PasswordAutofillData(
                    label = applicationContext.getString(R.string.more), 
                    id = null, 
                    username = null, 
                    password = null
                ),
                AutofillHelper.buildIntent(
                    applicationContext, 
                    1003, 
                    AutofillData.ChoosePwd(searchHint, helper.structures)
                ),
                false
            )
        )

        Log.d(TAG, "Button to conclude in app added to FillResponse")
        
        // set Save Info, with an optional bundle if delaying the save
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            AutofillHelper.buildSaveInfo(helper, searchHint)?.let { pair ->
                builder.setSaveInfo(pair.first) 
                pair.second?.let { bundle ->
                    builder.setClientState(bundle)
                    Log.d(TAG, "SaveInfo set in FillResponse, delaying the save")
                } ?: Log.d(TAG, "SaveInfo set in FillResponse")
            }
        }

        return builder.build()
    }

    private suspend fun processSaveRequest(request: SaveRequest): IntentSender? {
        Log.d(TAG, "Processing save request with ${request.fillContexts.size} contexts")

        if (request.fillContexts.isEmpty())
            return null
            
        val helper = AssistStructureParser(listOf(request.fillContexts.last().structure))
        val delayed_helper = AssistStructureParser(request.fillContexts.map { it.structure })

        // Do not autofill this application
        if (helper.packageName == packageName) return null

        // Determine Search Hint
        val searchHint = helper.webDomain ?: getAppLabel(helper.packageName)

        // Get the most recent username and password among the past contexts
        val username: String = delayed_helper.usernameAutofillContent.firstOrNull { !it.isNullOrBlank() } ?: ""
        val password: String = delayed_helper.passwordAutofillContent.firstOrNull { !it.isNullOrBlank() } ?: ""
        
        if (password.isBlank()) {
            throw IllegalArgumentException("Blank password, cannot save")
        }

        // Check Login Status
        try {
            userController.getServer()
        } catch (_: UserException) {
            throw IllegalStateException("User not logged in, cannot save")
        }

        // Ensure Session is open
        if (!apiController.sessionOpen.value && !apiController.openSession(preferencesManager.getMasterPassword())) {
            throw IllegalStateException("Session is not open and cannot be opened, cannot save")
        }

        return AutofillHelper.buildIntent(
            applicationContext, 
            1004, 
            AutofillData.Save(searchHint, SaveData(searchHint, username, password, searchHint))
        )
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

    companion object {
        const val TAG = "NCPAutofillService"
        const val AUTOFILL_DATA = "autofill_data"
    }
}