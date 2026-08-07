package com.hegocre.nextcloudpasswords.services.autofill

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.BlendMode
import android.graphics.drawable.Icon
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.service.autofill.InlinePresentation
import android.service.autofill.Presentations
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.autofill.inline.v1.InlineSuggestionUi
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.activities.MainActivity
import android.service.autofill.SaveInfo
import android.os.Bundle
import android.util.Log
import com.hegocre.nextcloudpasswords.utils.AutofillData
import com.hegocre.nextcloudpasswords.utils.PasswordAutofillData

@RequiresApi(Build.VERSION_CODES.O)
object AutofillHelper {
    fun buildDataset(
        context: Context,
        helper: AssistStructureParser,
        inlinePresentationSpec: InlinePresentationSpec?,
        password: PasswordAutofillData?,
        intent: IntentSender?,
        needsAppLock: Boolean,
        intentIdx: Int = 0
    ): Dataset {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (inlinePresentationSpec != null) {
                buildInlineDataset(
                    context,
                    helper,
                    inlinePresentationSpec,
                    password,
                    intent,
                    needsAppLock,
                    intentIdx
                )
            } else {
                buildPresentationDataset(
                    context, 
                    helper, 
                    password, 
                    intent, 
                    needsAppLock, 
                    intentIdx
                )
            }
        } else {
            buildPresentationDataset(
                context, 
                helper, 
                password, 
                intent, 
                needsAppLock, 
                intentIdx
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun buildSaveInfo(helper: AssistStructureParser, searchHint: String): Pair<SaveInfo, Bundle?>? {
        val requiredIds = mutableListOf<AutofillId>()
        val optionalIds = mutableListOf<AutofillId>()

        Log.d(NCPAutofillService.TAG, "Building SaveInfo, usernameAutofillIds: ${helper.usernameAutofillIds}, passwordAutofillIds: ${helper.passwordAutofillIds}")
        
        if (helper.passwordAutofillIds.size == 1) requiredIds += helper.passwordAutofillIds[0]
        else optionalIds += helper.passwordAutofillIds
        
        if (helper.usernameAutofillIds.size == 1) requiredIds += helper.usernameAutofillIds[0]
        else optionalIds += helper.usernameAutofillIds

        Log.d(NCPAutofillService.TAG, "Required IDs: $requiredIds, Optional IDs: $optionalIds")

        val type = if (helper.usernameAutofillIds.isNotEmpty()) {
            SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD
        } else {
            SaveInfo.SAVE_DATA_TYPE_PASSWORD
        } 

        val builder = if (requiredIds.isNotEmpty()) {
            SaveInfo.Builder(type, requiredIds.toTypedArray())
        } else {
            SaveInfo.Builder(type)
        }

        // if there are only username views but no password views, then delay the save on supported devices
        if(helper.usernameAutofillIds.isNotEmpty() && helper.passwordAutofillIds.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return Pair(
                builder.apply {
                    setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE or SaveInfo.FLAG_DELAY_SAVE)
                    if (optionalIds.isNotEmpty()) setOptionalIds(optionalIds.toTypedArray())
                }.build(),
                Bundle().apply {
                    putCharSequence(SEARCH_HINT, searchHint)
                }
            )
        } else if (helper.passwordAutofillIds.isNotEmpty()) {
            return Pair(
                builder.apply {
                    setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
                    if (optionalIds.isNotEmpty()) setOptionalIds(optionalIds.toTypedArray())
                }.build(), 
                null
            )
        } else {
            // if not delaying save and no password views, do not save
            return null
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun buildInlineDataset(
        context: Context,
        helper: AssistStructureParser,
        inlinePresentationSpec: InlinePresentationSpec,
        password: PasswordAutofillData?,
        intent: IntentSender? = null,
        needsAppLock: Boolean = false,
        intentIdx: Int
    ): Dataset {
        // build redacted dataset when app lock is needed
        return if (needsAppLock && password?.id != null) {
            Dataset.Builder().apply {
                helper.usernameAutofillIds.forEach { autofillId ->
                    addInlineAutofillValue(
                        context,
                        autofillId,
                        inlinePresentationSpec,
                        password.label,
                        null
                    )
                }
                helper.passwordAutofillIds.forEach { autofillId ->
                    addInlineAutofillValue(
                        context,
                        autofillId,
                        inlinePresentationSpec,
                        password.label,
                        null
                    )
                }
                setAuthentication(buildIntent(
                    context, 
                    1005+intentIdx, 
                    AutofillData.FromId(id=password.id, structures=helper.structures)
                ))
            }.build()
        } else {
            Dataset.Builder().apply {
                helper.usernameAutofillIds.forEach { autofillId ->
                    addInlineAutofillValue(
                        context,
                        autofillId,
                        inlinePresentationSpec,
                        password?.label,
                        password?.username
                    )
                }
                helper.passwordAutofillIds.forEach { autofillId ->
                    addInlineAutofillValue(
                        context,
                        autofillId,
                        inlinePresentationSpec,
                        password?.label,
                        password?.password
                    )
                }
                intent?.let { setAuthentication(it) }
            }.build()
        }
    }

    private fun buildPresentationDataset(
        context: Context,
        helper: AssistStructureParser,
        password: PasswordAutofillData?,
        intent: IntentSender? = null,
        needsAppLock: Boolean = false,
        intentIdx: Int
    ): Dataset {
        // build redacted dataset when app lock is needed
        return if (needsAppLock && password?.id != null) {
            Dataset.Builder().apply {
                helper.usernameAutofillIds.forEach { autofillId ->
                    addAutofillValue(context, autofillId, password.label, null)
                }
                helper.passwordAutofillIds.forEach { autofillId ->
                    addAutofillValue(context, autofillId, password.label, null)
                }
                setAuthentication(buildIntent(
                    context, 
                    1005+intentIdx, 
                    AutofillData.FromId(id=password.id, structures=helper.structures)
                ))
            }.build()
        } else {
            Dataset.Builder().apply {
                helper.usernameAutofillIds.forEach { autofillId ->
                    addAutofillValue(context, autofillId, password?.label, password?.username)
                }
                helper.passwordAutofillIds.forEach { autofillId ->
                    addAutofillValue(context, autofillId, password?.label, password?.password)
                }
                intent?.let { setAuthentication(it) }
            }.build()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun Dataset.Builder.addAutofillValue(
        context: Context,
        autofillId: AutofillId,
        label: String?,
        value: String?,
    ) {
        val autofillLabel = label ?: context.getString(R.string.app_name)

        val presentation = if (label == null) {
            RemoteViews(context.packageName, R.layout.password_list_item).apply {
                setTextViewText(R.id.text, autofillLabel)
            }
        } else {
            RemoteViews(context.packageName, android.R.layout.simple_list_item_1).apply {
                setTextViewText(android.R.id.text1, autofillLabel)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val fieldBuilder = Field.Builder()
            value?.let {
                fieldBuilder.setValue(AutofillValue.forText(it))
            }
            val dialogPresentation = Presentations.Builder().apply {
                setMenuPresentation(presentation)
            }.build()
            fieldBuilder.setPresentations(dialogPresentation)
            setField(autofillId, fieldBuilder.build())
        } else {
            @Suppress("DEPRECATION")
            setValue(
                autofillId,
                value?.let { AutofillValue.forText(it) },
                presentation
            )
        }
    }

    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun Dataset.Builder.addInlineAutofillValue(
        context: Context,
        autofillId: AutofillId,
        inlinePresentationSpec: InlinePresentationSpec,
        label: String?,
        value: String?,
    ) {
        val autofillLabel = label ?: context.getString(R.string.app_name)

        val authIntent = Intent(AUTOFILL_INTENT_ID).apply {
            setPackage(context.packageName)
        }

        val intentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_CANCEL_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            authIntent,
            intentFlags
        )

        val inlinePresentation = InlinePresentation(
            InlineSuggestionUi.newContentBuilder(pendingIntent).apply {
                setTitle(autofillLabel)
                setStartIcon(
                    Icon.createWithResource(context, R.mipmap.ic_launcher)
                        .setTintBlendMode(BlendMode.DST)
                )
            }.build().slice, inlinePresentationSpec, false
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val fieldBuilder = Field.Builder()
            value?.let {
                fieldBuilder.setValue(AutofillValue.forText(it))
            }
            val dialogPresentation = Presentations.Builder().apply {
                setInlinePresentation(inlinePresentation)
            }.build()
            fieldBuilder.setPresentations(dialogPresentation)
            setField(autofillId, fieldBuilder.build())
        } else {
            val presentation = if (label == null) {
                RemoteViews(context.packageName, R.layout.password_list_item).apply {
                    setTextViewText(R.id.text, autofillLabel)
                }
            } else {
                RemoteViews(context.packageName, android.R.layout.simple_list_item_1).apply {
                    setTextViewText(android.R.id.text1, autofillLabel)
                }
            }

            @Suppress("DEPRECATION")
            setValue(
                autofillId,
                value?.let { AutofillValue.forText(it) },
                presentation,
                inlinePresentation
            )
        }
    }

    fun buildIntent(context: Context, code: Int, autofillData: AutofillData): IntentSender {
        val appIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(NCPAutofillService.AUTOFILL_DATA, autofillData)
        }

        val intentFlags = PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE

        return PendingIntent.getActivity(
            context, code, appIntent, intentFlags
        ).intentSender
    }

    private const val AUTOFILL_INTENT_ID = "com.hegocre.nextcloudpasswords.intents.autofill"
    const val SEARCH_HINT = "search_hint"
}