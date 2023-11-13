package com.hegocre.nextcloudpasswords.services.autofill

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import androidx.annotation.RequiresApi
import com.hegocre.nextcloudpasswords.data.user.UserController
import com.hegocre.nextcloudpasswords.data.user.UserException

@RequiresApi(Build.VERSION_CODES.O)
class NCPAutofillService : AutofillService() {
    @SuppressLint("RestrictedApi")
    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val context = request.fillContexts
        val structure = context.last().structure

        val helper = AssistStructureParser(structure)

        // Do not autofill this application
        if (helper.packageName == packageName) {
            callback.onSuccess(null)
            return
        }
        try {
            UserController.getInstance(applicationContext).getServer()
        } catch (e: UserException) {
            // User not logged in, cannot fill request
            callback.onSuccess(null)
            return
        }

        val inlineSuggestionsRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            request.inlineSuggestionsRequest
        } else null

        val searchHint: String? = when {
            // If the structure contains a domain, use that (probably a web browser)
            helper.webDomain != null && helper.webDomain != "localhost" -> {
                helper.webDomain
            }

            else -> with(packageManager) {
                //Get the name of the package (QUERY_ALL_PACKAGES permission needed)
                try {
                    val app = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        getApplicationInfo(
                            helper.packageName,
                            PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                        )
                    else
                        getApplicationInfo(
                            helper.packageName,
                            PackageManager.GET_META_DATA
                        )

                    getApplicationLabel(app).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    null
                }
            }
        }

        // Intent to open MainActivity and provide a response to the request
        val authIntent = Intent("com.hegocre.nextcloudpasswords.action.main").apply {
            setPackage(packageName)
            putExtra(AUTOFILL_REQUEST, true)
            searchHint?.let {
                putExtra(AUTOFILL_SEARCH_HINT, it)
            }
        }

        val intentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_CANCEL_CURRENT
        }

        val intentSender = PendingIntent.getActivity(
            this,
            1001,
            authIntent,
            intentFlags
        ).intentSender

        if (helper.passwordAutofillIds.isNotEmpty()) {
            val fillResponse = FillResponse.Builder().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    addDataset(
                        AutofillHelper.buildDataset(
                            applicationContext,
                            null,
                            structure,
                            inlineSuggestionsRequest?.inlinePresentationSpecs?.first(),
                            intentSender
                        )
                    )
                } else {
                    addDataset(
                        AutofillHelper.buildDataset(
                            applicationContext,
                            null,
                            structure,
                            null,
                            intentSender
                        )
                    )
                }
            }.build()


            callback.onSuccess(fillResponse)
        } else {
            // Do not return a response if there are no autofill fields.
            callback.onSuccess(null)
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onFailure("Not implemented")
    }

    companion object {
        const val AUTOFILL_REQUEST = "autofill_request"
        const val AUTOFILL_SEARCH_HINT = "autofill_query"
    }
}