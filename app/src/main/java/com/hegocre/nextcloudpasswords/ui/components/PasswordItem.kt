package com.hegocre.nextcloudpasswords.ui.components

import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.twotone.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.ui.theme.Yellow200
import com.hegocre.nextcloudpasswords.ui.theme.Yellow500
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
        style = MaterialTheme.typography.h5,
        modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
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
                style = MaterialTheme.typography.h5,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    imageVector = if (password.favorite)
                        Icons.Filled.Star else Icons.TwoTone.Star,
                    contentDescription = "favorite",
                    tint = if (MaterialTheme.colors.isLight) Yellow500 else Yellow200
                )
            }
        }
        CompositionLocalProvider(
            LocalContentAlpha provides ContentAlpha.medium
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
                text = if (showPassword) password.password else "???".repeat(password.password.length),
                icon = {
                    Icon(imageVector = Icons.TwoTone.Password, contentDescription = "password")
                },
                trailingIcon = {
                    IconButton(onClick = {
                        context.copyToClipboard(password.password)
                        Toast.makeText(context, toastPasswordText, Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.TwoTone.ContentCopy, contentDescription = "copy")
                    }
                },
                onClickText = { showPassword = !showPassword }
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
    onClickText: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        icon()
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

