package com.hegocre.nextcloudpasswords.utils

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

fun ComponentActivity.enableEdgeToEdgeWithoutNavigationBarScrim() {
    enableEdgeToEdge()
    // The bottom navigation bar already provides contrast, a system scrim over it looks like a glitch
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
}
