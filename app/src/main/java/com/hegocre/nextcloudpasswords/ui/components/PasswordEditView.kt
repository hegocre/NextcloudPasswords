package com.hegocre.nextcloudpasswords.ui.components

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.api.FoldersApi
import com.hegocre.nextcloudpasswords.data.folder.Folder
import com.hegocre.nextcloudpasswords.data.password.CustomField
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.ui.theme.ContentAlpha
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.ui.theme.favoriteColor
import com.hegocre.nextcloudpasswords.utils.isValidEmail
import com.hegocre.nextcloudpasswords.utils.isValidURL
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.KSuspendFunction3

class EditablePasswordState(originalPassword: Password?) {
    var password by mutableStateOf(originalPassword?.password ?: "")
    var label by mutableStateOf(originalPassword?.label ?: "")
    var username by mutableStateOf(originalPassword?.username ?: "")
    var url by mutableStateOf(originalPassword?.url ?: "")
    var notes by mutableStateOf(originalPassword?.notes ?: "")
    var folder by mutableStateOf(originalPassword?.folder ?: FoldersApi.DEFAULT_FOLDER_UUID)
    var customFields =
        if (originalPassword?.customFields?.isBlank() == true) mutableStateListOf() else
        Json.decodeFromString<List<CustomField>>(originalPassword?.customFields ?: "[]")
            .toMutableStateList()
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
                    it.password, it.label, it.username, it.url, it.notes,
                    it.folder, Json.encodeToString(it.customFields.toList()), it.favorite.toString()
                )
            },
            restore = {
                EditablePasswordState(null).apply {
                    password = it[0]
                    label = it[1]
                    username = it[2]
                    url = it[3]
                    notes = it[4]
                    folder = it[5]
                    customFields =
                        Json.decodeFromString<List<CustomField>>(it[6]).toMutableStateList()
                    favorite = it[7].toBooleanStrictOrNull() ?: false
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
    folders: List<Folder>,
    isUpdating: Boolean,
    onGeneratePassword: KSuspendFunction3<Int, Boolean, Boolean, Deferred<String?>>?,
    onSavePassword: () -> Unit,
    onDeletePassword: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showDeleteDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showAddCustomFieldDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showFolderDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showFieldErrors by rememberSaveable {
        mutableStateOf(false)
    }


    LazyColumn {
        item(key = "top_spacer") { Spacer(modifier = Modifier.width(16.dp)) }

        item(key = "favorite_button") {
            val contentColor by animateColorAsState(
                targetValue = if (editablePasswordState.favorite)
                    MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.80f
                ),
                label = "favoriteContentColor"
            )
            val containerColor by animateColorAsState(
                targetValue = if (editablePasswordState.favorite)
                    MaterialTheme.colorScheme.favoriteColor.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.12f
                ),
                label = "favoriteContentColor"
            )
            Button(
                onClick = { editablePasswordState.favorite = !editablePasswordState.favorite },
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    contentColor = contentColor,
                    containerColor = containerColor
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = stringResource(id = R.string.password_attr_favorite)
                )
                Text(
                    text = stringResource(id = R.string.password_attr_favorite),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        item(key = "password_label") {
            OutlinedTextField(
                value = editablePasswordState.label,
                onValueChange = { newText -> editablePasswordState.label = newText },
                label = { Text(text = stringResource(id = R.string.password_folder_attr_label)) },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 16.dp),
                isError = showFieldErrors && editablePasswordState.label.isBlank(),
                supportingText = if (showFieldErrors && editablePasswordState.label.isBlank()) {
                    {
                        Text(text = stringResource(id = R.string.error_field_cannot_be_empty))
                    }
                } else null
            )
        }

        item(key = "password_username") {
            OutlinedTextField(
                value = editablePasswordState.username,
                onValueChange = { newText -> editablePasswordState.username = newText },
                label = { Text(text = stringResource(id = R.string.password_attr_username)) },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 16.dp)
            )
        }


        item(key = "password_password") {
            var showPassword by rememberSaveable {
                mutableStateOf(false)
            }

            var showGenerateDialog by remember {
                mutableStateOf(false)
            }

            var isGenerating by rememberSaveable {
                mutableStateOf(false)
            }

            OutlinedTextField(
                value = editablePasswordState.password,
                onValueChange = { newText -> editablePasswordState.password = newText },
                label = { Text(text = stringResource(id = R.string.password_attr_password)) },
                singleLine = true,
                maxLines = 1,
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {


                        if (isGenerating) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword)
                                    Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = stringResource(R.string.text_input_show_password_toggle)
                            )
                        }

                        if (onGeneratePassword != null) {
                            IconButton(onClick = {
                                showGenerateDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Casino,
                                    contentDescription = stringResource(id = R.string.action_generate_password)
                                )
                            }
                        }

                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                visualTransformation = if (showPassword)
                    VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 16.dp),
                isError = showFieldErrors && editablePasswordState.password.isBlank(),
                supportingText = if (showFieldErrors && editablePasswordState.password.isBlank()) {
                    {
                        Text(text = stringResource(id = R.string.error_field_cannot_be_empty))
                    }
                } else null
            )

            if (showGenerateDialog) {
                PasswordGenerationDialog(
                    onGenerate = { strength, includeDigits, includeSymbols ->
                        if (onGeneratePassword != null) {
                            coroutineScope.launch {
                                isGenerating = true
                                val generatedPassword = onGeneratePassword(
                                    strength, includeDigits, includeSymbols
                                ).await()
                                if (generatedPassword == null) {
                                    Toast.makeText(
                                        context,
                                        R.string.error_could_not_generate_password,
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    editablePasswordState.password = generatedPassword
                                }
                                isGenerating = false
                            }
                            showGenerateDialog = false
                        }
                    },
                    onDismissRequest = {
                        showGenerateDialog = false
                    }
                )
            }
        }

        item(key = "password_url") {
            OutlinedTextField(
                value = editablePasswordState.url,
                onValueChange = { newText -> editablePasswordState.url = newText },
                label = { Text(text = stringResource(id = R.string.password_attr_url)) },
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 16.dp),
                isError = showFieldErrors && !editablePasswordState.url.isValidURL(),
                supportingText = if (showFieldErrors && !editablePasswordState.url.isValidURL()) {
                    {
                        Text(text = stringResource(id = R.string.error_enter_valid_url))
                    }
                } else null
            )
        }

        item(key = "password_folder") {
            CompositionLocalProvider(
                LocalTextSelectionColors provides TextSelectionColors(
                    Color.Transparent,
                    Color.Transparent
                ),
                LocalTextInputService provides null
            ) {
                OutlinedTextField(
                    value = if (editablePasswordState.folder == FoldersApi.DEFAULT_FOLDER_UUID) {
                        stringResource(id = R.string.top_level_folder_name)
                    } else {
                        folders.firstOrNull { it.id == editablePasswordState.folder }?.label
                            ?: stringResource(id = R.string.top_level_folder_name)
                    },
                    onValueChange = { },
                    label = { Text(text = stringResource(id = R.string.folder)) },
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .padding(horizontal = 16.dp),
                    interactionSource = remember { MutableInteractionSource() }
                        .also { mutableInteractionSource ->
                            LaunchedEffect(key1 = mutableInteractionSource) {
                                mutableInteractionSource.interactions.collect {
                                    if (it is PressInteraction.Release) {
                                        showFolderDialog = true
                                    }
                                }
                            }
                        },
                    colors = OutlinedTextFieldDefaults.colors(cursorColor = Color.Unspecified)
                )
            }
        }

        itemsIndexed(
            items = editablePasswordState.customFields,
            key = { index, field -> "${index}_password_custom_${field.label}" }) { index, customField ->
            var showValue by rememberSaveable {
                mutableStateOf(customField.type != CustomField.TYPE_SECRET)
            }

            OutlinedTextField(
                value = customField.value,
                onValueChange = { newText ->
                    val newElement = editablePasswordState.customFields[index].copy(value = newText)
                    editablePasswordState.customFields.removeAt(index)
                    editablePasswordState.customFields.add(index, newElement)
                },
                label = { Text(text = customField.label) },
                singleLine = true,
                maxLines = 1,
                trailingIcon = {
                    Row {
                        if (customField.type == CustomField.TYPE_SECRET) {
                            IconButton(onClick = { showValue = !showValue }) {
                                Icon(
                                    imageVector = if (showValue)
                                        Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = stringResource(R.string.text_input_show_password_toggle)
                                )
                            }
                        }

                        IconButton(onClick = { editablePasswordState.customFields.removeAt(index) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = when (customField.type) {
                        CustomField.TYPE_SECRET -> KeyboardType.Password
                        CustomField.TYPE_EMAIL -> KeyboardType.Email
                        CustomField.TYPE_URL -> KeyboardType.Uri
                        else -> KeyboardType.Text
                    }
                ),
                visualTransformation = if (showValue)
                    VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 16.dp),
                isError = when (customField.type) {
                    CustomField.TYPE_URL -> showFieldErrors && !customField.value.isValidURL()
                    CustomField.TYPE_EMAIL -> showFieldErrors && !customField.value.isValidEmail()
                    else -> false
                },
                supportingText = when (customField.type) {
                    CustomField.TYPE_URL -> {
                        if (showFieldErrors && !customField.value.isValidURL()) {
                            {
                                Text(text = stringResource(id = R.string.error_enter_valid_url))
                            }
                        } else null
                    }

                    CustomField.TYPE_EMAIL -> {
                        if (showFieldErrors && !customField.value.isValidEmail()) {
                            {
                                Text(text = stringResource(id = R.string.error_enter_valid_email))
                            }
                        } else null
                    }

                    else -> null
                },
            )

        }

        item(key = "password_notes") {
            OutlinedTextField(
                value = editablePasswordState.notes,
                onValueChange = { newText -> editablePasswordState.notes = newText },
                label = { Text(text = stringResource(id = R.string.password_attr_notes)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .padding(horizontal = 16.dp)
            )
        }

        item(key = "custom_field_add") {
            Button(
                onClick = { showAddCustomFieldDialog = true },
                content = {
                    Text(text = stringResource(id = R.string.action_add_custom_field))
                },
                colors = ButtonDefaults.filledTonalButtonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .padding(horizontal = 16.dp)
            )
        }

        item(key = "password_save") {
            Button(
                onClick = {
                    if (!editablePasswordState.isValid()) {
                        showFieldErrors = true
                    } else {
                        onSavePassword()
                    }
                },
                content = {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(text = stringResource(id = R.string.action_save))
                    }
                },
                enabled = !isUpdating,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        if (onDeletePassword != null) {
            item(key = "password_delete") {
                if (!isUpdating) {
                    Button(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                            disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = ContentAlpha.medium)
                        ),
                        content = {
                            Text(text = stringResource(id = R.string.action_delete_password))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(
                modifier = Modifier
                    .windowInsetsBottomHeight(WindowInsets.ime.add(WindowInsets.navigationBars))
                    .padding(bottom = 16.dp)
            )
        }

    }

    if (showDeleteDialog) {
        DeleteElementDialog(
            onConfirmButton = {
                showDeleteDialog = false
                onDeletePassword?.invoke()
            },
            onDismissRequest = {
                showDeleteDialog = false
            }
        )
    }

    if (showAddCustomFieldDialog) {
        AddCustomFieldDialog(
            onAddClick = { type, label ->
                editablePasswordState.customFields.add(
                    CustomField(
                        type = type, label = label, value = ""
                    )
                )
                showAddCustomFieldDialog = false
            },
            onDismissRequest = {
                showAddCustomFieldDialog = false
            }
        )
    }

    if (showFolderDialog) {
        SelectFolderDialog(
            folders = folders,
            currentFolder = editablePasswordState.folder,
            onSelectClick = { folder ->
                editablePasswordState.folder = folder
                showFolderDialog = false
            },
            onDismissRequest = {
                showFolderDialog = false
            }
        )
    }
}

@Preview
@Composable
fun PasswordEditPreview() {
    NextcloudPasswordsTheme {
        Surface {
            EditablePasswordView(
                editablePasswordState = rememberEditablePasswordState().apply {
                    customFields.add(
                        CustomField(
                            type = CustomField.TYPE_TEXT,
                            label = "Custom field 1",
                            value = ""
                        )
                    )
                    customFields.add(
                        CustomField(
                            type = CustomField.TYPE_SECRET,
                            label = "Custom field 2",
                            value = ""
                        )
                    )
                },
                folders = listOf(),
                isUpdating = false,
                onSavePassword = { },
                onDeletePassword = { },
                onGeneratePassword = null
            )
        }
    }
}