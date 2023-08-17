package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme

@Composable
fun PreferencesCategory(
    title: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.primary,
            LocalTextStyle provides MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp)
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                title()
            }
        }
        content()
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
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            title()
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodySmall
            ) {
                subtitle?.let {
                    it()
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
fun DropdownPreference(
    items: Map<String, String>,
    onItemSelected: (String) -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null
) {
    var dropdownVisible by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { dropdownVisible = true }
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            title()
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodySmall
            ) {
                subtitle?.let {
                    it()
                }
            }
        }
        Box {
            IconButton(onClick = { dropdownVisible = true }) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "More"
                )
            }
            DropdownMenu(
                expanded = dropdownVisible,
                offset = DpOffset(0.dp, -(56).dp),
                onDismissRequest = { dropdownVisible = false },
            ) {
                items.forEach { entry ->
                    DropdownMenuItem(
                        onClick = {
                            onItemSelected(entry.key)
                            dropdownVisible = false
                        },
                        text = {
                            Text(text = entry.value)
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun SwitchPreferencePreview() {
    NextcloudPasswordsTheme {
        Surface {
            SwitchPreference(
                checked = true,
                onCheckedChange = {},
                title = { Text(text = "Setting") },
                subtitle = { Text(text = "This is a setting") }
            )
        }
    }
}

@Preview
@Composable
fun DropdownPreferencePreview() {
    NextcloudPasswordsTheme {
        Surface {
            DropdownPreference(
                items = mapOf("" to ""),
                onItemSelected = {},
                title = { Text(text = "Setting") },
                subtitle = { Text(text = "Selected option") }
            )
        }
    }
}