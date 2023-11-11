package com.hegocre.nextcloudpasswords.ui.components

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import kotlinx.coroutines.job

object AppBarDefaults {
    val TopAppBarElevation = 4.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NCPSearchTopBar(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.app_name),
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        rememberTopAppBarState()
    ),
    searchQuery: String = "",
    setSearchQuery: (String) -> Unit = {},
    isAutofill: Boolean = false,
    onLogoutClick: () -> Unit = {},
    searchExpanded: Boolean = false,
    onSearchClick: () -> Unit = {},
    onSearchCloseClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier,
    ) {
        Crossfade(targetState = searchExpanded, label = "expanded") { expanded ->
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
                    scrollBehavior = scrollBehavior,
                    showMenu = !isAutofill
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleAppBar(
    title: String,
    onSearchClick: () -> Unit,
    onLogoutClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    showMenu: Boolean
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    LargeTopAppBar(
        title = { Text(text = title) },
        scrollBehavior = scrollBehavior,
        windowInsets = WindowInsets.statusBars,
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = stringResource(id = R.string.search)
                )
            }
            if (showMenu) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(id = R.string.menu)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        offset = DpOffset(0.dp, -(56).dp),
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        val context = LocalContext.current

                        DropdownMenuItem(
                            onClick = {
                                val intent =
                                    Intent("com.hegocre.nextcloudpasswords.action.settings")
                                        .setPackage(context.packageName)
                                context.startActivity(intent)
                                menuExpanded = false
                            },
                            text = {
                                Text(text = stringResource(id = R.string.settings))
                            },
                        )

                        DropdownMenuItem(
                            onClick = {
                                val intent = Intent("com.hegocre.nextcloudpasswords.action.about")
                                    .setPackage(context.packageName)
                                context.startActivity(intent)
                                menuExpanded = false
                            },
                            text = {
                                Text(text = stringResource(id = R.string.about))
                            },
                        )

                        DropdownMenuItem(
                            onClick = {
                                onLogoutClick()
                                menuExpanded = false
                            },
                            text = {
                                Text(text = stringResource(R.string.log_out))
                            }
                        )
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
    val requester = remember { FocusRequester() }

    Column(
        Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(AppBarDefaults.TopAppBarElevation))
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())
        Row(
            modifier = Modifier.height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackPressed
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.back)
                )
            }

            LaunchedEffect(key1 = Unit) {
                coroutineContext.job.invokeOnCompletion {
                    if (it?.cause == null) {
                        requester.requestFocus()
                    }
                }
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
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
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
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(id = R.string.clear_query)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Top bar")
@Composable
fun TopBarPreview() {
    NextcloudPasswordsTheme {
        NCPSearchTopBar()
    }
}

@Preview
@Composable
fun SearchBarPreview() {
    NextcloudPasswordsTheme {
        SearchAppBar(
            searchQuery = "Query",
            setSearchQuery = {},
            onBackPressed = {}
        )
    }
}