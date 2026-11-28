package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.NCPScreen

/**
 * Password details open in a side pane next to the list on expanded width windows, such as an
 * unfolded foldable, and in a bottom sheet otherwise. Editors always get the full width.
 */
fun showsPasswordDetailPane(windowSizeClass: WindowSizeClass, currentScreen: NCPScreen): Boolean =
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
            && currentScreen != NCPScreen.PasswordEdit
            && currentScreen != NCPScreen.FolderEdit

@Composable
fun PasswordDetailPane(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (contentModifier: Modifier) -> Unit
) {
    // The pane runs to the bottom edge like the list next to it, so only its top corners are rounded
    Surface(
        shape = MaterialTheme.shapes.large.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.End)
            )
            // The top margin lines the pane up with the search field next to it
            .padding(top = 8.dp, end = 16.dp)
            .width(400.dp)
            .fillMaxHeight()
    ) {
        Column {
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(id = R.string.action_close_password_details)
                )
            }
            // The details scroll their own content below a fixed header
            content(
                Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(bottom = 16.dp)
            )
        }
    }
}
