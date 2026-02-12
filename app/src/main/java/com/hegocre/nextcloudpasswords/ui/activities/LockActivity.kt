package com.hegocre.nextcloudpasswords.ui.activities

import com.hegocre.nextcloudpasswords.ui.components.NextcloudPasswordsAppLock
import com.hegocre.nextcloudpasswords.utils.AppLockHelper
import com.hegocre.nextcloudpasswords.services.autofill.NCPAutofillService
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import android.app.Activity
import android.util.Log
import android.view.autofill.AutofillManager
import android.service.autofill.Dataset
import android.os.Build

class LockActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val appLockHelper = remember { AppLockHelper.getInstance(context) }
            
            NextcloudPasswordsAppLock(
                onCheckPasscode = appLockHelper::checkPasscode,
                onCorrectPasscode = {
                    appLockHelper.disableLock()

                    val dataset: Dataset? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        intent.getParcelableExtra(
                            NCPAutofillService.SELECTED_DATASET,
                            Dataset::class.java
                        )
                    else
                        @Suppress("DEPRECATION") intent.getParcelableExtra(NCPAutofillService.SELECTED_DATASET)

                    val resultIntent = Intent().apply {
                        putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset)
                    }

                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            )
        }
    }
}