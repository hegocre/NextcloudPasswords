package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
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
            LocalContentColor provides MaterialTheme.colors.primary,
            LocalTextStyle provides MaterialTheme.typography.subtitle1.copy(fontSize = 14.sp)
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
                LocalContentAlpha provides ContentAlpha.medium,
                LocalTextStyle provides MaterialTheme.typography.body1.copy(fontSize = 15.sp)
            ) {
                subtitle?.let {
                    it()
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
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
                LocalContentAlpha provides ContentAlpha.medium,
                LocalTextStyle provides MaterialTheme.typography.body1.copy(fontSize = 15.sp)
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
                    DropdownMenuItem(onClick = {
                        onItemSelected(entry.key)
                        dropdownVisible = false
                    }) {
                        Text(text = entry.value)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun CheckBoxPreview() {
    NextcloudPasswordsTheme {
        SwitchPreference(
            checked = true,
            onCheckedChange = {},
            title = { Text(text = "Title") },
            subtitle = { Text(text = "Subtitle") }
        )
    }
}