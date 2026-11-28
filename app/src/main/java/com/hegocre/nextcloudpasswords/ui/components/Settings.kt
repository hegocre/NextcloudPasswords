package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme

@Composable
fun PreferencesCategory(
    title: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        Column(
            modifier = Modifier.padding(bottom = 18.dp)
        ) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.primary,
                LocalTextStyle provides MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp)
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    title()
                }
            }

            content()
        }
    }

}

@Composable
fun SwitchPreference(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { if (enabled) onCheckedChange(!checked) }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(end = 12.dp)) {
            title()
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
            ) {
                subtitle?.let {
                    it()
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = { if (enabled) onCheckedChange(it) },
            enabled = enabled,
        )
    }
}

@Composable
fun ListPreference(
    items: Map<String, String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialogVisible by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { dialogVisible = true }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            title()
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
            ) {
                Text(items.getOrDefault(selectedItem, ""))
            }
        }

        if (dialogVisible) {
            ListPreferenceDialog(
                title = title,
                options = items,
                selectedOption = selectedItem,
                onSelectOption = {
                    onItemSelected(it)
                    dialogVisible = false
                },
                onDismissRequest = { dialogVisible = false }
            )
        }
    }
}

/**
 * A preference with a few short options, all shown at once as a connected button group, so that
 * choosing one takes a single tap instead of opening a dialog.
 */
@Composable
fun ButtonGroupPreference(
    items: Map<String, String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        title()
        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            val lastIndex = items.size - 1
            items.entries.forEachIndexed { index, (item, label) ->
                ToggleButton(
                    checked = item == selectedItem,
                    onCheckedChange = { onItemSelected(item) },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton }
                ) {
                    Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Preview
@Composable
fun PreferencesPreview() {
    NextcloudPasswordsTheme {
        Surface {
            Column {
                PreferencesCategory(title = { Text("General") }) {
                    ButtonGroupPreference(
                        items = mapOf(
                            "passwords" to "Passwords",
                            "favorites" to "Favorites",
                            "folders" to "Folders"
                        ),
                        onItemSelected = {},
                        title = { Text(text = "Initial view") },
                        selectedItem = "passwords"
                    )

                    SwitchPreference(
                        checked = true,
                        onCheckedChange = {},
                        title = { Text(text = "Show icons") },
                        subtitle = { Text(text = "Show website icons") }
                    )
                }

                PreferencesCategory(title = { Text("Security") }) {
                    SwitchPreference(
                        checked = true,
                        onCheckedChange = {},
                        title = { Text(text = "App lock") },
                        subtitle = { Text(text = "Lock access to the application with a code") }
                    )

                    SwitchPreference(
                        checked = true,
                        onCheckedChange = {},
                        title = { Text(text = "Biometric unlock") },
                        subtitle = { Text(text = "Unlock the app with biometric credentials such as fingerprint or face") }
                    )
                }

                PreferencesCategory(title = { Text("Security") }) {
                    SwitchPreference(
                        checked = true,
                        onCheckedChange = {},
                        title = { Text(text = "Autofill") },
                        subtitle = { Text(text = "Enable autofill service") },
                        enabled = false
                    )
                }
            }

        }
    }
}