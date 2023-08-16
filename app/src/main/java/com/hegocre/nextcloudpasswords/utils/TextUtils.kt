package com.hegocre.nextcloudpasswords.utils

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
        android.util.Patterns.WEB_URL.matcher(this).matches()
    }
}