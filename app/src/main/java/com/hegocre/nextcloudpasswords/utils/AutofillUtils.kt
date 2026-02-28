package com.hegocre.nextcloudpasswords.utils

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
    interface isAutofill {
        val structure: AssistStructure
    }

    interface isSave {
        val saveData: SaveData
    }

    @Parcelize
    data class FromId(
        val id: String, 
        override val structure: AssistStructure
    ) : AutofillData(), isAutofill

    @Parcelize
    data class ChoosePwd(
        val searchHint: String, 
        override val structure: AssistStructure
    ) : AutofillData(), isAutofill

    @Parcelize
    data class SaveAutofill(
        val searchHint: String,
        override val saveData: SaveData,
        override val structure: AssistStructure, 
    ) : AutofillData(), isAutofill, isSave

    @Parcelize
    data class Save(
        val searchHint: String,
        override val saveData: SaveData
    ) : AutofillData(), isSave

    fun isAutofill(): Boolean {
        return when (this) {
            is isAutofill -> true
            else -> false
        }
    }

    fun isSave(): Boolean {
        return when (this) {
            is isSave -> true
            else -> false
        }
    }
}

data class ListDecryptionStateNonNullable<T>(
    val decryptedList: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val notAllDecrypted: Boolean = false
)