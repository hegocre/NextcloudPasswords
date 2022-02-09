package com.hegocre.nextcloudpasswords.utils

sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val code: Int) : Result<Nothing>()
}