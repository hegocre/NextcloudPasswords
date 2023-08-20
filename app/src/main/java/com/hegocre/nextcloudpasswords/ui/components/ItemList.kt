package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.twotone.Lock
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.data.folder.Folder
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.ui.components.pullrefresh.PullRefreshIndicator
import com.hegocre.nextcloudpasswords.ui.components.pullrefresh.pullRefresh
import com.hegocre.nextcloudpasswords.ui.components.pullrefresh.rememberPullRefreshState
import com.hegocre.nextcloudpasswords.ui.theme.Amber200
import com.hegocre.nextcloudpasswords.ui.theme.Amber500
import com.hegocre.nextcloudpasswords.ui.theme.ContentAlpha
import com.hegocre.nextcloudpasswords.ui.theme.Green200
import com.hegocre.nextcloudpasswords.ui.theme.Green500
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.ui.theme.Red200
import com.hegocre.nextcloudpasswords.ui.theme.Red500
import com.hegocre.nextcloudpasswords.ui.theme.isLight
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers

data class ListDecryptionState<T>(
    val decryptedList: List<T>? = null,
    val isLoading: Boolean = false
)

@Composable
fun RefreshListBody(
    isRefreshing: Boolean,
    onRefresh: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
        content()

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun MixedLazyColumn(
    passwords: List<Password>? = null,
    folders: List<Folder>? = null,
    onPasswordClick: ((Password) -> Unit)? = null,
    onPasswordLongClick: ((Password) -> Unit)? = null,
    onFolderClick: ((Folder) -> Unit)? = null,
    onFolderLongClick: ((Folder) -> Unit)? = null,
) {
    val context = LocalContext.current
    val shouldShowIcon by PreferencesManager.getInstance(context).getShowIcons()
        .collectAsState(initial = false, context = Dispatchers.IO)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        folders?.let {
            items(items = it, key = { folder -> folder.id }) { folder ->
                FolderRow(
                    folder = folder,
                    onFolderClick = onFolderClick,
                    onFolderLongClick = onFolderLongClick
                )
            }
        }
        passwords?.let {
            items(items = it, key = { password -> password.id }) { folder ->
                PasswordRow(
                    password = folder,
                    shouldShowIcon = shouldShowIcon,
                    onPasswordClick = onPasswordClick,
                    onPasswordLongClick = onPasswordLongClick
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PasswordRow(
    password: Password,
    modifier: Modifier = Modifier,
    shouldShowIcon: Boolean = false,
    onPasswordClick: ((Password) -> Unit)? = null,
    onPasswordLongClick: ((Password) -> Unit)? = null,
) {
    ListItem(
        modifier = modifier
            .combinedClickable(
                onClick = {
                    onPasswordClick?.invoke(password)
                },
                onLongClick = {
                    onPasswordLongClick?.invoke(password)
                }
            ),
        headlineContent = {
            Text(
                text = password.label,
            )
        },
        supportingContent = if (password.username.isNotBlank()) {
            {
                Text(
                    text = password.username,
                )
            }
        } else null,
        trailingContent = if (password.status != 3) {
            {
                Icon(
                    imageVector = Icons.TwoTone.Security,
                    contentDescription = stringResource(id = R.string.security_status),
                    modifier = Modifier
                        .size(40.dp)
                        .padding(all = 8.dp),
                    tint = (if (MaterialTheme.colorScheme.isLight()) {
                        when (password.status) {
                            0 -> Green500
                            1 -> Amber500
                            2 -> Red500
                            else -> Color.Unspecified
                        }
                    } else {
                        when (password.status) {
                            0 -> Green200
                            1 -> Amber200
                            2 -> Red200
                            else -> Color.Unspecified
                        }
                    })
                )
            }
        } else null,
        leadingContent = if (shouldShowIcon) {
            {
                val context = LocalContext.current
                val imageBitmap by password.faviconBitmap.collectAsState()
                if (imageBitmap == null && !password.isFaviconLoading) {
                    LaunchedEffect(Unit) {
                        password.loadFavicon(context)
                    }
                }
                Crossfade(
                    targetState = imageBitmap,
                    animationSpec = tween(250),
                    label = "Icon"
                ) { image ->
                    if (image == null) {
                        Image(
                            modifier = Modifier
                                .size(45.dp)
                                .padding(8.dp),
                            imageVector = Icons.TwoTone.Lock,
                            contentDescription = stringResource(R.string.site_favicon),
                            colorFilter = ColorFilter.tint(
                                MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = ContentAlpha.medium
                                )
                            )
                        )
                    } else {
                        imageBitmap?.let {
                            Image(
                                modifier = Modifier
                                    .size(45.dp)
                                    .padding(all = 8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                bitmap = it,
                                contentDescription = stringResource(R.string.site_favicon)
                            )
                        }
                    }
                }
            }
        } else null,

        )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderRow(
    folder: Folder,
    modifier: Modifier = Modifier,
    onFolderClick: ((Folder) -> Unit)? = null,
    onFolderLongClick: ((Folder) -> Unit)? = null,
) {
    ListItem(
        leadingContent = {
            Image(
                imageVector = Icons.Filled.Folder,
                contentDescription = stringResource(R.string.folder_icon),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.medium)),
                modifier = Modifier
                    .size(45.dp)
                    .padding(8.dp)
            )
        },
        headlineContent = {
            Text(text = folder.label)
        },
        modifier = modifier
            .combinedClickable(
                onClick = {
                    onFolderClick?.invoke(folder)
                },
                onLongClick = {
                    onFolderLongClick?.invoke(folder)
                }
            )
    )
}

@Preview
@Composable
fun PasswordRowPreview() {
    NextcloudPasswordsTheme {
        PasswordRow(
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
            shouldShowIcon = true
        )
    }
}

@Preview
@Composable
fun FolderRowPreview() {
    NextcloudPasswordsTheme {
        FolderRow(
            folder = Folder(
                id = "",
                label = "Management",
                parent = "00000000-0000-0000-0000-000000000000",
                revision = "",
                cseType = "",
                cseKey = "",
                sseType = "",
                client = "",
                hidden = false,
                trashed = false,
                favorite = false,
                created = 0,
                updated = 0,
                edited = 0
            )
        )
    }
}