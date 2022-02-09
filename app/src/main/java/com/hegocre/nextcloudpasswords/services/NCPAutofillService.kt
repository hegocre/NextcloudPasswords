package com.hegocre.nextcloudpasswords.services

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.*
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.activities.MainActivity
import com.hegocre.nextcloudpasswords.utils.AssistStructureParser

@RequiresApi(Build.VERSION_CODES.O)
class NCPAutofillService : AutofillService() {
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

        val searchHint: String? = when {
            // If the structure contains a domain, use that (probably a web browser)
            helper.webDomain != null -> {
                helper.webDomain
            }
            else -> with(packageManager) {
                //Get the name of the package (QUERY_ALL_PACKAGES permission needed)
                Log.d("AUTOFILL", "Package name: ${helper.packageName}")
                try {
                    val app = getApplicationInfo(helper.packageName, PackageManager.GET_META_DATA)
                    getApplicationLabel(app).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    null
                }
            }
        }

        val authPresentation = RemoteViews(packageName, R.layout.password_list_item).apply {
            setTextViewText(R.id.text, getString(R.string.app_name))
        }

        // Intent to open MainActivity and provide a response to the request
        val authIntent = Intent(this, MainActivity::class.java).apply {
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
            val fillResponse = FillResponse.Builder()
                .setAuthentication(
                    helper.allAutofillIds.toTypedArray(),
                    intentSender,
                    authPresentation
                )
                .build()

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