package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .padding(end = 16.dp, bottom = 16.dp)
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
            content(Modifier.padding(bottom = 16.dp))
        }
    }
}
