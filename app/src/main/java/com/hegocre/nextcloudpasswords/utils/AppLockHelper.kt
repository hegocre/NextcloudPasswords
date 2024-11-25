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
            val correctPasscode = preferencesManager.getAppLockPasscode() ?: "0000"
            passcode == correctPasscode
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
    }
}