package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.ui.theme.Amber200
import com.hegocre.nextcloudpasswords.ui.theme.Amber500
import com.hegocre.nextcloudpasswords.ui.theme.ContentAlpha
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.ui.theme.isLight

class EditablePasswordState(originalPassword: Password?) {
    var password by mutableStateOf(originalPassword?.password ?: "")
    var label by mutableStateOf(originalPassword?.label ?: "")
    var username by mutableStateOf(originalPassword?.username ?: "")
    var url by mutableStateOf(originalPassword?.url ?: "")
    var notes by mutableStateOf(originalPassword?.notes ?: "")
    var customFields by mutableStateOf(originalPassword?.customFields ?: "")
    var favorite by mutableStateOf(originalPassword?.favorite ?: false)

    fun isValid(): Boolean {
        if (label.isBlank())
            return false
        if (password.isBlank())
            return false
        if (!url.isValidURL())
            return false
        for (customField in customFields) {
            when (customField.type) {
                CustomField.TYPE_URL -> {
                    if (!customField.value.isValidURL())
                        return false
                }

                CustomField.TYPE_EMAIL -> {
                    if (!customField.value.isValidEmail())
                        return false
                }
            }
        }
        return true
    }

    companion object {
        val Saver: Saver<EditablePasswordState, *> = listSaver(
            save = {
                listOf(
                    it.password, it.label, it.username, it.url,
                    it.notes, it.customFields, it.favorite.toString()
                )
            },
            restore = {
                EditablePasswordState(null).apply {
                    password = it[0]
                    label = it[1]
                    username = it[2]
                    url = it[3]
                    notes = it[4]
                    customFields = it[5]
                    favorite = it[6].toBooleanStrictOrNull() ?: false
                }
            }
        )
    }
}

@Composable
fun rememberEditablePasswordState(password: Password? = null): EditablePasswordState =
    rememberSaveable(password, saver = EditablePasswordState.Saver) {
        EditablePasswordState(password)
    }

@Composable
fun EditablePasswordView(
    editablePasswordState: EditablePasswordState,
    isUpdating: Boolean,
    onSavePassword: () -> Unit,
    onDeletePassword: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(all = 16.dp),
    ) {
        Button(
            onClick = { editablePasswordState.favorite = !editablePasswordState.favorite },
            modifier = Modifier.padding(bottom = 16.dp),
            colors = if (editablePasswordState.favorite) ButtonDefaults.filledTonalButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
                containerColor = (if (MaterialTheme.colorScheme.isLight()) Amber500 else Amber200)
                    .copy(alpha = 0.3f)
            )
            else ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f),
                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            ),
        ) {
            Icon(imageVector = Icons.Default.Star, contentDescription = null)
            Text(
                text = stringResource(id = R.string.favorite),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        OutlinedTextField(
            value = editablePasswordState.label,
            onValueChange = { newText -> editablePasswordState.label = newText },
            label = { Text(text = stringResource(id = R.string.label)) },
            singleLine = true,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = editablePasswordState.username,
            onValueChange = { newText -> editablePasswordState.username = newText },
            label = { Text(text = stringResource(id = R.string.username)) },
            singleLine = true,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        var showPassword by rememberSaveable {
            mutableStateOf(false)
        }

        OutlinedTextField(
            value = editablePasswordState.password,
            onValueChange = { newText -> editablePasswordState.password = newText },
            label = { Text(text = stringResource(id = R.string.password)) },
            singleLine = true,
            maxLines = 1,
            trailingIcon = {
                Row {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "generate random"
                        )
                    }

                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword)
                                Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(R.string.show_password)
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
            visualTransformation = if (showPassword)
                VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )

        OutlinedTextField(
            value = editablePasswordState.url,
            onValueChange = { newText -> editablePasswordState.url = newText },
            label = { Text(text = stringResource(id = R.string.url)) },
            singleLine = true,
            maxLines = 1,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = editablePasswordState.notes,
            onValueChange = { newText -> editablePasswordState.notes = newText },
            label = { Text(text = stringResource(id = R.string.notes)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        Button(
            onClick = onSavePassword,
            content = {
                if (isUpdating) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(text = stringResource(id = R.string.save))
                }
            },
            enabled = !isUpdating,
            modifier = Modifier.fillMaxWidth()
        )

        if (onDeletePassword != null) {
            var showDeleteDialog by rememberSaveable {
                mutableStateOf(false)
            }

            if (!isUpdating) {
                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                        disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = ContentAlpha.medium)
                    ),
                    content = {
                        Text(text = stringResource(id = R.string.delete_password))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            if (showDeleteDialog) {
                DeleteElementDialog(
                    onConfirmButton = {
                        showDeleteDialog = false
                        onDeletePassword()
                    },
                    onDismissRequest = {
                        showDeleteDialog = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.ime))

    }
}

@Preview
@Composable
fun PasswordEditPreview() {
    NextcloudPasswordsTheme {
        Surface {
            EditablePasswordView(
                editablePasswordState = rememberEditablePasswordState(),
                isUpdating = false,
                onSavePassword = { },
                onDeletePassword = {}
            )
        }
    }
}