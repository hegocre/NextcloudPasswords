package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.data.password.CustomField
import com.hegocre.nextcloudpasswords.ui.theme.ContentAlpha
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme

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
                    errorText = errorText
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
