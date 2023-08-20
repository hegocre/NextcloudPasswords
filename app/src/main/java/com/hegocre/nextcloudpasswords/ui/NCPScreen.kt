package com.hegocre.nextcloudpasswords.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.ui.graphics.vector.ImageVector
import com.hegocre.nextcloudpasswords.R

enum class NCPScreen(
    @StringRes val title: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val hidden: Boolean = false
) {
    Passwords(
        title = R.string.passwords,
        selectedIcon = Icons.Filled.VpnKey,
        unselectedIcon = Icons.Outlined.VpnKey
    ),
    Favorites(
        title = R.string.favorites,
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder
    ),
    Folders(
        title = R.string.folders,
        selectedIcon = Icons.Filled.Folder,
        unselectedIcon = Icons.Outlined.Folder
    ),
    PasswordEdit(
        title = R.string.edit_password,
        selectedIcon = Icons.Default.Edit,
        unselectedIcon = Icons.Default.Edit,
        hidden = true
    ),
    FolderEdit(
        title = R.string.edit_folder,
        selectedIcon = Icons.Default.Edit,
        unselectedIcon = Icons.Default.Edit,
        hidden = true
    );

    companion object {
        fun fromRoute(route: String?): NCPScreen =
            when (route?.substringBefore("/")) {
                Passwords.name -> Passwords
                Favorites.name -> Favorites
                Folders.name -> Folders
                PasswordEdit.name -> PasswordEdit
                FolderEdit.name -> FolderEdit
                null -> Passwords
                else -> throw IllegalArgumentException("Route $route is not recognized.")
            }
    }
}