package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.api.FoldersApi
import com.hegocre.nextcloudpasswords.data.folder.Folder
import com.hegocre.nextcloudpasswords.data.password.CustomField
import com.hegocre.nextcloudpasswords.ui.theme.ContentAlpha
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import kotlinx.coroutines.job

@Composable
fun MasterPasswordDialog(
    masterPassword: String,
    setMasterPassword: (String) -> Unit,
    savePassword: Boolean,
    setSavePassword: (Boolean) -> Unit,
    onOkClick: () -> Unit,
    errorText: String = "",
    onDismissRequest: (() -> Unit)? = null
) {
    val requester = remember { FocusRequester() }

    var showPassword by rememberSaveable { mutableStateOf(false) }
    Dialog(
        onDismissRequest = { onDismissRequest?.invoke() },
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = contentColorFor(backgroundColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                OutlinedTextFieldWithCaption(
                    text = masterPassword,
                    onValueChange = setMasterPassword,
                    visualTransformation = if (showPassword)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardType = KeyboardType.Password,
                    label = stringResource(R.string.enter_master_password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword)
                                    Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = stringResource(R.string.show_password)
                            )
                        }
                    },
                    errorText = errorText,
                    modifier = Modifier.focusRequester(requester)
                )

                CompositionLocalProvider(
                    LocalContentColor provides LocalContentColor.current.copy(alpha = ContentAlpha.medium)
                ) {
                    Row {
                        Checkbox(
                            checked = savePassword,
                            onCheckedChange = setSavePassword,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Text(
                            text = "Save password",
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        setSavePassword(!savePassword)
                                    }
                                },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                TextButton(
                    onClick = onOkClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        coroutineContext.job.invokeOnCompletion {
            requester.requestFocus()
        }
    }
}

@Composable
fun LogOutDialog(
    onDismissRequest: (() -> Unit)? = null,
    onConfirmButton: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismissRequest?.invoke() },
        title = { Text(text = stringResource(R.string.log_out)) },
        text = { Text(text = stringResource(R.string.are_you_sure_log_out)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmButton()
                    onDismissRequest?.invoke()
                }
            ) {
                Text(text = stringResource(R.string.log_out))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismissRequest?.invoke() }) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        }
    )
}

@Composable
fun DeleteElementDialog(
    onDismissRequest: (() -> Unit)? = null,
    onConfirmButton: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismissRequest?.invoke() },
        title = { Text(text = stringResource(R.string.delete)) },
        text = { Text(text = stringResource(R.string.are_you_sure_delete_element)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmButton()
                    onDismissRequest?.invoke()
                }
            ) {
                Text(text = stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismissRequest?.invoke() }) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomFieldDialog(
    onAddClick: (String, String) -> Unit,
    onDismissRequest: (() -> Unit)? = null
) {
    val types = mapOf(
        CustomField.TYPE_TEXT to stringResource(id = R.string.text),
        CustomField.TYPE_EMAIL to stringResource(id = R.string.email),
        CustomField.TYPE_URL to stringResource(id = R.string.url),
        CustomField.TYPE_SECRET to stringResource(id = R.string.secret)
    )

    val (type, setType) = remember { mutableStateOf(CustomField.TYPE_TEXT) }
    val (label, setLabel) = remember { mutableStateOf("") }

    var typeMenuExpanded by remember { mutableStateOf(false) }

    var showEmptyError by rememberSaveable {
        mutableStateOf(false)
    }

    Dialog(
        onDismissRequest = { onDismissRequest?.invoke() },
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = contentColorFor(backgroundColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(all = 24.dp)) {
                Text(
                    text = stringResource(id = R.string.add_custom_field),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {
                    ExposedDropdownMenuBox(
                        expanded = typeMenuExpanded,
                        onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .menuAnchor(),
                            value = types[type] ?: "",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                            label = { Text(text = stringResource(id = R.string.field_type)) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )

                        ExposedDropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false }
                        ) {
                            types.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(text = type.value) },
                                    onClick = {
                                        setType(type.key)
                                        typeMenuExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        modifier = Modifier.padding(bottom = 16.dp, top = 8.dp),
                        value = label,
                        onValueChange = setLabel,
                        singleLine = true,
                        maxLines = 1,
                        label = { Text(text = stringResource(id = R.string.label)) },
                        isError = showEmptyError && label.isBlank(),
                        supportingText = if (showEmptyError && label.isBlank()) {
                            {
                                Text(text = stringResource(id = R.string.field_cannot_be_empty))
                            }
                        } else null
                    )
                }


                TextButton(
                    onClick = {
                        if (label.isBlank()) {
                            showEmptyError = true
                        } else {
                            onAddClick(type, label)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 0.dp)
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        }
    }
}

@Composable
fun SelectFolderDialog(
    folders: List<Folder>,
    currentFolder: String,
    onSelectClick: (String) -> Unit,
    onDismissRequest: (() -> Unit)? = null
) {
    val (selectedFolderId, setSelectedFolderId) = remember { mutableStateOf(currentFolder) }
    val filteredFolders = remember(folders, selectedFolderId) {
        folders.filter {
            it.parent == selectedFolderId
        }
    }
    val selectedFolder = remember(folders, selectedFolderId) {
        folders.firstOrNull { it.id == selectedFolderId }
    }
    val parentFolder = remember(selectedFolder) {
        folders.firstOrNull { it.id == selectedFolder?.parent }
    }

    Dialog(
        onDismissRequest = { onDismissRequest?.invoke() },
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = contentColorFor(backgroundColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 24.dp)) {
                Text(
                    text = if (selectedFolderId == FoldersApi.DEFAULT_FOLDER_UUID) {
                        stringResource(id = R.string.home)
                    } else {
                        selectedFolder?.label ?: stringResource(id = R.string.home)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .padding(horizontal = 24.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (selectedFolderId != FoldersApi.DEFAULT_FOLDER_UUID) {
                            item(key = "parent_${parentFolder?.id ?: FoldersApi.DEFAULT_FOLDER_UUID}") {
                                ListItem(
                                    leadingContent = {
                                        Image(
                                            imageVector = Icons.Filled.Folder,
                                            contentDescription = stringResource(R.string.folder_icon),
                                            colorFilter = ColorFilter.tint(
                                                MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = ContentAlpha.medium
                                                )
                                            ),
                                            modifier = Modifier
                                                .size(45.dp)
                                                .padding(8.dp)
                                        )
                                    },
                                    headlineContent = {
                                        Text(text = "..")
                                    },
                                    modifier = Modifier.clickable {
                                        setSelectedFolderId(
                                            parentFolder?.id ?: FoldersApi.DEFAULT_FOLDER_UUID
                                        )
                                    }
                                )
                            }
                        }

                        items(items = filteredFolders, key = { folder -> folder.id }) { folder ->
                            FolderRow(
                                folder = folder,
                                onFolderClick = {
                                    setSelectedFolderId(folder.id)
                                },
                                modifier = Modifier
                            )
                        }
                    }
                }

                TextButton(
                    onClick = {
                        onSelectClick(selectedFolderId)
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 24.dp)
                ) {
                    Text(text = stringResource(R.string.select))
                }
            }
        }
    }
}

@Composable
fun AddElementDialog(
    onPasswordAdd: () -> Unit,
    onFolderAdd: () -> Unit,
    onDismissRequest: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = { onDismissRequest?.invoke() },
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = contentColorFor(backgroundColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 24.dp)) {
                Text(
                    text = stringResource(id = R.string.create),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .padding(horizontal = 24.dp)
                )

                ListItem(
                    headlineContent = {
                        Text(text = stringResource(id = R.string.password))
                    },
                    modifier = Modifier
                        .clickable(onClick = onPasswordAdd)
                        .padding(horizontal = 8.dp)
                )

                ListItem(
                    headlineContent = {
                        Text(text = stringResource(id = R.string.folder))
                    },
                    modifier = Modifier
                        .clickable(onClick = onFolderAdd)
                        .padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun InputPasscodeDialog(
    title: String,
    onInputPasscode: (String) -> Unit,
    onDismissRequest: (() -> Unit)? = null
) {
    val requester = FocusRequester()

    var showPasscode by rememberSaveable { mutableStateOf(false) }
    val (passcode, setPasscode) = remember { mutableStateOf("") }

    var showEmptyError by rememberSaveable {
        mutableStateOf(false)
    }

    Dialog(
        onDismissRequest = { onDismissRequest?.invoke() },
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = contentColorFor(backgroundColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(all = 24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    modifier = Modifier
                        .padding(bottom = 16.dp, top = 8.dp)
                        .focusRequester(requester),
                    value = passcode,
                    onValueChange = { newPasscode ->
                        if (newPasscode.length <= 4 &&
                            (newPasscode.toIntOrNull() != null || newPasscode.isEmpty())
                        ) {
                            setPasscode(newPasscode)
                        }
                    },
                    singleLine = true,
                    maxLines = 1,
                    label = { Text(text = stringResource(id = R.string.passcode)) },
                    isError = showEmptyError && passcode.isBlank(),
                    supportingText = { Text(text = "${passcode.length}/4") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = { showPasscode = !showPasscode }) {
                            Icon(
                                imageVector = if (showPasscode)
                                    Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = stringResource(R.string.show_password)
                            )
                        }
                    },
                    visualTransformation = if (showPasscode)
                        VisualTransformation.None else PasswordVisualTransformation(),
                )


                TextButton(
                    onClick = {
                        if (passcode.length != 4 || passcode.toIntOrNull() == null) {
                            showEmptyError = true
                        } else {
                            onInputPasscode(passcode)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 0.dp)
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        coroutineContext.job.invokeOnCompletion {
            requester.requestFocus()
        }
    }
}

@Composable
fun ListPreferenceDialog(
    title: (@Composable () -> Unit),
    options: Map<String, String>,
    selectedOption: String,
    onSelectOption: (String) -> Unit,
    onDismissRequest: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = { onDismissRequest?.invoke() },
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = contentColorFor(backgroundColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 24.dp)) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.headlineSmall
                    ) {
                        title()
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                ) {
                    items(items = options.keys.toList(), key = { it }) { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectOption(option)
                                }
                                .padding(vertical = 4.dp, horizontal = 12.dp)
                        ) {
                            RadioButton(
                                selected = option == selectedOption,
                                onClick = { onSelectOption(option) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = options.getOrDefault(option, ""))
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun MasterPasswordDialogPreview() {
    NextcloudPasswordsTheme {
        MasterPasswordDialog(
            masterPassword = "",
            setMasterPassword = {},
            savePassword = false,
            setSavePassword = {},
            onOkClick = {}
        )
    }
}

@Preview
@Composable
fun LogOutDialogPreview() {
    NextcloudPasswordsTheme {
        LogOutDialog {

        }
    }
}

@Preview
@Composable
fun DeleteDialogPreview() {
    NextcloudPasswordsTheme {
        DeleteElementDialog {

        }
    }
}

@Preview
@Composable
fun AddFieldDialogPreview() {
    NextcloudPasswordsTheme {
        AddCustomFieldDialog(onAddClick = { _, _ -> })
    }
}

@Preview
@Composable
fun AddElementDialogPreview() {
    NextcloudPasswordsTheme {
        AddElementDialog({}, {})
    }
}

@Preview
@Composable
fun InputPasscodePreview() {
    NextcloudPasswordsTheme {
        InputPasscodeDialog(title = "Input passcode", onInputPasscode = {})
    }
}

@Preview
@Composable
fun ListPreferenceDialogPreview() {
    NextcloudPasswordsTheme {
        ListPreferenceDialog(
            title = { Text("Language") },
            options = mapOf(
                "ES" to "Spanish",
                "EN" to "English",
                "CA" to "Catalan"
            ),
            selectedOption = "CA",
            onSelectOption = {}
        )
    }
}