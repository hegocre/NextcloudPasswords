package com.hegocre.nextcloudpasswords.utils

import android.app.assist.AssistStructure
import android.os.Build
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.autofill.AutofillId
import androidx.annotation.RequiresApi

/**
 * Parser used to get the needed information from an assist structure to reply to an Autofill request.
 *
 * @param assistStructure The assist structure provided by the Autofill Request, containing all autofill
 * fields information
 */
@RequiresApi(Build.VERSION_CODES.O)
class AssistStructureParser(assistStructure: AssistStructure) {
    val nodes = mutableListOf<AssistStructure.ViewNode>()

    val allAutofillIds = mutableListOf<AutofillId>()
    val usernameAutofillIds = mutableListOf<AutofillId>()
    val passwordAutofillIds = mutableListOf<AutofillId>()

    private val webDomains = HashMap<String, Int>()

    val packageName = assistStructure.activityComponent.flattenToShortString().substringBefore("/")

    // Get the most repeated domain on the fields (there may be more than one)
    val webDomain: String?
        get() = webDomains.toList().maxByOrNull { (_, value) -> value }?.first

    init {
        for (i in 0 until assistStructure.windowNodeCount) {
            val windowNode = assistStructure.getWindowNodeAt(i)
            windowNode.rootViewNode?.let { parseNode(it) }
        }
    }

    /**
     * Parse the provided node and its child and classify them by type (username or password).
     *
     * @param node The node to parse.
     */
    private fun parseNode(node: AssistStructure.ViewNode) {
        nodes.add(node)

        node.autofillId?.let { autofillId ->
            allAutofillIds.add(autofillId)
            val fieldType = getFieldType(node)
            if (fieldType != null) {
                if (fieldType == FIELD_TYPE_USERNAME)
                    usernameAutofillIds.add(autofillId)
                else if (fieldType == FIELD_TYPE_PASSWORD)
                    passwordAutofillIds.add(autofillId)
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
        //Determine field type, first by autofill hint. If there are no hints, try with html attributes.
        //If no html, try with input type (may sometimes work wrong)
        if (node.autofillType == View.AUTOFILL_TYPE_TEXT) {
            node.autofillHints?.forEach { hint ->
                if (hint == View.AUTOFILL_HINT_USERNAME || hint == View.AUTOFILL_HINT_EMAIL_ADDRESS) {
                    Log.d("AUTOFILL", "Found autofill hint username")
                    return FIELD_TYPE_USERNAME
                } else if (hint == View.AUTOFILL_HINT_PASSWORD) {
                    Log.d("AUTOFILL", "Found autofill hint password")
                    return FIELD_TYPE_PASSWORD
                }
            }
            if (node.hasAttribute("type", "mail") ||
                node.hasAttribute("name", "mail") ||
                node.hasAttribute("name", "user") ||
                node.hasAttribute("name", "username")
            ) {
                Log.d("AUTOFILL", "Found autofill attribute username")
                return FIELD_TYPE_USERNAME
            }
            if (node.hasAttribute("type", "password")) {
                Log.d("AUTOFILL", "Found autofill attribute password")
                return FIELD_TYPE_PASSWORD
            }
            if (node.hint?.lowercase()?.contains("user") == true ||
                node.hint?.lowercase()?.contains("mail") == true
            ) {
                Log.d("AUTOFILL", "Found autofill field username")
                return FIELD_TYPE_USERNAME
            }
            Log.d("AUTOFILL", "Input type = ${node.inputType}")
            if (node.inputType.isTextType(InputType.TYPE_TEXT_VARIATION_PASSWORD) ||
                node.inputType.isTextType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) ||
                node.inputType.isTextType(InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
            ) {
                Log.d("AUTOFILL", "Found autofill field password")
                return FIELD_TYPE_PASSWORD
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
     * Check if a text field matches the provided flag. Sometimes, the [InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS]
     * or the [InputType.TYPE_CLASS_TEXT] are included.
     *
     * @param flag The flag to compare.
     * @return Whether the field matches the flag.
     */
    private fun Int?.isTextType(flag: Int) = this != null && (
            flag == this || flag or InputType.TYPE_CLASS_TEXT == this ||
                    flag or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS == this ||
                    flag or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_CLASS_TEXT == this)

    companion object {
        private const val FIELD_TYPE_USERNAME = 0
        private const val FIELD_TYPE_PASSWORD = 1
    }
}