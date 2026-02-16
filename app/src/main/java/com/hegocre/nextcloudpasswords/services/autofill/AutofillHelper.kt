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
import com.hegocre.nextcloudpasswords.ui.activities.MainActivity
import android.service.autofill.SaveInfo
import android.os.Parcel
import android.os.Parcelable
import android.os.Bundle
import android.util.Log
import android.view.autofill.AutofillManager

data class SaveData(
    val label: String,
    val username: String,
    val password: String,
    val url: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(label)
        parcel.writeString(username)
        parcel.writeString(password)
        parcel.writeString(url)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<SaveData> {
        override fun createFromParcel(parcel: Parcel): SaveData {
            return SaveData(parcel)
        }

        override fun newArray(size: Int): Array<SaveData?> {
            return arrayOfNulls(size)
        }
    }
}

data class PasswordAutofillData(val id: String?, val label: String, val username: String?, val password: String?)

@RequiresApi(Build.VERSION_CODES.O)
object AutofillHelper {
    fun buildDataset(
        context: Context,
        password: PasswordAutofillData?,
        helper: AssistStructureParser,
        inlinePresentationSpec: InlinePresentationSpec?,
        intent: IntentSender? = null,
        needsAppLock: Boolean = false
    ): Dataset {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (inlinePresentationSpec != null) {
                buildInlineDataset(
                    context,
                    password,
                    helper,
                    inlinePresentationSpec,
                    intent,
                    needsAppLock
                )
            } else {
                buildPresentationDataset(context, password, helper, intent, needsAppLock)
            }
        } else {
            buildPresentationDataset(context, password, helper, intent, needsAppLock)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    fun buildSaveInfo(helper: AssistStructureParser): Pair<SaveInfo, Bundle?>? {
        val requiredIds = mutableListOf<AutofillId>()
        val optionalIds = mutableListOf<AutofillId>()

        Log.d(NCPAutofillService.TAG, "Building SaveInfo, usernameAutofillIds: ${helper.usernameAutofillIds}, passwordAutofillIds: ${helper.passwordAutofillIds}")
        
        if (helper.passwordAutofillIds.size == 1) requiredIds += helper.passwordAutofillIds[0]
        else optionalIds += helper.passwordAutofillIds

        if (helper.usernameAutofillIds.size == 1) requiredIds += helper.usernameAutofillIds[0]
        else optionalIds += helper.usernameAutofillIds

        Log.d(NCPAutofillService.TAG, "Required IDs: $requiredIds, Optional IDs: $optionalIds")

        val type = if (!helper.usernameAutofillIds.isEmpty()) {
            SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD
        } else {
            SaveInfo.SAVE_DATA_TYPE_PASSWORD
        } 

        val builder = if (!requiredIds.isEmpty()) {
            SaveInfo.Builder(type, requiredIds.toTypedArray())
        } else {
            SaveInfo.Builder(type)
        }

        // if there are only username views but no password views, then delay the save on supported devices
        if(!helper.usernameAutofillIds.isEmpty() && helper.passwordAutofillIds.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d(NCPAutofillService.TAG, "Delaying save because only username views are detected")
            return Pair(
                builder.apply {
                    setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE or SaveInfo.FLAG_DELAY_SAVE)
                }.build(),
                Bundle().apply {
                    putCharSequence(USERNAME, helper.usernameAutofillContent.firstOrNull() ?: "")
                }
            )
        } else if (!helper.passwordAutofillIds.isEmpty()) {
            return Pair(
                builder.apply {
                    setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
                    if (!optionalIds.isEmpty()) setOptionalIds(optionalIds.toTypedArray())
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
        password: PasswordAutofillData?,
        helper: AssistStructureParser,
        inlinePresentationSpec: InlinePresentationSpec,
        intent: IntentSender? = null,
        needsAppLock: Boolean = false
    ): Dataset {
        // build redacted dataset when app lock is needed
        return if (needsAppLock && password != null && password.id != null) {
            Dataset.Builder().apply {
                helper.usernameAutofillIds.forEach { autofillId ->
                    addInlineAutofillValue(
                        context,
                        autofillId,
                        password.label,
                        null,
                        inlinePresentationSpec
                    )
                }
                helper.passwordAutofillIds.forEach { autofillId ->
                    addInlineAutofillValue(
                        context,
                        autofillId,
                        password.label,
                        null,
                        inlinePresentationSpec
                    )
                }
                setAuthentication(buildAppLockIntent(context, password.id, helper))
            }.build()
        } else {
            Dataset.Builder().apply {
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
                intent?.let { setAuthentication(it) }
            }.build()
        }
    }

    private fun buildPresentationDataset(
        context: Context,
        password: PasswordAutofillData?,
        helper: AssistStructureParser,
        intent: IntentSender? = null,
        needsAppLock: Boolean = false
    ): Dataset {
        // build redacted dataset when app lock is needed
        return if (needsAppLock && password != null && password.id != null) {
            Dataset.Builder().apply {
                helper.usernameAutofillIds.forEach { autofillId ->
                    addAutofillValue(context, autofillId, password.label, null)
                }
                helper.passwordAutofillIds.forEach { autofillId ->
                    addAutofillValue(context, autofillId, password.label, null)
                }
                setAuthentication(buildAppLockIntent(context, password.id, helper))
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

    fun buildAppLockIntent(context: Context, passwordId: String, helper: AssistStructureParser): IntentSender {
        val authIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(NCPAutofillService.AUTOFILL_REQUEST, true)
            putExtra(NCPAutofillService.PASSWORD_ID, passwordId)
            putExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE, helper.structure)
        }

        val intentFlags = PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE

        return PendingIntent.getActivity(
            context, 1001, authIntent, intentFlags // TODO: unique code?
        ).intentSender
    }

    private const val AUTOFILL_INTENT_ID = "com.hegocre.nextcloudpasswords.intents.autofill"
    const val USERNAME = "username"
}