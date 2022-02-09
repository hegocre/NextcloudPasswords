package com.hegocre.nextcloudpasswords.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.twotone.Favorite
import androidx.compose.material.icons.twotone.Folder
import androidx.compose.material.icons.twotone.VpnKey
import androidx.compose.ui.graphics.vector.ImageVector
import com.hegocre.nextcloudpasswords.R

enum class NCPScreen(
    @StringRes val title: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Passwords(
        title = R.string.passwords,
        selectedIcon = Icons.Filled.VpnKey,
        unselectedIcon = Icons.TwoTone.VpnKey
    ),
    Favorites(
        title = R.string.favorites,
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.TwoTone.Favorite
    ),
    Folders(
        title = R.string.folders,
        selectedIcon = Icons.Filled.Folder,
        unselectedIcon = Icons.TwoTone.Folder
    );

    companion object {
        fun fromRoute(route: String?): NCPScreen =
            when (route?.substringBefore("/")) {
                Passwords.name -> Passwords
                Favorites.name -> Favorites
                Folders.name -> Folders
                null -> Passwords
                else -> throw IllegalArgumentException("Route $route is not recognized.")
            }
    }
}