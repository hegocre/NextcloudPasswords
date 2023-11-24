package com.hegocre.nextcloudpasswords.utils

import com.hegocre.nextcloudpasswords.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class LogHelper private constructor() {
    private val _appLog = StringBuilder("")
    val appLog: String
        get() = _appLog.toString()

    init {
        if (BuildConfig.DEBUG) {
            CoroutineScope(Dispatchers.IO).launch {
                Runtime.getRuntime().exec("logcat -c")
                Runtime.getRuntime().exec("logcat")
                    .inputStream
                    .bufferedReader()
                    .useLines { lines ->
                        lines.forEach { line ->
                            ensureActive()
                            _appLog.append("$line\n")
                        }
                    }
            }
        }
    }

    companion object {
        private var instance: LogHelper? = null

        fun getInstance(): LogHelper {
            synchronized(this) {
                if (instance == null) instance = LogHelper()

                return instance as LogHelper
            }
        }
    }
}