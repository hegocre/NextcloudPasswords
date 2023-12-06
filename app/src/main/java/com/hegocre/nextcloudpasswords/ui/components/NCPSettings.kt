package com.hegocre.nextcloudpasswords.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.NCPScreen
import com.hegocre.nextcloudpasswords.ui.theme.NCPTheme
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import com.hegocre.nextcloudpasswords.utils.showBiometricPrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NCPSettingsScreen(
    onNavigationUp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember {
        PreferencesManager.getInstance(context)
    }

    NextcloudPasswordsTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.settings))
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigationUp) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(id = R.string.back)
                            )
                        }
                    },
                    windowInsets = WindowInsets.statusBars
                )
            },
            bottomBar = {
                Spacer(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                )
            }
        )
        { innerPadding ->
            Column(
                Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                PreferencesCategory(title = { Text(stringResource(R.string.general)) }) {
                    val selectedScreen by preferencesManager.getStartScreen()
                        .collectAsState(
                            initial = NCPScreen.Passwords.name,
                            context = Dispatchers.IO
                        )

                    val startViews = mapOf(
                        NCPScreen.Passwords.name to stringResource(NCPScreen.Passwords.title),
                        NCPScreen.Favorites.name to stringResource(NCPScreen.Favorites.title),
                        NCPScreen.Folders.name to stringResource(NCPScreen.Folders.title)
                    )

                    ListPreference(
                        items = startViews,
                        onItemSelected = {
                            scope.launch {
                                preferencesManager.setStartScreen(it)
                            }
                        },
                        title = { Text(text = stringResource(id = R.string.start_view_preference_title)) },
                        selectedItem = selectedScreen
                    )

                    val showIcons by preferencesManager.getShowIcons()
                        .collectAsState(initial = false, context = Dispatchers.IO)
                    SwitchPreference(
                        checked = showIcons,
                        onCheckedChange = { show ->
                            scope.launch(Dispatchers.IO) {
                                preferencesManager.setShowIcons(show)
                            }
                        },
                        title = { Text(stringResource(R.string.show_icons_preference_title)) },
                        subtitle = { Text(stringResource(R.string.show_icons_preference_subtitle)) }
                    )
                }

                PreferencesCategory(title = { Text(text = stringResource(id = R.string.appearance)) }) {
                    val appTheme by preferencesManager.getAppTheme()
                        .collectAsState(initial = NCPTheme.SYSTEM)
                    val useNextcloudInstanceColor by preferencesManager.getUseInstanceColor()
                        .collectAsState(initial = false)
                    val useSystemDynamicColor by preferencesManager.getUseSystemDynamicColor()
                        .collectAsState(initial = false)
                    val themes = mapOf(
                        NCPTheme.SYSTEM to stringResource(id = R.string.system),
                        NCPTheme.LIGHT to stringResource(id = R.string.light),
                        NCPTheme.DARK to stringResource(id = R.string.dark),
                        NCPTheme.AMOLED to stringResource(id = R.string.black)
                    )

                    ListPreference(
                        items = themes,
                        selectedItem = appTheme,
                        onItemSelected = { theme ->
                            scope.launch(Dispatchers.IO) {
                                preferencesManager.setAppTheme(theme)
                            }
                        },
                        title = { Text(text = stringResource(id = R.string.app_theme)) })

                    SwitchPreference(
                        checked = useNextcloudInstanceColor,
                        onCheckedChange = { use ->
                            scope.launch(Dispatchers.IO) {
                                preferencesManager.setUseInstanceColor(use)
                            }
                        },
                        title = { Text(text = stringResource(id = R.string.use_nextcloud_color)) },
                        subtitle = { Text(text = stringResource(id = R.string.use_nextcloud_color_msg)) },
                        enabled = !useSystemDynamicColor
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SwitchPreference(
                            checked = useSystemDynamicColor,
                            onCheckedChange = { use ->
                                scope.launch(Dispatchers.IO) {
                                    preferencesManager.setUseSystemDynamicColor(use)
                                }
                            },
                            title = { Text(text = stringResource(id = R.string.use_dynamic_color)) },
                            subtitle = { Text(text = stringResource(id = R.string.use_dynamic_color_msg)) },
                            enabled = !useNextcloudInstanceColor
                        )
                    }
                }

                PreferencesCategory(title = { Text(text = stringResource(id = R.string.security)) }) {
                    val hasAppLock by preferencesManager.getHasAppLock()
                        .collectAsState(false)
                    val hasBiometricAppLock by preferencesManager
                        .getHasBiometricAppLock().collectAsState(false)
                    val canUseBiometrics = remember {
                        BiometricManager.from(context)
                            .canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
                    }

                    var showCreatePasscodeDialog by rememberSaveable {
                        mutableStateOf(false)
                    }
                    var showConfirmPasscodeDialog by rememberSaveable {
                        mutableStateOf(false)
                    }
                    var showDeletePasscodeDialog by rememberSaveable {
                        mutableStateOf(false)
                    }
                    var firstPasscode by rememberSaveable {
                        mutableStateOf("")
                    }

                    SwitchPreference(
                        checked = hasAppLock,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showCreatePasscodeDialog = true
                            } else {
                                showDeletePasscodeDialog = true
                            }
                        },
                        title = { Text(text = stringResource(id = R.string.app_lock_settings_title)) },
                        subtitle = { Text(text = stringResource(id = R.string.app_lock_settings_subtitle)) }
                    )

                    if (canUseBiometrics) {
                        SwitchPreference(
                            checked = hasBiometricAppLock,
                            onCheckedChange = { enabled ->
                                showBiometricPrompt(
                                    context = context,
                                    title = context.getString(R.string.biometric_prompt_title),
                                    description = context.getString(R.string.biometric_prompt_description),
                                    onBiometricUnlock = {
                                        scope.launch {
                                            preferencesManager.setHasBiometricAppLock(enabled)
                                        }
                                    }
                                )

                            },
                            title = { Text(text = stringResource(id = R.string.biometric_unlock_settings_title)) },
                            subtitle = { Text(text = stringResource(id = R.string.biometric_unlock_settings_subtitle)) },
                            enabled = hasAppLock
                        )
                    }

                    if (showCreatePasscodeDialog) {
                        InputPasscodeDialog(
                            title = stringResource(id = R.string.input_passcode),
                            onInputPasscode = {
                                firstPasscode = it
                                showCreatePasscodeDialog = false
                                showConfirmPasscodeDialog = true
                            },
                            onDismissRequest = {
                                showCreatePasscodeDialog = false
                            }
                        )
                    }

                    if (showConfirmPasscodeDialog) {
                        InputPasscodeDialog(
                            title = stringResource(id = R.string.confirm_passcode),
                            onInputPasscode = { secondPasscode ->
                                if (firstPasscode != secondPasscode) {
                                    Toast.makeText(
                                        context,
                                        R.string.passcodes_dont_match,
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    scope.launch {
                                        with(preferencesManager) {
                                            setAppLockPasscode(secondPasscode)
                                            setHasAppLock(true)
                                        }
                                    }
                                }
                                showConfirmPasscodeDialog = false
                            },
                            onDismissRequest = {
                                showConfirmPasscodeDialog = false
                                firstPasscode = ""
                            }
                        )
                    }

                    if (showDeletePasscodeDialog) {
                        InputPasscodeDialog(
                            title = stringResource(id = R.string.input_passcode),
                            onInputPasscode = { passcode ->
                                scope.launch {
                                    with(preferencesManager) {
                                        val currentPasscode = getAppLockPasscode()

                                        if (currentPasscode == passcode) {
                                            setHasAppLock(false)
                                            setAppLockPasscode(null)
                                            setHasBiometricAppLock(false)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                R.string.incorrect_code,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                    }
                                }
                                showDeletePasscodeDialog = false
                            },
                            onDismissRequest = {
                                showDeletePasscodeDialog = false
                            }
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val autofillManager = context.getSystemService(AutofillManager::class.java)
                    var autofillEnabled by remember { mutableStateOf(autofillManager.hasEnabledAutofillServices()) }
                    val launchAutofillRequest = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            autofillEnabled = true
                        }
                    }
                    PreferencesCategory(title = { Text(text = stringResource(R.string.autofill_service)) }) {
                        SwitchPreference(
                            checked = autofillEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    val intent =
                                        Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                    launchAutofillRequest.launch(intent)
                                }
                            },
                            title = { Text(stringResource(R.string.autofill)) },
                            subtitle = { Text(stringResource(R.string.autofill_setting_subtitle)) },
                            enabled = !autofillEnabled
                        )
                    }
                }
            }
        }
    }
}