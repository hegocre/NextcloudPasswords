package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.api.FoldersApi
import com.hegocre.nextcloudpasswords.data.folder.Folder
import com.hegocre.nextcloudpasswords.ui.theme.Amber200
import com.hegocre.nextcloudpasswords.ui.theme.Amber500
import com.hegocre.nextcloudpasswords.ui.theme.ContentAlpha
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.ui.theme.isLight

class EditableFolderState(originalFolder: Folder?) {
    var label by mutableStateOf(originalFolder?.label ?: "")
    var parent by mutableStateOf(originalFolder?.parent ?: FoldersApi.DEFAULT_FOLDER_UUID)
    var favorite by mutableStateOf(originalFolder?.favorite ?: false)

    fun isValid(): Boolean {
        return label.isNotBlank()
    }

    companion object {
        val Saver: Saver<EditableFolderState, *> = listSaver(
            save = {
                listOf(
                    it.label, it.parent, it.favorite.toString()
                )
            },
            restore = {
                EditableFolderState(null).apply {
                    label = it[1]
                    parent = it[2]
                    favorite = it[6].toBooleanStrictOrNull() ?: false
                }
            }
        )
    }
}

@Composable
fun rememberEditableFolderState(folder: Folder? = null): EditableFolderState =
    rememberSaveable(folder, saver = EditableFolderState.Saver) {
        EditableFolderState(folder)
    }

@Composable
fun EditableFolderView(
    editableFolderState: EditableFolderState,
    folders: List<Folder>,
    isUpdating: Boolean,
    onSaveFolder: () -> Unit,
    onDeleteFolder: (() -> Unit)? = null
) {
    var showDeleteDialog by rememberSaveable {
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
            Button(
                onClick = { editableFolderState.favorite = !editableFolderState.favorite },
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 16.dp),
                colors = if (editableFolderState.favorite) ButtonDefaults.filledTonalButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    containerColor = (if (MaterialTheme.colorScheme.isLight()) Amber500 else Amber200)
                        .copy(alpha = 0.3f)
                )
                else ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f),
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = stringResource(id = R.string.favorite)
                )
                Text(
                    text = stringResource(id = R.string.favorite),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        item(key = "folder_label") {
            OutlinedTextField(
                value = editableFolderState.label,
                onValueChange = { newText -> editableFolderState.label = newText },
                label = { Text(text = stringResource(id = R.string.label)) },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 16.dp),
                isError = showFieldErrors && editableFolderState.label.isBlank(),
                supportingText = if (showFieldErrors && editableFolderState.label.isBlank()) {
                    {
                        Text(text = stringResource(id = R.string.field_cannot_be_empty))
                    }
                } else null
            )
        }

        item(key = "folder_parent") {
            CompositionLocalProvider(
                LocalTextSelectionColors provides TextSelectionColors(
                    Color.Transparent,
                    Color.Transparent
                ),
                LocalTextInputService provides null
            ) {
                OutlinedTextField(
                    value = if (editableFolderState.parent == FoldersApi.DEFAULT_FOLDER_UUID) {
                        stringResource(id = R.string.home)
                    } else {
                        folders.firstOrNull { it.id == editableFolderState.parent }?.label
                            ?: stringResource(id = R.string.home)
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

        item(key = "folder_save") {
            Button(
                onClick = {
                    if (!editableFolderState.isValid()) {
                        showFieldErrors = true
                    } else {
                        onSaveFolder()
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
                        Text(text = stringResource(id = R.string.save))
                    }
                },
                enabled = !isUpdating,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        if (onDeleteFolder != null) {
            item(key = "folder_delete") {
                if (!isUpdating) {
                    Button(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                            disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = ContentAlpha.medium)
                        ),
                        content = {
                            Text(text = stringResource(id = R.string.delete_folder))
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
                    .windowInsetsBottomHeight(WindowInsets.ime)
                    .padding(bottom = 16.dp)
            )
        }

    }

    if (showDeleteDialog) {
        DeleteElementDialog(
            onConfirmButton = {
                showDeleteDialog = false
                onDeleteFolder?.invoke()
            },
            onDismissRequest = {
                showDeleteDialog = false
            }
        )
    }

    if (showFolderDialog) {
        SelectFolderDialog(
            folders = folders,
            currentFolder = editableFolderState.parent,
            onSelectClick = { folder ->
                editableFolderState.parent = folder
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
fun FolderEditPreview() {
    NextcloudPasswordsTheme {
        Surface {
            EditableFolderView(
                editableFolderState = rememberEditableFolderState(),
                folders = listOf(),
                isUpdating = false,
                onSaveFolder = { },
                onDeleteFolder = { },
            )
        }
    }
}