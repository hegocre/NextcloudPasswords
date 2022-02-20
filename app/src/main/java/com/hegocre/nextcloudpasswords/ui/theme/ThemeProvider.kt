package com.hegocre.nextcloudpasswords.ui.theme

import android.content.Context
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ThemeProvider private constructor(context: Context) {
    private val preferencesManager = PreferencesManager.getInstance(context).apply {
        CoroutineScope(Dispatchers.IO).launch {
            getUserTheme().collect { themeName ->
                _currentTheme.emit(NCPTheme.fromTitle(themeName))
            }
        }
    }

    private val _currentTheme = MutableStateFlow(NCPTheme.System)
    val currentTheme: StateFlow<NCPTheme>
        get() = _currentTheme

    fun setUserTheme(name: String) {
        CoroutineScope(Dispatchers.IO).launch {
            preferencesManager.setUserTheme(name)
        }
    }

    companion object {
        private var instance: ThemeProvider? = null

        fun getInstance(context: Context): ThemeProvider {
            synchronized(this) {
                var tempInstance = instance

                if (tempInstance == null) {
                    tempInstance = ThemeProvider(context)
                    instance = tempInstance
                }

                return tempInstance
            }
        }
    }
}