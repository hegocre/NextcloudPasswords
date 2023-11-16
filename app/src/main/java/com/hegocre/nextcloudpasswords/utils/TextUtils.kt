package com.hegocre.nextcloudpasswords.utils

import android.webkit.URLUtil
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree

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

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.autofill(
    onFill: (String) -> Unit,
    autofillTypes: List<AutofillType>
) = composed {
    val autofill = LocalAutofill.current
    val autofillNode = AutofillNode(
        onFill = onFill, autofillTypes = autofillTypes
    )
    LocalAutofillTree.current += autofillNode

    this
        .onGloballyPositioned {
            autofillNode.boundingBox = it.boundsInWindow()
        }
        .onFocusChanged { focusState ->
            autofill?.run {
                if (focusState.isFocused) {
                    requestAutofillForNode(autofillNode)
                } else {
                    cancelAutofillForNode(autofillNode)
                }
            }
        }
}