package com.hegocre.nextcloudpasswords.utils

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.hegocre.nextcloudpasswords.R

// 1
private val biometricsIgnoredErrors = listOf(
    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
    BiometricPrompt.ERROR_CANCELED,
    BiometricPrompt.ERROR_USER_CANCELED,
    BiometricPrompt.ERROR_NO_BIOMETRICS
)

fun showBiometricPrompt(
    context: Context,
    onBiometricUnlock: () -> Unit,
    onBiometricFailed: (() -> Unit)? = null,
    onBiometricError: (() -> Unit)? = null
) {
    // 2
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(context.getString(R.string.unlock_app))
        .setNegativeButtonText(context.getString(android.R.string.cancel))
        .build()

    // 3
    val biometricPrompt = BiometricPrompt(
        context as FragmentActivity,
        object : BiometricPrompt.AuthenticationCallback() {
            // 4
            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                if (errorCode !in biometricsIgnoredErrors) {
                    onBiometricError?.invoke()
                }
            }

            // 5
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                onBiometricUnlock()
            }

            // 6
            override fun onAuthenticationFailed() {
                onBiometricFailed?.invoke()
            }
        }
    )
    // 7
    biometricPrompt.authenticate(promptInfo)
}
