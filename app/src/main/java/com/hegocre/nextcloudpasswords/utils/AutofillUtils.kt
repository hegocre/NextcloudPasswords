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
) : Parcelable

sealed class AutofillData : Parcelable {
    interface IsAutofill {
        val structures: List<AssistStructure>
    }

    interface IsSave {
        val saveData: SaveData
    }

    @Parcelize
    data class FromId(
        val id: String, 
        override val structures: List<AssistStructure>
    ) : AutofillData(), IsAutofill

    @Parcelize
    data class ChoosePwd(
        val searchHint: String, 
        override val structures: List<AssistStructure>
    ) : AutofillData(), IsAutofill

    @Parcelize
    data class SaveAutofill(
        val searchHint: String,
        override val saveData: SaveData,
        override val structures: List<AssistStructure>, 
    ) : AutofillData(), IsAutofill, IsSave

    @Parcelize
    data class Save(
        val searchHint: String,
        override val saveData: SaveData
    ) : AutofillData(), IsSave

    fun isAutofill(): Boolean {
        return when (this) {
            is IsAutofill -> true
            else -> false
        }
    }

    fun isSave(): Boolean {
        return when (this) {
            is IsSave -> true
            else -> false
        }
    }
}

data class ListDecryptionStateNonNullable<T>(
    val decryptedList: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val notAllDecrypted: Boolean = false
)