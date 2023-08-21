package com.hegocre.nextcloudpasswords.ui.components

import androidx.biometric.BiometricManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import com.hegocre.nextcloudpasswords.utils.showBiometricPrompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

@Composable
fun NextcloudPasswordsAppLock(
    onCheckPasscode: (String) -> Deferred<Boolean>,
    onCorrectPasscode: () -> Unit
) {
    val isPreview = LocalInspectionMode.current

    val (inputPassword, setInputPassword) = rememberSaveable {
        mutableStateOf("")
    }
    var isError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val hasBiometricAppLock by if (isPreview) remember { mutableStateOf(false) }
    else PreferencesManager.getInstance(context).getHasBiometricAppLock().collectAsState(
        initial = false
    )

    LaunchedEffect(key1 = inputPassword) {
        if (inputPassword.length == 4) {
            if (onCheckPasscode(inputPassword).await()) {
                onCorrectPasscode()
            } else {
                setInputPassword("")
                isError = true
            }
        }
    }

    LaunchedEffect(key1 = isError) {
        if (isError) {
            delay(1500L)
            isError = false
        }
    }

    LaunchedEffect(key1 = hasBiometricAppLock) {
        if (hasBiometricAppLock &&
            BiometricManager.from(context)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        ) {
            showBiometricPrompt(
                context = context,
                onBiometricUnlock = onCorrectPasscode
            )
        }
    }

    NextcloudPasswordsTheme {
        Scaffold { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (!isError)
                            stringResource(id = R.string.unlock_app)
                        else
                            stringResource(id = R.string.incorrect_code),
                    )

                    Spacer(modifier = Modifier.height(168.dp))

                    Row {
                        KeyboardDigitIndicator(
                            enabled = inputPassword.isNotEmpty(),
                            isError = isError
                        )

                        KeyboardDigitIndicator(
                            enabled = inputPassword.length > 1,
                            isError = isError
                        )

                        KeyboardDigitIndicator(
                            enabled = inputPassword.length > 2,
                            isError = isError
                        )

                        KeyboardDigitIndicator(
                            enabled = inputPassword.length > 3,
                            isError = isError
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Row {
                        KeyboardNumber(
                            number = "1",
                            onPressNumber = {
                                setInputPassword(inputPassword + it)
                            }
                        )

                        KeyboardNumber(
                            number = "2",
                            onPressNumber = {
                                setInputPassword(inputPassword + it)
                            }
                        )

                        KeyboardNumber(
                            number = "3",
                            onPressNumber = {
                                setInputPassword(inputPassword + it)
                            }
                        )
                    }

                    Row {
                        KeyboardNumber(
                            number = "4",
                            onPressNumber = {
                                setInputPassword(inputPassword + it)
                            }
                        )

                        KeyboardNumber(
                            number = "5",
                            onPressNumber = {
                                setInputPassword(inputPassword + it)
                            }
                        )

                        KeyboardNumber(
                            number = "6",
                            onPressNumber = {
                                setInputPassword(inputPassword + it)
                            }
                        )
                    }

                    Row {
                        KeyboardNumber(
                            number = "7",
                            onPressNumber = {
                                setInputPassword(inputPassword + it)
                            }
                        )

                        KeyboardNumber(
                            number = "8",
                            onPressNumber = {
                                setInputPassword(inputPassword + it)
                            }
                        )

                        KeyboardNumber(
                            number = "9",
                            onPressNumber = {
                                setInputPassword(inputPassword + it)
                            }
                        )
                    }

                    Row {
                        Spacer(
                            modifier = Modifier
                                .padding(AppLockDefaults.DIGIT_PADDING)
                                .height(AppLockDefaults.DIGIT_SIZE)
                                .width(AppLockDefaults.DIGIT_SIZE)
                        )

                        KeyboardNumber(
                            number = "0",
                            onPressNumber = {
                                setInputPassword(inputPassword + it)
                            }
                        )

                        if (inputPassword.isNotBlank()) {
                            FilledTonalIconButton(
                                onClick = {
                                    setInputPassword(inputPassword.dropLast(1))
                                },
                                modifier = Modifier
                                    .padding(AppLockDefaults.DIGIT_PADDING)
                                    .height(AppLockDefaults.DIGIT_SIZE)
                                    .width(AppLockDefaults.DIGIT_SIZE)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Backspace,
                                    contentDescription = stringResource(id = R.string.delete)
                                )
                            }
                        } else {
                            Spacer(
                                modifier = Modifier
                                    .padding(AppLockDefaults.DIGIT_PADDING)
                                    .height(AppLockDefaults.DIGIT_SIZE)
                                    .width(AppLockDefaults.DIGIT_SIZE)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeyboardNumber(
    number: String,
    onPressNumber: (String) -> Unit
) {
    FilledTonalButton(
        onClick = {
            onPressNumber(number)
        },
        modifier = Modifier
            .padding(AppLockDefaults.DIGIT_PADDING)
            .height(AppLockDefaults.DIGIT_SIZE)
            .width(AppLockDefaults.DIGIT_SIZE)
    ) {
        Text(text = number, fontSize = 25.sp)
    }
}

@Composable
fun KeyboardDigitIndicator(
    enabled: Boolean,
    isError: Boolean
) {
    Spacer(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .height(10.dp)
            .width(10.dp)
            .clip(CircleShape)
            .background(
                if (!isError) {
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = animateFloatAsState(
                            targetValue = if (enabled) 1f else 0.4f,
                            label = "alpha"
                        ).value
                    )
                } else {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                }
            )
    )
}

object AppLockDefaults {
    val DIGIT_SIZE = 90.dp
    val DIGIT_PADDING = 8.dp
}

@Preview
@Composable
fun AppLockPreview() {
    NextcloudPasswordsTheme {
        NextcloudPasswordsAppLock(onCheckPasscode = {
            return@NextcloudPasswordsAppLock CoroutineScope(Dispatchers.Default).async {
                true
            }
        }, {})
    }
}