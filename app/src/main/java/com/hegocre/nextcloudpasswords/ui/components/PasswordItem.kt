package com.hegocre.nextcloudpasswords.ui.components

import android.content.ActivityNotFoundException
import android.webkit.URLUtil
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.twotone.AccountCircle
import androidx.compose.material.icons.twotone.AlternateEmail
import androidx.compose.material.icons.twotone.ContentCopy
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.Link
import androidx.compose.material.icons.twotone.Password
import androidx.compose.material.icons.twotone.Shield
import androidx.compose.material.icons.twotone.Timelapse
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.data.password.CustomField
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.data.password.UpdatedPassword
import com.hegocre.nextcloudpasswords.ui.components.markdown.MDDocument
import com.hegocre.nextcloudpasswords.ui.theme.ContentAlpha
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.ui.theme.favoriteColor
import com.hegocre.nextcloudpasswords.utils.OTP
import com.hegocre.nextcloudpasswords.utils.copyToClipboard
import com.hegocre.nextcloudpasswords.utils.formatOtp
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import org.commonmark.node.Document
import org.commonmark.parser.Parser

@Composable
fun PasswordItem(
    passwordInfo: Pair<Password, List<String>>?,
    modifier: Modifier = Modifier,
    updatePassword: (updatedPassword: UpdatedPassword, onSuccess: () -> Unit, onFailure: () -> Unit) -> Unit,
    onEditPassword: (() -> Unit)? = null,
) {
    passwordInfo?.let { pass ->
        PasswordItemContent(
            passwordInfo = pass,
            onEditPassword = onEditPassword,
            updatePassword = updatePassword,
            modifier = modifier
        )
    } ?: Text(
        text = stringResource(R.string.password),
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun PasswordItemContent(
    passwordInfo: Pair<Password, List<String>>,
    onEditPassword: (() -> Unit)?,
    updatePassword: (updatedPassword: UpdatedPassword, onSuccess: () -> Unit, onFailure: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val copiedText = stringResource(R.string.copied)

    val uriHandler = LocalUriHandler.current

    val password = passwordInfo.first
    val folderPath = remember {
        buildAnnotatedString {
            appendInlineContent("folder")
            append(" ")
            passwordInfo.second.reversed().forEachIndexed { index, folderName ->
                if (index != 0) append(" /")
                append(" $folderName")
            }
        }
    }

    val customFields by remember {
        derivedStateOf {
            if (password.customFields.isNotBlank()) {
                Json.decodeFromString<List<CustomField>>(password.customFields)
            } else {
                listOf()
            }
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(bottom = 4.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = CenterVertically
        ) {
            val favoriteInlineContent = mapOf(
                Pair(
                    "favorite",
                    InlineTextContent(
                        placeholder = Placeholder(
                            width = LocalTextStyle.current.fontSize,
                            height = LocalTextStyle.current.fontSize,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = stringResource(
                                id = R.string.password_attr_favorite
                            ),
                            tint = MaterialTheme.colorScheme.favoriteColor
                        )
                    }
                )
            )
            Text(
                text = buildAnnotatedString {
                    append(password.label)
                    if (password.favorite) {
                        append(" ")
                        appendInlineContent("favorite")
                    }
                },
                inlineContent = favoriteInlineContent,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .weight(1f)
            )
            if (password.editable) {
                onEditPassword?.let {
                    IconButton(onClick = it) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(id = R.string.action_edit_password),
                        )
                    }
                }
            }
        }
        LazyColumn {
            item(key = "${password.id}_path") {
                val folderInlineContent = mapOf(
                    Pair(
                        "folder",
                        InlineTextContent(
                            placeholder = Placeholder(
                                width = LocalTextStyle.current.fontSize,
                                height = LocalTextStyle.current.fontSize,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = stringResource(
                                    id = R.string.folder
                                ),
                                tint = MaterialTheme.colorScheme.onSurface
                                    .copy(alpha = ContentAlpha.medium)
                            )
                        }
                    )
                )
                Text(
                    text = folderPath,
                    inlineContent = folderInlineContent,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .padding(horizontal = 16.dp)
                )
            }

            if (password.username.isNotBlank()) {
                item(key = "${password.id}_username") {
                    val usernameLabel = stringResource(id = R.string.password_attr_username)

                    PasswordTextField(
                        text = password.username,
                        label = usernameLabel,
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.AccountCircle,
                                contentDescription = stringResource(id = R.string.password_attr_username)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                context.copyToClipboard(password.username)
                                Toast.makeText(
                                    context,
                                    String.format(copiedText, usernameLabel),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(
                                    imageVector = Icons.TwoTone.ContentCopy,
                                    contentDescription = stringResource(id = R.string.action_copy_value)
                                )
                            }
                        }
                    )
                }
            }

            item(key = "${password.id}_password") {
                var showPassword by rememberSaveable { mutableStateOf(false) }

                val passwordLabel = stringResource(id = R.string.password_attr_password)

                PasswordTextField(
                    text = if (showPassword) password.password else "●".repeat(password.password.length),
                    label = passwordLabel,
                    icon = {
                        Icon(
                            imageVector = Icons.TwoTone.Password,
                            contentDescription = stringResource(id = R.string.password_attr_password)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            context.copyToClipboard(password.password, isSensitive = true)
                            Toast.makeText(
                                context,
                                String.format(copiedText, passwordLabel),
                                Toast.LENGTH_SHORT
                            ).show()
                        }) {
                            Icon(
                                imageVector = Icons.TwoTone.ContentCopy,
                                contentDescription = stringResource(id = R.string.action_copy_value)
                            )
                        }
                    },
                    onClickText = { showPassword = !showPassword },
                    fontFamily = FontFamily(Font(R.font.dejavu_sans_mono))
                )
            }

            val customFieldOtp by derivedStateOf { customFields.find { it.label == OTP.CUSTOM_FIELD_LABEL } }
            customFieldOtp?.let { otpField ->
                item(key = "${password.id}_otp") {
                    var otp by remember(otpField) {
                        mutableStateOf(
                            try {
                                Json.decodeFromString<OTP>(otpField.value)
                            } catch (_: Exception) {
                                null
                            }
                        )
                    }

                    otp?.let { otpNotNull ->
                        var currentOtp by remember(otpNotNull) { mutableStateOf(otpNotNull.getCurrent()) }
                        currentOtp.first?.let { code ->
                            var progress by remember { mutableStateOf<Float?>(null) }

                            val resources = LocalResources.current

                            PasswordOtpField(
                                otp = code,
                                label = stringResource(R.string.otp_title),
                                progress = progress,
                                onGenerateNext = if (currentOtp.second == null && otpNotNull.type == OTP.Companion.Type.HOTP) {
                                    {
                                        val newOtp = otpNotNull.getNext()
                                        otp = newOtp

                                        customFields.indexOfFirst { it.label == OTP.CUSTOM_FIELD_LABEL }.let { otpIndex ->
                                            if (otpIndex != -1) {
                                                val newCustomFields = customFields.toMutableList()
                                                newCustomFields[otpIndex] = CustomField(
                                                    label = OTP.CUSTOM_FIELD_LABEL,
                                                    type = CustomField.TYPE_DATA,
                                                    value = Json.encodeToString(newOtp),
                                                )
                                                val encodedCustomFields = Json.encodeToString(newCustomFields)

                                                val updatedPassword = UpdatedPassword(
                                                    id = password.id,
                                                    revision = password.revision,
                                                    password = password.password,
                                                    label = password.label,
                                                    username = password.username,
                                                    url = password.url,
                                                    notes = password.notes,
                                                    customFields = encodedCustomFields,
                                                    hash = password.hash,
                                                    cseType = "none",
                                                    cseKey = "",
                                                    folder = password.folder,
                                                    edited = password.edited,
                                                    hidden = password.hidden,
                                                    favorite = password.favorite
                                                )

                                                updatePassword(
                                                    updatedPassword,
                                                    {},
                                                    {
                                                        Toast.makeText(
                                                            context,
                                                            resources.getString(R.string.error_could_not_sync_counter),
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else null
                            )

                            currentOtp.second?.let { endTimeInMillis ->
                                if (otpNotNull.type == OTP.Companion.Type.TOTP) {
                                    LaunchedEffect(endTimeInMillis) {
                                        while (System.currentTimeMillis() < endTimeInMillis) {
                                            val remaining = endTimeInMillis - System.currentTimeMillis()
                                            progress = 1f - (remaining.toFloat() / (otpNotNull.period.toFloat() * 1000f))
                                            delay(timeMillis = 50L)
                                        }
                                        currentOtp = otpNotNull.getCurrent()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (password.url.isNotBlank()) {
                item(key = "${password.id}_url") {
                    val urlLabel = stringResource(id = R.string.password_attr_url)

                    PasswordTextField(
                        text = password.url,
                        label = urlLabel,
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.Link,
                                contentDescription = stringResource(id = R.string.password_attr_url)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                context.copyToClipboard(password.url)
                                Toast.makeText(
                                    context,
                                    String.format(copiedText, urlLabel),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(
                                    imageVector = Icons.TwoTone.ContentCopy,
                                    contentDescription = stringResource(id = R.string.action_copy_value)
                                )
                            }
                        },
                        onClickText = if (URLUtil.isValidUrl(password.url)) {
                            {
                                try {
                                    uriHandler.openUri(password.url)
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(
                                        context,
                                        R.string.error_could_not_open_url,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } else if (URLUtil.isValidUrl("https://${password.url}")) {
                            {
                                try {
                                    uriHandler.openUri("https://${password.url}")
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(
                                        context,
                                        R.string.error_could_not_open_url,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } else null
                    )
                }
            }

            val customFieldsNoOtp by derivedStateOf { customFields.filterNot { it.label == OTP.CUSTOM_FIELD_LABEL } }
            if (customFieldsNoOtp.isNotEmpty()) {
                itemsIndexed(
                    items = customFieldsNoOtp,
                    key = { index, field -> "${index}_${password.id}_${field.label}" }) {_, customField ->
                    when (customField.type) {
                        CustomField.TYPE_TEXT, CustomField.TYPE_EMAIL -> {
                            PasswordTextField(
                                text = customField.value,
                                label = customField.label,
                                icon = {
                                    if (customField.type == CustomField.TYPE_TEXT) {
                                        Icon(
                                            imageVector = Icons.TwoTone.Info,
                                            contentDescription = stringResource(id = R.string.custom_field_type_text)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.TwoTone.AlternateEmail,
                                            contentDescription = stringResource(id = R.string.custom_field_type_email)
                                        )
                                    }
                                },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        context.copyToClipboard(customField.value, isSensitive = customField.isSensitive)
                                        Toast.makeText(
                                            context,
                                            String.format(copiedText, customField.label),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }) {
                                        Icon(
                                            imageVector = Icons.TwoTone.ContentCopy,
                                            contentDescription = stringResource(id = R.string.action_copy_value)
                                        )
                                    }
                                }
                            )
                        }

                        CustomField.TYPE_SECRET -> {
                            var showSecret by rememberSaveable { mutableStateOf(false) }

                            PasswordTextField(
                                text = if (showSecret) customField.value else
                                    "●".repeat(customField.value.length),
                                label = customField.label,
                                icon = {
                                    Icon(
                                        imageVector = Icons.TwoTone.Shield,
                                        contentDescription = stringResource(id = R.string.custom_field_type_secret)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        context.copyToClipboard(customField.value, isSensitive = customField.isSensitive)
                                        Toast.makeText(
                                            context,
                                            String.format(copiedText, customField.label),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }) {
                                        Icon(
                                            imageVector = Icons.TwoTone.ContentCopy,
                                            contentDescription = stringResource(id = R.string.action_copy_value)
                                        )
                                    }
                                },

                                onClickText = { showSecret = !showSecret },
                                fontFamily = FontFamily(Font(R.font.dejavu_sans_mono))
                            )
                        }

                        CustomField.TYPE_URL -> {
                            PasswordTextField(
                                text = customField.value,
                                label = customField.label,
                                icon = {
                                    Icon(
                                        imageVector = Icons.TwoTone.Link,
                                        contentDescription = stringResource(id = R.string.password_attr_url)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        context.copyToClipboard(customField.value, isSensitive = customField.isSensitive)
                                        Toast.makeText(
                                            context,
                                            String.format(copiedText, customField.label),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }) {
                                        Icon(
                                            imageVector = Icons.TwoTone.ContentCopy,
                                            contentDescription = stringResource(id = R.string.action_copy_value)
                                        )
                                    }
                                },
                                onClickText = if (URLUtil.isValidUrl(customField.value)) {
                                    {
                                        try {
                                            uriHandler.openUri(customField.value)
                                        } catch (_: ActivityNotFoundException) {
                                            Toast.makeText(
                                                context,
                                                R.string.error_could_not_open_url,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                } else if (URLUtil.isValidUrl("https://${customField.value}")) {
                                    {
                                        try {
                                            uriHandler.openUri("https://${customField.value}")
                                        } catch (_: ActivityNotFoundException) {
                                            Toast.makeText(
                                                context,
                                                R.string.error_could_not_open_url,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                } else null
                            )
                        }
                    }
                }
            }

            if (password.notes.isNotBlank()) {
                item(key = "${password.id}_notes") {
                    val notesLabel = stringResource(id = R.string.password_attr_notes)

                    PasswordMarkdownField(
                        markdown = password.notes.replace("\n", "\n\n"),
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .padding(horizontal = 4.dp),
                        label = notesLabel
                    )
                }
            }
        }
    }
}

@Composable
fun PasswordTextField(
    text: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {},
    trailingIcon: @Composable () -> Unit = {},
    onClickText: (() -> Unit)? = null,
    maxLines: Int? = null,
    fontFamily: FontFamily? = null,
) {
    ListItem(
        headlineContent = {
            Text(
                text = text,
                maxLines = maxLines ?: Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
                fontFamily = fontFamily,
                modifier = Modifier
                    .clickable(
                        enabled = onClickText != null,
                        onClick = onClickText ?: {}
                    ),
            )
        },
        overlineContent = {
            Text(
                text = label.uppercase(),
                maxLines = 1
            )
        },
        leadingContent = icon,
        trailingContent = trailingIcon,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
    )
}

@Composable
fun PasswordOtpField(
    otp: String,
    label: String,
    progress: Float?,
    onGenerateNext: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onClickText: (() -> Unit)? = null,
    fontFamily: FontFamily? = null,
) {
    ListItem(
        headlineContent = {
            Text(
                text = otp.formatOtp(),
                maxLines = 1,
                fontFamily = fontFamily,
                modifier = Modifier
                    .clickable(
                        enabled = onClickText != null,
                        onClick = onClickText ?: {}
                    ),
            )
        },
        overlineContent = {
            Text(
                text = label.uppercase(),
                maxLines = 1
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.TwoTone.Timelapse,
                contentDescription = stringResource(R.string.otp_title)
            )
        },
        trailingContent = {
            Row {
                onGenerateNext?.let { onGenerateNext ->
                    IconButton(onClick = onGenerateNext) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = stringResource(id = R.string.generate_next_otp_htop)
                        )
                    }
                }

                progress?.let {
                    CircularProgressIndicator(
                        progress = { it },
                        modifier = Modifier
                            .align(CenterVertically)
                            .padding(end = 16.dp)
                            .width(20.dp)
                            .height(20.dp),
                        trackColor = Color.Transparent,
                        strokeWidth = 2.dp
                    )
                }

                val context = LocalContext.current
                val copiedText = stringResource(R.string.copied)
                val otpTitle = stringResource(R.string.otp_title)

                IconButton(onClick = {
                    context.copyToClipboard(otp, isSensitive = true)
                    Toast.makeText(
                        context,
                        String.format(copiedText, otpTitle),
                        Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Icon(
                        imageVector = Icons.TwoTone.ContentCopy,
                        contentDescription = stringResource(id = R.string.action_copy_value)
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
    )
}

@Composable
fun PasswordMarkdownField(
    markdown: String,
    modifier: Modifier = Modifier,
    label: String = "",
) {
    val root = remember(markdown) {
        Parser.builder().build().parse(markdown) as Document
    }
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        if (label.isNotBlank()) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SelectionContainer {
            Column {
                MDDocument(root)
            }
        }
    }
}

@Preview
@Composable
fun PasswordItemPreview() {
    NextcloudPasswordsTheme {
        Surface {
            PasswordItem(
                passwordInfo = Pair(
                    Password(
                        id = "",
                        label = "Nextcloud with a really long label",
                        username = "john_doe",
                        password = "secret_value",
                        url = "https://nextcloud.com/",
                        notes = "# This is a note\n\nIt is very important that this is read by all __means__\n\n" +
                                "## Subsection \n\n This is also important.\n\n" +
                                "## Another subsection\n\n### Even deeper\n\n Some text\nSome more text",
                        customFields = "[{\"label\":\"client.ios.otp\",\"type\":\"data\",\"value\":\"{\\\"secret\\\": \\\"hello\\\"}\"}]",
                        status = 0,
                        statusCode = "GOOD",
                        hash = "",
                        folder = "",
                        revision = "",
                        share = null,
                        shared = false,
                        cseType = "",
                        cseKey = "",
                        sseType = "",
                        client = "",
                        hidden = false,
                        trashed = false,
                        favorite = true,
                        editable = true,
                        edited = 0,
                        created = 0,
                        updated = 0
                    ), listOf("Second", "Home")
                ),
                onEditPassword = {},
                modifier = Modifier.padding(bottom = 16.dp),
                updatePassword = { _, _, _ -> }
            )
        }
    }
}