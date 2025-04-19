package com.hegocre.nextcloudpasswords.ui.components

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.api.FoldersApi
import com.hegocre.nextcloudpasswords.data.folder.Folder
import com.hegocre.nextcloudpasswords.ui.theme.ContentAlpha
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.ui.theme.favoriteColor
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

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
                    label = it[0]
                    parent = it[1]
                    favorite = it[2].toBooleanStrictOrNull() ?: false
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
    val coroutineScope = rememberCoroutineScope()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    var showDeleteDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showFolderDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showFieldErrors by rememberSaveable {
        mutableStateOf(false)
    }
    var showDiscardDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var confirmedDiscard by rememberSaveable {
        mutableStateOf(false)
    }

    BackHandler (enabled = !confirmedDiscard) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            onConfirmButton = {
                confirmedDiscard = true
                showDiscardDialog = false
                coroutineScope.launch {
                    awaitFrame()
                    onBackPressedDispatcher?.onBackPressed()
                    confirmedDiscard = false
                }
            },
            onDismissRequest = {
                showDiscardDialog = false
            }
        )
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
                    containerColor = MaterialTheme.colorScheme.favoriteColor.copy(alpha = 0.3f)
                )
                else ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f),
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
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

        item(key = "folder_label") {
            OutlinedTextField(
                value = editableFolderState.label,
                onValueChange = { newText -> editableFolderState.label = newText },
                label = { Text(text = stringResource(id = R.string.password_folder_attr_label)) },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 16.dp),
                isError = showFieldErrors && editableFolderState.label.isBlank(),
                supportingText = if (showFieldErrors && editableFolderState.label.isBlank()) {
                    {
                        Text(text = stringResource(id = R.string.error_field_cannot_be_empty))
                    }
                } else null
            )
        }

        item(key = "folder_parent") {
            OutlinedClickableTextField(
                value = if (editableFolderState.parent == FoldersApi.DEFAULT_FOLDER_UUID) {
                    stringResource(id = R.string.top_level_folder_name)
                } else {
                    folders.firstOrNull { it.id == editableFolderState.parent }?.label
                        ?: stringResource(id = R.string.top_level_folder_name)
                },
                label = stringResource(id = R.string.folder_attr_parent_folder),
                onClick = { showFolderDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 16.dp)
            )
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
                        Text(text = stringResource(id = R.string.action_save))
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
                            Text(text = stringResource(id = R.string.action_delete_folder))
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