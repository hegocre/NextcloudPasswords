package com.hegocre.nextcloudpasswords.utils

import android.webkit.URLUtil

fun String.isValidEmail(): Boolean {
    return if (this.isBlank()) {
        true
    } else {
        android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
    }
}

fun String.isValidURL(): Boolean {
    return if (this.isBlank()) {
        true
    } else {
        URLUtil.isValidUrl(this) || URLUtil.isValidUrl("http://${this}")
    }
}