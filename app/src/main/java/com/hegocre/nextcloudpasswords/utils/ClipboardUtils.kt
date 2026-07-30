package com.hegocre.nextcloudpasswords.utils

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle

fun Context.copyToClipboard(value: String, isSensitive: Boolean = false) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(if (isSensitive) "password" else "text/plain",
        value)
    clip.description.extras = PersistableBundle().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, isSensitive)
        else
            putBoolean("android.content.extra.IS_SENSITIVE", isSensitive)
    }
    clipboard.setPrimaryClip(clip)
}