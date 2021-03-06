package com.hegocre.nextcloudpasswords.ui.components

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.activities.SettingsActivity
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme

@Composable
fun NCPSearchTopBar(
    title: String = stringResource(R.string.app_name),
    searchQuery: String = "",
    setSearchQuery: (String) -> Unit = {},
    isAutofill: Boolean = false,
    onLogoutClick: () -> Unit = {},
    searchExpanded: Boolean = false,
    onSearchClick: () -> Unit = {},
    onSearchCloseClick: () -> Unit = {}
) {
    Surface(
        elevation = if (MaterialTheme.colors.isLight) AppBarDefaults.TopAppBarElevation else 0.dp
    ) {
        Crossfade(targetState = searchExpanded) { expanded ->
            if (expanded) {
                SearchAppBar(
                    searchQuery = searchQuery,
                    setSearchQuery = setSearchQuery,
                    onBackPressed = onSearchCloseClick
                )
            } else {
                TitleAppBar(
                    title = title,
                    onSearchClick = onSearchClick,
                    onLogoutClick = onLogoutClick,
                    showMenu = !isAutofill
                )
            }
        }
    }
}

@Composable
fun TitleAppBar(
    title: String,
    onSearchClick: () -> Unit,
    onLogoutClick: () -> Unit,
    showMenu: Boolean
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    TopAppBar(
        title = { Text(text = title) },
        backgroundColor = MaterialTheme.colors.background,
        elevation = 0.dp,
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search"
                )
            }
            if (showMenu) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        offset = DpOffset(0.dp, -(56).dp),
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        val context = LocalContext.current
                        DropdownMenuItem(onClick = {
                            val intent = Intent(context, SettingsActivity::class.java)
                            context.startActivity(intent)
                            menuExpanded = false
                        }) {
                            Text(text = stringResource(id = R.string.settings))
                        }

                        DropdownMenuItem(onClick = {
                            onLogoutClick()
                            menuExpanded = false
                        }) {
                            Text(text = stringResource(R.string.log_out))
                        }
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchAppBar(
    searchQuery: String,
    setSearchQuery: (String) -> Unit,
    onBackPressed: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val requester = FocusRequester()

    TopAppBar(
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.background
    ) {
        IconButton(
            onClick = onBackPressed
        ) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "back")
        }
        TextField(
            modifier = Modifier
                .weight(1f)
                .focusRequester(requester),
            value = searchQuery,
            onValueChange = setSearchQuery,
            maxLines = 1,
            singleLine = true,
            placeholder = { Text(text = stringResource(R.string.search)) },
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.background,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
            })
        )
        IconButton(
            onClick = { setSearchQuery("") }
        ) {
            Icon(imageVector = Icons.Default.Clear, contentDescription = "clear")
        }
    }

    SideEffect {
        requester.requestFocus()
    }
}

@Preview(name = "Top bar")
@Composable
fun TopBarPreview() {
    NextcloudPasswordsTheme {
        NCPSearchTopBar()
    }
}