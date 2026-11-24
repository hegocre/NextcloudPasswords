package com.hegocre.nextcloudpasswords.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hegocre.nextcloudpasswords.R

/**
 * Creates a password or, when [onFolderAdd] is given, lets the user pick between a password and a
 * folder. The choice opens as a floating menu above the button, or as a dropdown below it when
 * [menuBelowButton] is set because the button sits at the top of the screen.
 */
@Composable
fun AddElementButton(
    onPasswordAdd: () -> Unit,
    onFolderAdd: (() -> Unit)?,
    menuBelowButton: Boolean,
    modifier: Modifier = Modifier
) {
    when {
        onFolderAdd == null -> AddFloatingActionButton(onClick = onPasswordAdd, modifier = modifier)
        menuBelowButton -> AddElementDropdownMenu(onPasswordAdd, onFolderAdd, modifier)
        else -> AddElementFloatingActionButtonMenu(onPasswordAdd, onFolderAdd, modifier)
    }
}

@Composable
private fun AddFloatingActionButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(id = R.string.action_create_element)
        )
    }
}

@Composable
private fun AddElementDropdownMenu(
    onPasswordAdd: () -> Unit,
    onFolderAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        AddFloatingActionButton(onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.password)) },
                leadingIcon = { Icon(imageVector = Icons.Outlined.VpnKey, contentDescription = null) },
                onClick = {
                    expanded = false
                    onPasswordAdd()
                }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.folder)) },
                leadingIcon = { Icon(imageVector = Icons.Outlined.Folder, contentDescription = null) },
                onClick = {
                    expanded = false
                    onFolderAdd()
                }
            )
        }
    }
}

@Composable
private fun AddElementFloatingActionButtonMenu(
    onPasswordAdd: () -> Unit,
    onFolderAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = expanded) { expanded = false }

    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(checked = expanded, onCheckedChange = { expanded = it }) {
                Icon(
                    imageVector = if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add,
                    contentDescription = stringResource(id = R.string.action_create_element),
                    modifier = Modifier.animateIcon({ checkedProgress })
                )
            }
        },
        // The menu insets itself from the screen edges, on top of the inset Scaffold gives its FAB
        modifier = modifier.offset(x = FabMenuInset, y = FabMenuInset)
    ) {
        FloatingActionButtonMenuItem(
            onClick = {
                expanded = false
                onPasswordAdd()
            },
            text = { Text(text = stringResource(id = R.string.password)) },
            icon = { Icon(imageVector = Icons.Filled.VpnKey, contentDescription = null) }
        )
        FloatingActionButtonMenuItem(
            onClick = {
                expanded = false
                onFolderAdd()
            },
            text = { Text(text = stringResource(id = R.string.folder)) },
            icon = { Icon(imageVector = Icons.Filled.Folder, contentDescription = null) }
        )
    }
}

private val FabMenuInset = 16.dp
