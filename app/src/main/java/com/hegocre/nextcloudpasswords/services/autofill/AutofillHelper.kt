package com.hegocre.nextcloudpasswords.services.autofill

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.assist.AssistStructure
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
import com.hegocre.nextcloudpasswords.data.password.Password

object AutofillHelper {
    @RequiresApi(Build.VERSION_CODES.O)
    fun buildDataset(
        context: Context,
        password: Password?,
        assistStructure: AssistStructure,
        inlinePresentationSpec: InlinePresentationSpec?,
        authenticationIntent: IntentSender? = null
    ): Dataset {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (inlinePresentationSpec != null) {
                buildInlineDataset(
                    context,
                    password,
                    assistStructure,
                    inlinePresentationSpec,
                    authenticationIntent
                )
            } else {
                buildPresentationDataset(context, password, assistStructure, authenticationIntent)
            }
        } else {
            buildPresentationDataset(context, password, assistStructure, authenticationIntent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun buildInlineDataset(
        context: Context,
        password: Password?,
        assistStructure: AssistStructure,
        inlinePresentationSpec: InlinePresentationSpec,
        authenticationIntent: IntentSender? = null
    ): Dataset {
        val helper = AssistStructureParser(assistStructure)
        return Dataset.Builder()
            .apply {
                helper.usernameAutofillIds.forEach { autofillId ->
                    addInlineAutofillValue(
                        context,
                        autofillId,
                        password?.label,
                        password?.username,
                        inlinePresentationSpec
                    )
                }
                helper.passwordAutofillIds.forEach { autofillId ->
                    addInlineAutofillValue(
                        context,
                        autofillId,
                        password?.label,
                        password?.password,
                        inlinePresentationSpec
                    )
                }
                if (authenticationIntent != null) {
                    setAuthentication(authenticationIntent)
                }
            }.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPresentationDataset(
        context: Context,
        password: Password?,
        assistStructure: AssistStructure,
        authenticationIntent: IntentSender? = null
    ): Dataset {
        val helper = AssistStructureParser(assistStructure)
        return Dataset.Builder().apply {
            helper.usernameAutofillIds.forEach { autofillId ->
                addAutofillValue(context, autofillId, password?.label, password?.username)
            }
            helper.passwordAutofillIds.forEach { autofillId ->
                addAutofillValue(context, autofillId, password?.label, password?.password)
            }
            if (authenticationIntent != null) {
                setAuthentication(authenticationIntent)
            }
        }.build()
    }

    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.O)
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
        label: String?,
        value: String?,
        inlinePresentationSpec: InlinePresentationSpec,
    ) {
        val autofillLabel = label ?: context.getString(R.string.app_name)

        val authIntent = Intent().apply {
            setPackage(context.packageName)
            identifier = AUTOFILL_INTENT_ID
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
                    Icon.createWithResource(context, R.mipmap.ic_launcher_round)
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

    const val AUTOFILL_INTENT_ID = "com.hegocre.nextcloudpasswords.intents.autofill"
}