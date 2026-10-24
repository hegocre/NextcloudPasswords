package com.hegocre.nextcloudpasswords.utils

import android.webkit.URLUtil

fun String.isValidEmail(): Boolean {
    return this.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isValidURL(): Boolean {
    return this.isBlank() || URLUtil.isValidUrl(this) || URLUtil.isValidUrl("http://${this}")
}