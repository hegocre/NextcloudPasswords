package com.hegocre.nextcloudpasswords.ui.components

import android.content.res.Configuration
import androidx.biometric.BiometricManager
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.utils.AppLockHelper
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import com.hegocre.nextcloudpasswords.utils.showBiometricPrompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.job
import kotlin.math.roundToInt

@Composable
fun NCPAppLockWrapper(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val appLockHelper = remember { AppLockHelper.getInstance(context) }
    val hasAppLock by PreferencesManager.getInstance(context).getHasAppLock()
        .collectAsState(null)
    val isLocked by appLockHelper.isLocked.collectAsState()

    hasAppLock?.let {
        Crossfade(targetState = it && isLocked, label = "locked") { locked ->
            if (locked) {
                NextcloudPasswordsAppLock(
                    onCheckPasscode = appLockHelper::checkPasscode,
                    onCorrectPasscode = appLockHelper::disableLock
                )
            } else {
                if (hasAppLock == false) {
                    // Avoid asking for passcode just after setting it
                    appLockHelper.disableLock()
                }
                content()
            }
        }
    }
}

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
    val hasBiometricAppLock by if (isPreview) remember { mutableStateOf(true) }
    else PreferencesManager.getInstance(context).getHasBiometricAppLock().collectAsState(
        initial = false
    )
    val canAuthenticateBiometric = if (isPreview) remember { true }
    else remember {
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    LaunchedEffect(key1 = inputPassword) {
        if (onCheckPasscode(inputPassword).await()) {
            onCorrectPasscode()
        }
    }

    LaunchedEffect(key1 = isError) {
        if (isError) {
            delay(1500L)
            isError = false
        }
    }

    LaunchedEffect(key1 = hasBiometricAppLock) {
        if (hasBiometricAppLock && canAuthenticateBiometric) {
            showBiometricPrompt(
                context = context,
                title = context.getString(R.string.biometric_prompt_title),
                description = context.getString(R.string.biometric_prompt_description),
                onBiometricUnlock = onCorrectPasscode
            )
        }
    }

    val requester = remember { FocusRequester() }

    NextcloudPasswordsTheme {
        Scaffold(
            modifier = Modifier
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.Zero, Key.NumPad0 -> setInputPassword(inputPassword + "0")
                            Key.One, Key.NumPad1 -> setInputPassword(inputPassword + "1")
                            Key.Two, Key.NumPad2 -> setInputPassword(inputPassword + "2")
                            Key.Three, Key.NumPad3 -> setInputPassword(inputPassword + "3")
                            Key.Four, Key.NumPad4 -> setInputPassword(inputPassword + "4")
                            Key.Five, Key.NumPad5 -> setInputPassword(inputPassword + "5")
                            Key.Six, Key.NumPad6 -> setInputPassword(inputPassword + "6")
                            Key.Seven, Key.NumPad7 -> setInputPassword(inputPassword + "7")
                            Key.Eight, Key.NumPad8 -> setInputPassword(inputPassword + "8")
                            Key.Nine, Key.NumPad9 -> setInputPassword(inputPassword + "9")
                            Key.Backspace -> setInputPassword(inputPassword.dropLast(1))
                        }
                    }
                    return@onKeyEvent true
                }
                .focusRequester(requester)
        ) { paddingValues ->
            LaunchedEffect(key1 = Unit) {
                coroutineContext.job.invokeOnCompletion {
                    if (it?.cause == null) {
                        requester.requestFocus()
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PasscodeIndicator(
                            inputPassword = inputPassword,
                            isError = isError
                        )

                        val spacerPadding = minOf((screenHeight() * 0.04).roundToInt(), 170)
                        Spacer(modifier = Modifier.height(spacerPadding.dp))

                        KeyPad(
                            inputPassword = inputPassword,
                            setInputPassword = { setInputPassword(inputPassword + it) },
                            showBiometricIndicator = hasBiometricAppLock && canAuthenticateBiometric,
                            onBiometricClick = {
                                showBiometricPrompt(
                                    context = context,
                                    title = context.getString(R.string.biometric_prompt_title),
                                    description = context.getString(R.string.biometric_prompt_description),
                                    onBiometricUnlock = onCorrectPasscode
                                )
                            },
                            showBackspaceIndicator = inputPassword.isNotBlank(),
                            onBackspaceClick = { setInputPassword(inputPassword.dropLast(1)) },
                            onBackspaceLongClick = { setInputPassword("") }
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            PasscodeIndicator(
                                inputPassword = inputPassword,
                                isError = isError
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            KeyPad(
                                inputPassword = inputPassword,
                                setInputPassword = { setInputPassword(inputPassword + it) },
                                showBiometricIndicator = hasBiometricAppLock && canAuthenticateBiometric,
                                onBiometricClick = {
                                    showBiometricPrompt(
                                        context = context,
                                        title = context.getString(R.string.biometric_prompt_title),
                                        description = context.getString(R.string.biometric_prompt_description),
                                        onBiometricUnlock = onCorrectPasscode
                                    )
                                },
                                showBackspaceIndicator = inputPassword.isNotBlank(),
                                onBackspaceClick = { setInputPassword(inputPassword.dropLast(1)) },
                                onBackspaceLongClick = { setInputPassword("") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PasscodeIndicator(
    inputPassword: String,
    isError: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (!isError)
                stringResource(id = R.string.app_lock_input_passcode)
            else
                stringResource(id = R.string.error_app_lock_incorrect_code),
        )

        val spacerPadding = minOf((screenHeight() * 0.1).roundToInt(), 170)
        Spacer(modifier = Modifier.height(spacerPadding.dp))

        if (inputPassword.isEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
        } else {
            LazyRow(modifier = Modifier.padding(horizontal = 16.dp)) {
                items(count = inputPassword.length, key = { it }) {
                    KeyboardDigitIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

}

@Composable
fun KeyPad(
    inputPassword: String,
    setInputPassword: (String) -> Unit,
    showBiometricIndicator: Boolean,
    onBiometricClick: () -> Unit,
    showBackspaceIndicator: Boolean,
    onBackspaceClick: () -> Unit,
    onBackspaceLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPreview = LocalInspectionMode.current

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            KeyboardNumber(
                number = "1",
                onPressNumber = setInputPassword
            )

            KeyboardNumber(
                number = "2",
                onPressNumber = setInputPassword
            )

            KeyboardNumber(
                number = "3",
                onPressNumber = setInputPassword
            )
        }

        Row {
            KeyboardNumber(
                number = "4",
                onPressNumber = setInputPassword
            )

            KeyboardNumber(
                number = "5",
                onPressNumber = setInputPassword
            )

            KeyboardNumber(
                number = "6",
                onPressNumber = setInputPassword
            )
        }

        Row {
            KeyboardNumber(
                number = "7",
                onPressNumber = setInputPassword
            )

            KeyboardNumber(
                number = "8",
                onPressNumber = setInputPassword
            )

            KeyboardNumber(
                number = "9",
                onPressNumber = setInputPassword
            )
        }

        Row {
            if (showBiometricIndicator) {
                FilledTonalIconButton(
                    onClick = onBiometricClick,
                    modifier = Modifier
                        .padding(buttonPadding().dp)
                        .height(buttonSize().dp)
                        .width(buttonSize().dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Fingerprint,
                        contentDescription = stringResource(id = R.string.biometric_unlock_preference_title),
                        modifier = Modifier.size(35.dp)
                    )
                }
            } else {
                Spacer(
                    modifier = Modifier
                        .padding(buttonPadding().dp)
                        .height(buttonSize().dp)
                        .width(buttonSize().dp)
                )
            }

            KeyboardNumber(
                number = "0",
                onPressNumber = setInputPassword
            )

            if (isPreview || showBackspaceIndicator) {
                val interactionSource = remember { MutableInteractionSource() }

                val viewConfiguration = LocalViewConfiguration.current

                LaunchedEffect(interactionSource, inputPassword) {
                    var isLongClick = false

                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                isLongClick = false
                                delay(viewConfiguration.longPressTimeoutMillis)
                                isLongClick = true
                                onBackspaceLongClick()
                            }

                            is PressInteraction.Release -> {
                                if (isLongClick.not()) {
                                    onBackspaceClick()
                                }
                            }
                        }
                    }
                }

                FilledTonalIconButton(
                    onClick = {},
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .padding(buttonPadding().dp)
                        .height(buttonSize().dp)
                        .width(buttonSize().dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                        contentDescription = stringResource(id = R.string.action_delete),
                        modifier = Modifier.size(25.dp)
                    )
                }
            } else {
                Spacer(
                    modifier = Modifier
                        .padding(buttonPadding().dp)
                        .height(buttonSize().dp)
                        .width(buttonSize().dp)
                )
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
            .padding(buttonPadding().dp)
            .height(buttonSize().dp)
            .width(buttonSize().dp)
    ) {
        Text(text = number, fontSize = 25.sp)
    }
}

@Composable
fun KeyboardDigitIndicator(
    color: Color,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) {
        visible = true
    }

    val scale by animateFloatAsState(targetValue = if (visible) 1f else 0f, label = "opacity")

    Spacer(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .height(10.dp)
            .width(10.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun screenHeight(): Float =
    LocalConfiguration.current.screenHeightDp.toFloat()

@Composable
fun screenWidth(): Float =
    LocalConfiguration.current.screenWidthDp.toFloat()

@Composable
fun buttonSize(): Int {
    val screenHeight = screenHeight().times(
        if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.8f else 1f
    )
    return minOf((screenHeight * 0.20).roundToInt(), (screenWidth() * 0.25).roundToInt(), 90)
}

@Composable
fun buttonPadding(): Int {
    val screenHeight = screenHeight().times(
        if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.8f else 1f
    )
    return minOf((screenHeight * 0.04).roundToInt(), (screenWidth() * 0.06).roundToInt(), 8)
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