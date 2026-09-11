package com.hegocre.nextcloudpasswords.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.autofill.contentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
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
import com.hegocre.nextcloudpasswords.data.password.RequestedPassword
import com.hegocre.nextcloudpasswords.ui.theme.ContentAlpha
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.utils.OTP
import com.hegocre.nextcloudpasswords.utils.OtpParseException
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.apache.commons.codec.binary.Base32

@OptIn(ExperimentalComposeUiApi::class)
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
                LaunchedEffect(key1 = Unit) {
                    coroutineContext.job.invokeOnCompletion {
                        if (it?.cause == null) {
                            requester.requestFocus()
                        }
                    }
                }

                OutlinedTextFieldWithCaption(
                    text = masterPassword,
                    onValueChange = setMasterPassword,
                    visualTransformation = if (showPassword)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardType = KeyboardType.Password,
                    label = stringResource(R.string.dialog_master_password_title),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword)
                                    Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = stringResource(R.string.text_input_show_password_toggle)
                            )
                        }
                    },
                    errorText = errorText,
                    modifier = Modifier
                        .focusRequester(requester)
                        .contentType(ContentType.Password)
                )

                CompositionLocalProvider(
                    LocalContentColor provides LocalContentColor.current.copy(alpha = ContentAlpha.medium)
                ) {
                    Row {
                        Checkbox(
                            checked = savePassword,
                            onCheckedChange = setSavePassword,
                            modifier = Modifier.align(CenterVertically)
                        )
                        Text(
                            text = stringResource(R.string.save_password),
                            modifier = Modifier
                                .align(CenterVertically)
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
        title = { Text(text = stringResource(R.string.action_log_out)) },
        text = { Text(text = stringResource(R.string.dialog_log_out_text)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmButton()
                    onDismissRequest?.invoke()
                }
            ) {
                Text(text = stringResource(R.string.action_log_out))
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
        title = { Text(text = stringResource(R.string.action_delete)) },
        text = { Text(text = stringResource(R.string.dialog_delete_element_text)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmButton()
                    onDismissRequest?.invoke()
                }
            ) {
                Text(text = stringResource(R.string.action_delete))
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
fun DiscardChangesDialog(
    onDismissRequest: (() -> Unit)? = null,
    onConfirmButton: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismissRequest?.invoke() },
        title = { Text(text = stringResource(R.string.action_discard)) },
        text = { Text(text = stringResource(R.string.dialog_discard_changes_text)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmButton()
                    onDismissRequest?.invoke()
                }
            ) {
                Text(text = stringResource(R.string.action_discard))
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
        CustomField.TYPE_TEXT to stringResource(id = R.string.custom_field_type_text),
        CustomField.TYPE_EMAIL to stringResource(id = R.string.custom_field_type_email),
        CustomField.TYPE_URL to stringResource(id = R.string.custom_field_type_url),
        CustomField.TYPE_SECRET to stringResource(id = R.string.custom_field_type_secret)
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
                    text = stringResource(id = R.string.action_add_custom_field),
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
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            value = types[type] ?: "",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                            label = { Text(text = stringResource(id = R.string.custom_field_type)) },
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
                        label = { Text(text = stringResource(id = R.string.custom_field_label)) },
                        isError = showEmptyError && label.isBlank(),
                        supportingText = if (showEmptyError && label.isBlank()) {
                            {
                                Text(text = stringResource(id = R.string.error_field_cannot_be_empty))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOtpDialog(
    onSaveClick: (OTP) -> Unit,
    onDeleteClick: () -> Unit,
    onDismissRequest: (() -> Unit)? = null,
    currentOtp: OTP = OTP(secret = "")
) {
    val types = mapOf(
        OTP.Companion.Type.TOTP to stringResource(R.string.otp_type_totp),
        OTP.Companion.Type.HOTP to stringResource(R.string.otp_type_hotp)
    )

    val algorithms = mapOf(
        OTP.Companion.Algorithm.SHA1 to "${OTP.Companion.Algorithm.SHA1.uppercase()} (${stringResource(R.string.value_default)})",
        OTP.Companion.Algorithm.SHA256 to OTP.Companion.Algorithm.SHA256.uppercase(),
        OTP.Companion.Algorithm.SHA512 to OTP.Companion.Algorithm.SHA512.uppercase()
    )

    val (secret, setSecret) = remember { mutableStateOf(currentOtp.secret) }
    val (type, setType) = remember { mutableStateOf(currentOtp.type) }
    val (algorithm, setAlgorithm) = remember { mutableStateOf(currentOtp.algorithm) }
    val (digits, setDigits) = remember { mutableStateOf(currentOtp.digits.toString()) }
    val (counter, setCounter) = remember { mutableStateOf(currentOtp.counter.toString()) }
    val (period, setPeriod) = remember { mutableStateOf(currentOtp.period.toString()) }

    var typeMenuExpanded by remember { mutableStateOf(false) }
    var algorithmMenuExpanded by remember { mutableStateOf(false) }

    var showInputErrors by rememberSaveable {
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
                Row (modifier = Modifier.padding(bottom = 8.dp), verticalAlignment = CenterVertically) {
                    val context = LocalContext.current
                    val resources = LocalResources.current
                    val scanQrCodeLauncher = rememberLauncherForActivityResult(ScanQRCode()) { result ->
                        when (result) {
                            is QRResult.QRSuccess -> {
                                val otpUri = result.content.rawValue
                                if (otpUri != null) {
                                    try {
                                        val otp = OTP.fromUrl(otpUri)
                                        setSecret(otp.secret)
                                        setType(otp.type)
                                        setAlgorithm(otp.algorithm)
                                        setDigits(otp.digits.toString())
                                        setCounter(otp.counter.toString())
                                        setPeriod(otp.period.toString())
                                    } catch (e: OtpParseException) {
                                        Toast.makeText(context, resources.getString(e.stringResId), Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            is QRResult.QRError -> {
                                Toast.makeText(context, result.exception.localizedMessage, Toast.LENGTH_LONG).show()
                            }
                            is QRResult.QRUserCanceled, is QRResult.QRMissingPermission -> {}
                        }
                    }

                    Text(
                        text = stringResource(R.string.otp_title),
                        style = MaterialTheme.typography.headlineSmall
                    )

                    IconButton(
                        onClick = {
                            scanQrCodeLauncher.launch(null)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = stringResource(R.string.scan_qr_code)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {
                    var showSecret by rememberSaveable { mutableStateOf(false) }
                    OutlinedTextField(
                        value = secret,
                        onValueChange = setSecret,
                        singleLine = true,
                        maxLines = 1,
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily(Font(R.font.dejavu_sans_mono))),
                        label = { Text(text = stringResource(R.string.otp_secret)) },
                        isError = showInputErrors && (secret.isBlank() || !Base32().isInAlphabet(secret)),
                        supportingText = if (showInputErrors && secret.isBlank()) {
                            {
                                Text(text = stringResource(id = R.string.error_field_cannot_be_empty))
                            }
                        } else if (showInputErrors && !Base32().isInAlphabet(secret)) {
                            {
                                Text(text = stringResource(R.string.error_invalid_secret))
                            }
                        } else null,
                        visualTransformation = if (showSecret)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showSecret = !showSecret }) {
                                Icon(
                                    imageVector = if (showSecret)
                                        Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = stringResource(R.string.text_input_show_secret_toggle)
                                )
                            }
                        }
                    )

                    var showAdvancedOptions by rememberSaveable { mutableStateOf(false) }

                    Row (modifier = Modifier
                        .padding(top = 16.dp, bottom = 8.dp)
                        .clickable(onClick = { showAdvancedOptions = !showAdvancedOptions })
                    ) {
                        Text(text = stringResource(R.string.show_advanced_options))

                        Icon(
                            imageVector = if (showAdvancedOptions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.toggle_advanced_options)
                        )
                    }

                    if (showAdvancedOptions) {
                        ExposedDropdownMenuBox(
                            expanded = typeMenuExpanded,
                            onExpandedChange = { typeMenuExpanded = !typeMenuExpanded },
                            modifier = Modifier.padding(bottom = 0.dp, top = 16.dp)
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                value = types[type] ?: "",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                                label = { Text(text = stringResource(R.string.otp_type)) },
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

                        ExposedDropdownMenuBox(
                            expanded = algorithmMenuExpanded,
                            onExpandedChange = { algorithmMenuExpanded = !algorithmMenuExpanded },
                            modifier = Modifier.padding(bottom = 0.dp, top = 16.dp)
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                value = algorithms[algorithm] ?: "",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = algorithmMenuExpanded) },
                                label = { Text(text = stringResource(R.string.otp_algorithm)) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )

                            ExposedDropdownMenu(
                                expanded = algorithmMenuExpanded,
                                onDismissRequest = { algorithmMenuExpanded = false }
                            ) {
                                algorithms.forEach { algorithm ->
                                    DropdownMenuItem(
                                        text = { Text(text = algorithm.value) },
                                        onClick = {
                                            setAlgorithm(algorithm.key)
                                            algorithmMenuExpanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            modifier = Modifier.padding(bottom = 0.dp, top = 16.dp),
                            value = digits,
                            onValueChange = { if (it.toIntOrNull() != null || it.isEmpty()) setDigits(it) },
                            singleLine = true,
                            maxLines = 1,
                            label = { Text(text = stringResource(R.string.otp_digits)) },
                            placeholder = { Text(text = "6") },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            isError = showInputErrors && (digits.toIntOrNull() ?: 6) !in 6..9,
                            supportingText = if (showInputErrors && (digits.toIntOrNull() ?: 6) !in 6..9) {
                                {
                                    Text(text = stringResource(id = R.string.otp_invalid_digit_range_error))
                                }
                            } else null
                        )

                        if (type == OTP.Companion.Type.HOTP) {
                            OutlinedTextField(
                                modifier = Modifier.padding(bottom = 8.dp, top = 16.dp),
                                value = counter,
                                onValueChange = { if ((it.toLongOrNull() != null && it.toLong() >= 0) || it.isEmpty()) setCounter(it) },
                                singleLine = true,
                                maxLines = 1,
                                label = { Text(text = stringResource(R.string.otp_counter)) },
                                placeholder = { Text(text = "0") },
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                isError = showInputErrors && (counter.toLongOrNull() ?: 30L) < 0,
                                supportingText = if (showInputErrors && (counter.toLongOrNull() ?: 30L) < 0) {
                                    {
                                        Text(text = stringResource(id = R.string.otp_invalid_counter_error))
                                    }
                                } else null
                            )
                        }

                        if (type == OTP.Companion.Type.TOTP) {
                            OutlinedTextField(
                                modifier = Modifier.padding(bottom = 8.dp, top = 16.dp),
                                value = period,
                                onValueChange = { if ((it.toIntOrNull() != null && it.toInt() >= 0) || it.isEmpty()) setPeriod(it) },
                                singleLine = true,
                                maxLines = 1,
                                label = { Text(text = stringResource(R.string.otp_period)) },
                                placeholder = { Text(text = "30") },
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                isError = showInputErrors && (period.toIntOrNull() ?: 30) < 1,
                                supportingText = if (showInputErrors && (period.toIntOrNull() ?: 30) < 1) {
                                    {
                                        Text(text = stringResource(id = R.string.otp_invalid_period_error))
                                    }
                                } else null
                            )
                        }
                    }
                }


                Row (modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)) {
                    TextButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(text = stringResource(R.string.action_delete))
                    }

                    TextButton(
                        onClick = {
                            if (secret.isBlank() ||
                                !Base32().isInAlphabet(secret) ||
                                (digits.toIntOrNull() ?: 6) !in 6..9 ||
                                (type == OTP.Companion.Type.TOTP && (period.toIntOrNull() ?: 30) < 1) ||
                                (type == OTP.Companion.Type.HOTP && (counter.toLongOrNull() ?: 30L) < 0)
                            ) {
                                showInputErrors = true
                            } else {
                                onSaveClick(
                                    OTP(secret,
                                        type,
                                        algorithm,
                                        digits.toIntOrNull() ?: 6,
                                        counter.toLongOrNull() ?: 0L,
                                        period.toIntOrNull() ?: 30
                                    )
                                )
                            }
                        },
                    ) {
                        Text(text = stringResource(android.R.string.ok))
                    }
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
                        stringResource(id = R.string.top_level_folder_name)
                    } else {
                        selectedFolder?.label ?: stringResource(id = R.string.top_level_folder_name)
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
                                            contentDescription = stringResource(R.string.content_description_folder_icon),
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
                    Text(text = stringResource(R.string.action_select))
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
                    text = stringResource(id = R.string.action_create_element),
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
    val requester = remember { FocusRequester() }

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

                LaunchedEffect(key1 = Unit) {
                    coroutineContext.job.invokeOnCompletion {
                        if (it?.cause == null) {
                            requester.requestFocus()
                        }
                    }
                }

                OutlinedTextField(
                    modifier = Modifier
                        .padding(bottom = 16.dp, top = 8.dp)
                        .focusRequester(requester),
                    value = passcode,
                    onValueChange = { newPasscode ->
                        if (newPasscode.length <= 16 &&
                            (newPasscode.toIntOrNull() != null || newPasscode.isEmpty())
                        ) {
                            setPasscode(newPasscode)
                        }
                    },
                    singleLine = true,
                    maxLines = 1,
                    label = { Text(text = stringResource(id = R.string.passcode)) },
                    isError = showEmptyError && passcode.isBlank(),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = { showPasscode = !showPasscode }) {
                            Icon(
                                imageVector = if (showPasscode)
                                    Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = stringResource(R.string.text_input_show_password_toggle)
                            )
                        }
                    },
                    visualTransformation = if (showPasscode)
                        VisualTransformation.None else PasswordVisualTransformation(),
                )


                TextButton(
                    onClick = {
                        if (passcode.length < 4 || passcode.toIntOrNull() == null) {
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
                            verticalAlignment = CenterVertically,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGenerationDialog(
    onGenerate: (Int, Boolean, Boolean) -> Unit,
    onDismissRequest: (() -> Unit)? = null
) {
    val strengthValues = mapOf(
        RequestedPassword.STRENGTH_ULTRA to stringResource(id = R.string.password_strength_ultra),
        RequestedPassword.STRENGTH_HIGH to stringResource(id = R.string.password_strength_high),
        RequestedPassword.STRENGTH_MEDIUM to stringResource(id = R.string.password_strength_medium),
        RequestedPassword.STRENGTH_STANDARD to stringResource(id = R.string.password_strength_standard),
        RequestedPassword.STRENGTH_LOW to stringResource(id = R.string.password_strength_low)
    )

    val (strength, setStrength) = remember { mutableIntStateOf(RequestedPassword.STRENGTH_STANDARD) }
    val (includeDigits, setIncludeDigits) = remember { mutableStateOf(true) }
    val (includeSymbols, setIncludeSymbols) = remember { mutableStateOf(true) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val lastValues = PreferencesManager.getInstance(context)
            .getPasswordGenerationOptions()?.split(";") ?: listOf()
        setStrength(lastValues.getOrNull(0)?.toIntOrNull() ?: strength)
        setIncludeDigits(lastValues.getOrNull(1)?.toBooleanStrictOrNull() ?: includeDigits)
        setIncludeSymbols(lastValues.getOrNull(2)?.toBooleanStrictOrNull() ?: includeSymbols)
    }

    var typeMenuExpanded by remember { mutableStateOf(false) }

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
                    Text(
                        text = stringResource(id = R.string.action_generate_password),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                ) {
                    ExposedDropdownMenuBox(
                        expanded = typeMenuExpanded,
                        onExpandedChange = { typeMenuExpanded = !typeMenuExpanded },
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 8.dp)
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            value = strengthValues[strength] ?: "",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                            label = { Text(text = stringResource(id = R.string.password_generation_strength)) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )

                        ExposedDropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false }
                        ) {
                            strengthValues.forEach { strengthValue ->
                                DropdownMenuItem(
                                    text = { Text(text = strengthValue.value) },
                                    onClick = {
                                        setStrength(strengthValue.key)
                                        typeMenuExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                setIncludeDigits(!includeDigits)
                            }
                            .padding(vertical = 4.dp, horizontal = 12.dp)
                    ) {
                        Checkbox(
                            checked = includeDigits,
                            onCheckedChange = setIncludeDigits
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(id = R.string.password_generation_include_numbers))
                    }

                    Row(
                        verticalAlignment = CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                setIncludeSymbols(!includeSymbols)
                            }
                            .padding(vertical = 4.dp, horizontal = 12.dp)
                    ) {
                        Checkbox(
                            checked = includeSymbols,
                            onCheckedChange = setIncludeSymbols
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(id = R.string.password_generation_include_special_characters))
                    }
                }

                val coroutineScope = rememberCoroutineScope()
                TextButton(
                    onClick = {
                        onGenerate(strength, includeDigits, includeSymbols)
                        coroutineScope.launch(Dispatchers.IO) {
                            PreferencesManager.getInstance(context).setPasswordGenerationOptions(
                                "$strength;$includeDigits;$includeSymbols"
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 24.dp)
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

@Preview
@Composable
fun GeneratePasswordDialogPreview() {
    NextcloudPasswordsTheme {
        PasswordGenerationDialog(onGenerate = { _, _, _ -> })
    }
}

@Preview
@Composable
fun EditOtpDialogPreview() {
    NextcloudPasswordsTheme {
        EditOtpDialog(onSaveClick = {}, onDeleteClick = {})
    }
}