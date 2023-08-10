package com.hegocre.nextcloudpasswords.ui.components

import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.twotone.AccountCircle
import androidx.compose.material.icons.twotone.ContentCopy
import androidx.compose.material.icons.twotone.Link
import androidx.compose.material.icons.twotone.Password
import androidx.compose.material.icons.twotone.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.ui.theme.Amber200
import com.hegocre.nextcloudpasswords.ui.theme.Amber500
import com.hegocre.nextcloudpasswords.ui.theme.ContentAlpha
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.ui.theme.isLight
import com.hegocre.nextcloudpasswords.utils.copyToClipboard

@Composable
fun PasswordItem(
    password: Password?,
    modifier: Modifier = Modifier
) {
    password?.let { pass ->
        PasswordItemContent(password = pass, modifier = modifier)
    } ?: Text(
        text = stringResource(R.string.password),
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun PasswordItemContent(
    password: Password,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val toastUsernameText = stringResource(R.string.username_copied)
    val toastPasswordText = stringResource(R.string.password_copied)
    val toastUrlText = stringResource(R.string.url_copied)

    var showPassword by rememberSaveable { mutableStateOf(false) }
    SideEffect {
        showPassword = false
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = password.label,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    imageVector = if (password.favorite)
                        Icons.Filled.Star else Icons.TwoTone.Star,
                    contentDescription = "favorite",
                    tint = if (MaterialTheme.colorScheme.isLight()) Amber500 else Amber200
                )
            }
        }
        CompositionLocalProvider(
            LocalContentColor provides LocalContentColor.current.copy(alpha = ContentAlpha.medium)
        ) {
            TextLabel(
                text = password.username,
                icon = {
                    Icon(imageVector = Icons.TwoTone.AccountCircle, contentDescription = "username")
                },
                trailingIcon = {
                    IconButton(onClick = {
                        context.copyToClipboard(password.username)
                        Toast.makeText(context, toastUsernameText, Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.TwoTone.ContentCopy, contentDescription = "copy")
                    }
                }
            )
            TextLabel(
                text = if (showPassword) password.password else "●".repeat(password.password.length),
                icon = {
                    Icon(imageVector = Icons.TwoTone.Password, contentDescription = "password")
                },
                trailingIcon = {
                    IconButton(onClick = {
                        context.copyToClipboard(password.password, isSensitive = true)
                        Toast.makeText(context, toastPasswordText, Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.TwoTone.ContentCopy, contentDescription = "copy")
                    }
                },
                onClickText = { showPassword = !showPassword },
                maxLines = if (showPassword) null else 1,
                fontFamily = if (showPassword) FontFamily(Font(R.font.dejavu_sans_mono)) else null
            )
            TextLabel(
                text = password.url,
                icon = {
                    Icon(imageVector = Icons.TwoTone.Link, contentDescription = "url")
                },
                trailingIcon = {
                    IconButton(onClick = {
                        context.copyToClipboard(password.url)
                        Toast.makeText(context, toastUrlText, Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.TwoTone.ContentCopy, contentDescription = "copy")
                    }
                },
                onClickText = if (URLUtil.isValidUrl(password.url)) {
                    {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(password.url)
                        context.startActivity(intent)
                    }
                } else null
            )
        }
    }
}

@Composable
fun TextLabel(
    text: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {},
    trailingIcon: @Composable () -> Unit = {},
    onClickText: (() -> Unit)? = null,
    maxLines: Int? = null,
    fontFamily: FontFamily? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        icon()
        Text(
            text = text,
            maxLines = maxLines ?: Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
            fontFamily = fontFamily,
            modifier = Modifier
                .padding(vertical = 4.dp, horizontal = 16.dp)
                .weight(1f)
                .clickable(
                    enabled = onClickText != null,
                    onClick = onClickText ?: {}
                ),

            )
        trailingIcon()
    }
}

@Preview
@Composable
fun PasswordItemPreview() {
    NextcloudPasswordsTheme {
        Surface {
            PasswordItem(
                password = Password(
                    id = "",
                    label = "Nextcloud",
                    username = "john_doe",
                    password = "secret_value",
                    url = "https://nextcloud.com/",
                    notes = "",
                    customFields = "",
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
                ),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            )
        }
    }
}