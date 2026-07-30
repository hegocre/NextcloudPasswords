package com.hegocre.nextcloudpasswords.utils

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class AppLockHelper private constructor(context: Context) {
    private val preferencesManager = PreferencesManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var reLockJob: Job? = null

    private var _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean>
        get() = _isLocked.asStateFlow()

    private var lastResetTime = 0L

    fun checkPasscode(passcode: String): Deferred<Boolean> {
        return scope.async(Dispatchers.Default) {
            val correctPasscode = preferencesManager.getAppLockPasscode() ?: return@async false
            passcode == correctPasscode
        }
    }

    fun disableLock() {
        val currentTime = System.currentTimeMillis()
        if (!_isLocked.value && currentTime - lastResetTime < 1000) return
        lastResetTime = currentTime

        reLockJob?.cancel()
        _isLocked.value = false
        reLockJob = scope.launch {
            delay(2.minutes)
            enableLock()
        }
    }

    fun enableLock() {
        reLockJob?.cancel()
        _isLocked.value = true
    }

    companion object {
        private var instance: AppLockHelper? = null

        fun getInstance(context: Context): AppLockHelper {
            synchronized(this) {
                var tempInstance = instance
                if (tempInstance == null) {
                    tempInstance = AppLockHelper(context)
                }
                instance = tempInstance
                return tempInstance
            }
        }
    }
}