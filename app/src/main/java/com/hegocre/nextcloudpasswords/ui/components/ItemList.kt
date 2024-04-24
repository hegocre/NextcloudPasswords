package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.data.folder.Folder
import com.hegocre.nextcloudpasswords.data.password.Password
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.ui.theme.statusBreached
import com.hegocre.nextcloudpasswords.ui.theme.statusGood
import com.hegocre.nextcloudpasswords.ui.theme.statusWeak
import com.hegocre.nextcloudpasswords.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers

data class ListDecryptionState<T>(
    val decryptedList: List<T>? = null,
    val isLoading: Boolean = false
)

@Composable
fun MixedLazyColumn(
    passwords: List<Password>? = null,
    folders: List<Folder>? = null,
    onPasswordClick: ((Password) -> Unit)? = null,
    onPasswordLongClick: ((Password) -> Unit)? = null,
    onFolderClick: ((Folder) -> Unit)? = null,
    onFolderLongClick: ((Folder) -> Unit)? = null,
    getPainterForUrl: (@Composable (String) -> Painter)? = null
) {
    val context = LocalContext.current
    val shouldShowIcon by PreferencesManager.getInstance(context).getShowIcons()
        .collectAsState(initial = false, context = Dispatchers.IO)
    val listState = rememberLazyListState()
    val knobRatio by remember {
        derivedStateOf { (10f / ((passwords?.size ?: 0) + (folders?.size ?: 0))).coerceIn(0f, 1f) }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (knobRatio == 1f) Modifier else Modifier.scrollbar(
                    state = listState,
                    horizontal = false,
                    visibleAlpha = 0.5f,
                    fixedKnobRatio = knobRatio,
                    knobCornerRadius = 0.dp
                )
            ),
        state = listState
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
                    onPasswordLongClick = onPasswordLongClick,
                    getPainterForUrl = getPainterForUrl
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
    getPainterForUrl: (@Composable (String) -> Painter)? = null
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
                    contentDescription = stringResource(id = R.string.password_attr_security_status),
                    modifier = Modifier
                        .size(40.dp)
                        .padding(all = 8.dp),
                    tint = (when (password.status) {
                        0 -> MaterialTheme.colorScheme.statusGood
                        1 -> MaterialTheme.colorScheme.statusWeak
                        2 -> MaterialTheme.colorScheme.statusBreached
                        else -> Color.Unspecified
                    })
                )
            }
        } else null,
        leadingContent = if (shouldShowIcon) {
            {
                getPainterForUrl?.let {
                    Image(
                        painter = getPainterForUrl(password.url.ifBlank { password.label }),
                        modifier = Modifier
                            .size(45.dp)
                            .padding(all = 8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentDescription = stringResource(R.string.content_description_site_favicon)
                    )
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
                contentDescription = stringResource(R.string.content_description_folder_icon),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
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

// Scrollbars added from https://stackoverflow.com/questions/66341823/jetpack-compose-scrollbars/71932181#71932181
@Composable
fun Modifier.scrollbar(
    state: LazyListState,
    horizontal: Boolean,
    alignEnd: Boolean = true,
    thickness: Dp = 4.dp,
    fixedKnobRatio: Float? = null,
    knobCornerRadius: Dp = 4.dp,
    trackCornerRadius: Dp = 2.dp,
    knobColor: Color = MaterialTheme.colorScheme.onSurface,
    padding: Dp = 0.dp,
    visibleAlpha: Float = 1f,
    hiddenAlpha: Float = 0f,
    fadeInAnimationDurationMs: Int = 150,
    fadeOutAnimationDurationMs: Int = 500,
    fadeOutAnimationDelayMs: Int = 1000,
): Modifier {
    check(thickness > 0.dp) { "Thickness must be a positive integer." }
    check(fixedKnobRatio == null || fixedKnobRatio < 1f) {
        "A fixed knob ratio must be smaller than 1."
    }
    check(knobCornerRadius >= 0.dp) { "Knob corner radius must be greater than or equal to 0." }
    check(trackCornerRadius >= 0.dp) { "Track corner radius must be greater than or equal to 0." }
    check(hiddenAlpha <= visibleAlpha) { "Hidden alpha cannot be greater than visible alpha." }
    check(fadeInAnimationDurationMs >= 0) {
        "Fade in animation duration must be greater than or equal to 0."
    }
    check(fadeOutAnimationDurationMs >= 0) {
        "Fade out animation duration must be greater than or equal to 0."
    }
    check(fadeOutAnimationDelayMs >= 0) {
        "Fade out animation delay must be greater than or equal to 0."
    }

    val targetAlpha =
        if (state.isScrollInProgress) {
            visibleAlpha
        } else {
            hiddenAlpha
        }
    val animationDurationMs =
        if (state.isScrollInProgress) {
            fadeInAnimationDurationMs
        } else {
            fadeOutAnimationDurationMs
        }
    val animationDelayMs =
        if (state.isScrollInProgress) {
            0
        } else {
            fadeOutAnimationDelayMs
        }

    val alpha by
    animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec =
        tween(delayMillis = animationDelayMs, durationMillis = animationDurationMs),
        label = "alpha"
    )

    return drawWithContent {
        drawContent()

        state.layoutInfo.visibleItemsInfo.firstOrNull()?.let { firstVisibleItem ->
            if (state.isScrollInProgress || alpha > 0f) {
                // Size of the viewport, the entire size of the scrollable composable we are decorating with
                // this scrollbar.
                val viewportSize =
                    if (horizontal) {
                        size.width
                    } else {
                        size.height
                    } - padding.toPx() * 2

                // The size of the first visible item. We use this to estimate how many items can fit in the
                // viewport. Of course, this works perfectly when all items have the same size. When they
                // don't, the scrollbar knob size will grow and shrink as we scroll.
                val firstItemSize = firstVisibleItem.size

                // The *estimated* size of the entire scrollable composable, as if it's all on screen at
                // once. It is estimated because it's possible that the size of the first visible item does
                // not represent the size of other items. This will cause the scrollbar knob size to grow
                // and shrink as we scroll, if the item sizes are not uniform.
                val estimatedFullListSize = firstItemSize * state.layoutInfo.totalItemsCount

                // The difference in position between the first pixels visible in our viewport as we scroll
                // and the top of the fully-populated scrollable composable, if it were to show all the
                // items at once. At first, the value is 0 since we start all the way to the top (or start
                // edge). As we scroll down (or towards the end), this number will grow.
                val viewportOffsetInFullListSpace =
                    state.firstVisibleItemIndex * firstItemSize + state.firstVisibleItemScrollOffset

                // Where we should render the knob in our composable.
                val knobPosition =
                    (viewportSize / estimatedFullListSize) * viewportOffsetInFullListSpace + padding.toPx()
                // How large should the knob be.
                val knobSize =
                    fixedKnobRatio?.let { it * viewportSize }
                        ?: ((viewportSize * viewportSize) / estimatedFullListSize)

                // Draw the knob
                drawRoundRect(
                    color = knobColor,
                    topLeft =
                    when {
                        // When the scrollbar is horizontal and aligned to the bottom:
                        horizontal && alignEnd -> Offset(
                            knobPosition,
                            size.height - thickness.toPx()
                        )
                        // When the scrollbar is horizontal and aligned to the top:
                        horizontal && !alignEnd -> Offset(knobPosition, 0f)
                        // When the scrollbar is vertical and aligned to the end:
                        alignEnd -> Offset(size.width - thickness.toPx(), knobPosition)
                        // When the scrollbar is vertical and aligned to the start:
                        else -> Offset(0f, knobPosition)
                    },
                    size =
                    if (horizontal) {
                        Size(knobSize, thickness.toPx())
                    } else {
                        Size(thickness.toPx(), knobSize)
                    },
                    alpha = alpha,
                    cornerRadius = CornerRadius(
                        x = knobCornerRadius.toPx(),
                        y = knobCornerRadius.toPx()
                    ),
                )
            }
        }
    }
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