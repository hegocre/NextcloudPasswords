package com.hegocre.nextcloudpasswords.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
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
        onFolderAdd == null -> AddFloatingActionButton(
            onClick = onPasswordAdd,
            elevation = if (menuBelowButton) flatElevation() else FloatingActionButtonDefaults.elevation(),
            modifier = modifier
        )
        menuBelowButton -> AddElementDropdownMenu(onPasswordAdd, onFolderAdd, modifier)
        else -> AddElementFloatingActionButtonMenu(onPasswordAdd, onFolderAdd, modifier)
    }
}

// A button at the top of the screen sits on the navigation rail, where a shadow looks detached
@Composable
private fun flatElevation() = FloatingActionButtonDefaults.bottomAppBarFabElevation()

@Composable
private fun AddFloatingActionButton(
    onClick: () -> Unit,
    elevation: FloatingActionButtonElevation,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(onClick = onClick, elevation = elevation, modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(id = R.string.action_create_element)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddElementDropdownMenu(
    onPasswordAdd: () -> Unit,
    onFolderAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        AddFloatingActionButton(onClick = { expanded = true }, elevation = flatElevation())
        DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 1),
                shadowElevation = MenuShadowElevation,
                modifier = Modifier.widthIn(min = MenuMinWidth)
            ) {
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onPasswordAdd()
                    },
                    text = { Text(text = stringResource(id = R.string.password)) },
                    shape = MenuDefaults.itemShape(index = 0, count = 2).shape,
                    leadingIcon = { Icon(imageVector = Icons.Outlined.VpnKey, contentDescription = null) }
                )
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onFolderAdd()
                    },
                    text = { Text(text = stringResource(id = R.string.folder)) },
                    shape = MenuDefaults.itemShape(index = 1, count = 2).shape,
                    leadingIcon = { Icon(imageVector = Icons.Outlined.Folder, contentDescription = null) }
                )
            }
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

// Menus grow in steps of 56dp, two steps leave the short labels too cramped
private val MenuMinWidth = 168.dp

// The light source for shadows sits above the middle of the screen, so a menu next to the rail at
// the start edge casts its default shadow visibly sideways; a lower elevation keeps it even
private val MenuShadowElevation = 1.dp
