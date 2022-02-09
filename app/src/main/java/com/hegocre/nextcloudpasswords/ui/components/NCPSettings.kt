package com.hegocre.nextcloudpasswords.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.NCPScreen
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun NCPSettingsScreen(
    onNavigationUp: () -> Unit
) {
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
                                contentDescription = "back"
                            )
                        }
                    },
                    elevation = if (MaterialTheme.colors.isLight) AppBarDefaults.TopAppBarElevation else 0.dp,
                    backgroundColor = MaterialTheme.colors.background
                )
            })
        { innerPadding ->
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            Column(Modifier.padding(innerPadding)) {
                val selectedScreen by PreferencesManager.getInstance(context).getStartScreen()
                    .collectAsState(initial = NCPScreen.Passwords.name, context = Dispatchers.IO)

                val startViews = mapOf(
                    NCPScreen.Passwords.name to stringResource(NCPScreen.Passwords.title),
                    NCPScreen.Favorites.name to stringResource(NCPScreen.Favorites.title),
                    NCPScreen.Folders.name to stringResource(NCPScreen.Folders.title)
                )

                PreferencesCategory(title = { Text(stringResource(R.string.general)) }) {
                    DropdownPreference(
                        items = startViews,
                        onItemSelected = { selectedScreen ->
                            scope.launch {
                                PreferencesManager.getInstance(context)
                                    .setStartScreen(selectedScreen)
                            }
                        },
                        title = { Text(text = stringResource(id = R.string.start_view_preference_title)) },
                        subtitle = {
                            Text(
                                text = startViews[selectedScreen] ?: NCPScreen.Passwords.name
                            )
                        }
                    )

                    val showIcons by PreferencesManager.getInstance(context).getShowIcons()
                        .collectAsState(initial = false, context = Dispatchers.IO)
                    SwitchPreference(
                        checked = showIcons,
                        onCheckedChange = { show ->
                            scope.launch {
                                PreferencesManager.getInstance(context).setShowIcons(show)
                            }
                        },
                        title = { Text(stringResource(R.string.show_icons_preference_title)) },
                        subtitle = { Text(stringResource(R.string.show_icons_preference_subtitle)) }
                    )
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