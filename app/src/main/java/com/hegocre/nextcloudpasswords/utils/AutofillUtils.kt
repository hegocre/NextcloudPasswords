package com.hegocre.nextcloudpasswords.utils

import android.os.Parcel
import android.os.Parcelable
import android.app.assist.AssistStructure
import kotlinx.parcelize.Parcelize

data class PasswordAutofillData(val id: String?, val label: String, val username: String?, val password: String?)   

@Parcelize
data class SaveData(
    val label: String,
    val username: String,
    val password: String,
    val url: String,
) : Parcelable {}

sealed class AutofillData : Parcelable {
    @Parcelize
    data class FromId(
        val id: String, 
        val structure: AssistStructure
    ) : AutofillData()

    @Parcelize
    data class ChoosePwd(
        val searchHint: String, 
        val structure: AssistStructure
    ) : AutofillData()

    @Parcelize
    data class SaveAutofill(
        val searchHint: String,
        val saveData: SaveData,
        val structure: AssistStructure, 
    ) : AutofillData()

    @Parcelize
    data class Save(
        val searchHint: String,
        val saveData: SaveData
    ) : AutofillData()

    fun isAutofill(): Boolean {
        return when (this) {
            is FromId -> true
            is ChoosePwd -> true
            is SaveAutofill -> true
            is Save -> false
        }
    }

    fun isSave(): Boolean {
        return when (this) {
            is SaveAutofill -> true
            is Save -> true
            else -> false
        }
    }
}