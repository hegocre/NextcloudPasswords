package com.hegocre.nextcloudpasswords.services.autofill

import android.app.assist.AssistStructure
import android.os.Build
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.autofill.AutofillId
import android.view.inputmethod.EditorInfo
import androidx.annotation.RequiresApi
import com.hegocre.nextcloudpasswords.BuildConfig

/**
 * Parser used to get the needed information from an assist structure to reply to an Autofill request.
 *
 * @param assistStructure The assist structure provided by the Autofill Request, containing all autofill
 * fields information
 */
@RequiresApi(Build.VERSION_CODES.O)
class AssistStructureParser(assistStructure: AssistStructure) {
    val usernameAutofillIds = mutableListOf<AutofillId>()
    val passwordAutofillIds = mutableListOf<AutofillId>()
    private var lastTextAutofillId: AutofillId? = null
    private var candidateTextAutofillId: AutofillId? = null

    private val webDomains = HashMap<String, Int>()

    val packageName = assistStructure.activityComponent.flattenToShortString().substringBefore("/")

    // Get the most repeated domain on the fields (there may be more than one)
    val webDomain: String?
        get() = webDomains.toList().filter { it.first != "localhost" }
            .maxByOrNull { (_, value) -> value }?.first

    init {
        for (i in 0 until assistStructure.windowNodeCount) {
            val windowNode = assistStructure.getWindowNodeAt(i)
            windowNode.rootViewNode?.let { parseNode(it) }
        }
        if (usernameAutofillIds.isEmpty())
            candidateTextAutofillId?.let {
                usernameAutofillIds.add(it)
            }
    }

    /**
     * Parse the provided node and its child and classify them by type (username or password).
     *
     * @param node The node to parse.
     */
    private fun parseNode(node: AssistStructure.ViewNode) {
        node.autofillId?.let { autofillId ->
            val fieldType = getFieldType(node)
            if (fieldType != null) {
                when (fieldType) {
                    FIELD_TYPE_USERNAME -> {
                        usernameAutofillIds.add(autofillId)
                    }
                    FIELD_TYPE_PASSWORD -> {
                        passwordAutofillIds.add(autofillId)
                        // We save the autofillId of the field above the password field,
                        // in case we don't find any explicit username field
                        candidateTextAutofillId = lastTextAutofillId
                    }
                    FIELD_TYPE_TEXT -> {
                        lastTextAutofillId = autofillId
                    }
                }
            }
        }

        node.webDomain?.let { webDomain ->
            webDomains[webDomain] = webDomains.getOrDefault(webDomain, 0) + 1
        }

        // Parse child
        for (i in 0 until node.childCount) {
            val windowNode = node.getChildAt(i)
            parseNode(windowNode)
        }
    }

    /**
     * Try to determine the type of the node. First, getting a provided autofill hint is tried. If not present,
     * html attributes are checked. If also not provided, the text type flag is checked.
     *
     * @param node The node that has to be classified.
     * @return The determined field type.
     */
    private fun getFieldType(node: AssistStructure.ViewNode): Int? {
        if (node.autofillType == View.AUTOFILL_TYPE_TEXT) {
            if (BuildConfig.DEBUG) {
                Log.d(packageName, "Autofill node -> ${node.hint}")
                Log.d(
                    packageName,
                    "-- Hints: ${node.autofillHints?.joinToString(", ")}"
                )
                Log.d(
                    packageName,
                    "-- HTML Attributes: ${node.htmlInfo?.attributes?.joinToString(", ")}"
                )
                Log.d(packageName, "-- Field type: ${node.inputType}")
            }

            // Get by autofill hint
            node.autofillHints?.forEach { hint ->
                if (hint == View.AUTOFILL_HINT_USERNAME || hint == View.AUTOFILL_HINT_EMAIL_ADDRESS) {
                    return FIELD_TYPE_USERNAME
                } else if (hint == View.AUTOFILL_HINT_PASSWORD) {
                    return FIELD_TYPE_PASSWORD
                }
            }

            // Get by HTML attributes
            if (node.hasAttribute("type", "email") ||
                node.hasAttribute("type", "tel") ||
                node.hasAttribute("type", "text") ||
                node.hasAttribute("name", "email") ||
                node.hasAttribute("name", "mail") ||
                node.hasAttribute("name", "user") ||
                node.hasAttribute("name", "username")
            ) {
                return FIELD_TYPE_USERNAME
            }
            if (node.hasAttribute("type", "password")) {
                return FIELD_TYPE_PASSWORD
            }


            if (node.hint?.lowercase()?.contains("user") == true ||
                node.hint?.lowercase()?.contains("mail") == true
            ) {
                return FIELD_TYPE_USERNAME
            }

            // Get by field type
            if (node.inputType.isPasswordType()) {
                return FIELD_TYPE_PASSWORD
            }

            if (node.inputType.isTextType()) {
                return FIELD_TYPE_TEXT
            }
        }
        return null
    }

    /**
     * Check if the view node contains a specific HTML attribute value.
     *
     * @param attr The attribute to check.
     * @param value The value to compare.
     * @return Whether the value of the provided attribute matches the provided value.
     */
    private fun AssistStructure.ViewNode?.hasAttribute(attr: String, value: String): Boolean =
        this?.htmlInfo?.attributes?.firstOrNull { it.first == attr && it.second == value } != null

    /**
     * Check if a text field matches the [InputType.TYPE_CLASS_TEXT] input type.
     *
     * @return Whether the field matches the input type.
     */
    private fun Int?.isTextType() = this != null && (this and InputType.TYPE_CLASS_TEXT != 0)

    /**
     * Check if a text field is any type of password field.
     *
     * @return Whether the field matches a password type.
     */
    private fun Int?.isPasswordType() = this != null &&
            (isPasswordInputType(this) || isVisiblePasswordInputType(this))

    //Methods extracted from android source, used since API 33 to heuristically provide
    // the AUTOFILL_HINT_PASSWORD_AUTO hint
    // https://android.googlesource.com/platform/frameworks/base/+/1f5c147eb5959a7e4fd03b751679cb5e00984c9c%5E%21/#F0
    private fun isPasswordInputType(inputType: Int): Boolean {
        val variation = inputType and (EditorInfo.TYPE_MASK_CLASS or EditorInfo.TYPE_MASK_VARIATION)
        return variation == EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
                || variation == EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD
                || variation == EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD
    }

    private fun isVisiblePasswordInputType(inputType: Int): Boolean {
        val variation = inputType and (EditorInfo.TYPE_MASK_CLASS or EditorInfo.TYPE_MASK_VARIATION)
        return variation == EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    }

    companion object {
        private const val FIELD_TYPE_USERNAME = 0
        private const val FIELD_TYPE_PASSWORD = 1
        private const val FIELD_TYPE_TEXT = 2
    }
}