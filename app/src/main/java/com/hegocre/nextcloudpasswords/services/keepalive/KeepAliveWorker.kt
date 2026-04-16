package com.hegocre.nextcloudpasswords.services.keepalive

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.hegocre.nextcloudpasswords.api.ApiController
import com.hegocre.nextcloudpasswords.api.SessionApi
import com.hegocre.nextcloudpasswords.data.user.UserController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class KeepAliveWorker(private val context: Context, private val params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val apiController = ApiController.getInstance(applicationContext)
        if (!apiController.sessionOpen.value) {
            return Result.success()
        }

        val server = UserController.getInstance(applicationContext).getServer()
        val sessionApi = SessionApi.getInstance(context)

        val sessionCode = params.inputData.getString(SESSION_CODE_KEY) ?: return Result.failure()
        val keepAliveDelay = params.inputData.getLong(KEEPALIVE_DELAY_KEY, -1L)
        if (keepAliveDelay == -1L) return Result.failure()

        val resultSuccess = withContext(Dispatchers.IO) {
            sessionApi.keepAlive(sessionCode)
        }

        if (resultSuccess) {
            WorkManager.getInstance(applicationContext)
                .enqueue(getRequest(keepAliveDelay, sessionCode))
            return Result.success()
        } else {
            if (params.runAttemptCount > 2) {
                ApiController.getInstance(applicationContext).clearSession()
                return Result.failure()
            } else {
                return Result.retry()
            }
        }
    }

    companion object {
        private const val SESSION_CODE_KEY = "com.hegocre.nextcloudpasswords.sessionCode"
        private const val KEEPALIVE_DELAY_KEY = "com.hegocre.nextcloudpasswords.keepAliveDelay"
        const val TAG = "com.hegocre.nextcloudpasswords.keepaliveworker"

        fun getRequest(delay: Long, sessionCode: String): WorkRequest {
            val data = Data.Builder()
                .putString(SESSION_CODE_KEY, sessionCode)
                .putLong(KEEPALIVE_DELAY_KEY, delay)
                .build()
            return OneTimeWorkRequestBuilder<KeepAliveWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10L, TimeUnit.SECONDS)
                .addTag(TAG)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
        }
    }
}