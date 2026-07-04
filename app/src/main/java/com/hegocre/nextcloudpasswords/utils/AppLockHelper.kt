package com.hegocre.nextcloudpasswords.utils

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppLockHelper private constructor(context: Context) {
    private val preferencesManager = PreferencesManager.getInstance(context)

    private var _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean>
        get() = _isLocked.asStateFlow()

    fun checkPasscode(passcode: String): Deferred<Boolean> {
        return CoroutineScope(Dispatchers.Default).async {
            // If the stored passcode cannot be read, never accept any input instead
            // of silently falling back to a default passcode
            val correctPasscode = preferencesManager.getAppLockPasscode()
                ?: return@async false
            passcode == correctPasscode
        }
    }

    fun getPasscodeLength(): Deferred<Int?> {
        return CoroutineScope(Dispatchers.Default).async {
            preferencesManager.getAppLockPasscode()?.length
        }
    }

    fun disableLock() {
        CoroutineScope(Dispatchers.Default).launch {
            _isLocked.emit(false)
        }
    }

    fun enableLock() {
        CoroutineScope(Dispatchers.Default).launch {
            _isLocked.emit(true)
        }
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

        /**
         * Decides whether an incorrect passcode [input] should be rejected with
         * visible feedback (and the input cleared). Because the passcode dialog
         * has no submit button, an attempt is only considered complete once it
         * reaches the length of the stored passcode.
         *
         * @param correctPasscodeLength length of the stored passcode, or `null`
         * when it cannot be read. In the latter case any non-empty [input] is
         * rejected, as the passcode can never be verified.
         */
        fun shouldRejectPasscodeAttempt(input: String, correctPasscodeLength: Int?): Boolean {
            if (input.isEmpty()) return false
            return correctPasscodeLength == null || input.length >= correctPasscodeLength
        }
    }
}