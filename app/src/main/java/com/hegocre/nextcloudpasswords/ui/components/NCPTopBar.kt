package com.hegocre.nextcloudpasswords.ui.components

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.theme.ContentAlpha
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.ui.viewmodels.PasswordsViewModel
import kotlinx.coroutines.job

object AppBarDefaults {
    val TopAppBarElevation = 4.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NCPSearchTopBar(
    passwordsViewModel: PasswordsViewModel? = null,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.app_name),
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        rememberTopAppBarState()
    ),
    userAvatar: @Composable (Dp) -> Unit = { size ->
        Image(
            imageVector = Icons.Outlined.AccountCircle,
            contentDescription = passwordsViewModel?.server?.username ?: "",
            modifier = Modifier.size(size)
        )
    },
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
                    passwordsViewModel = passwordsViewModel,
                    title = title,
                    onSearchClick = onSearchClick,
                    onLogoutClick = onLogoutClick,
                    scrollBehavior = scrollBehavior,
                    showMenu = !isAutofill,
                    userAvatar = userAvatar
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleAppBar(
    passwordsViewModel: PasswordsViewModel? = null,
    title: String,
    onSearchClick: () -> Unit,
    onLogoutClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    showMenu: Boolean,
    userAvatar: @Composable (Dp) -> Unit
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
                    contentDescription = stringResource(id = R.string.action_search)
                )
            }
            if (showMenu) {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        userAvatar(26.dp)
                    }

                    PopupAppMenu(
                        username = passwordsViewModel?.server?.username ?: "",
                        serverAddress = passwordsViewModel?.server?.url ?: "",
                        menuExpanded = menuExpanded,
                        userAvatar = userAvatar,
                        onDismissRequest = { menuExpanded = false },
                        onLogoutClick = onLogoutClick
                    )
                }
            }
        },
    )
}

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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.navigation_back)
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
                placeholder = { Text(text = stringResource(R.string.action_search)) },
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
                    contentDescription = stringResource(id = R.string.action_clear_search_query)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopupAppMenu(
    username: String,
    serverAddress: String,
    menuExpanded: Boolean,
    userAvatar: @Composable (Dp) -> Unit,
    onDismissRequest: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current

    val dismissSheet = Modifier
        .pointerInput(Unit) {
            detectTapGestures {
                onDismissRequest()
            }
        }

    val orientation = LocalConfiguration.current.orientation
    val screenHeight = LocalWindowInfo.current.containerSize.height.dp

    if (menuExpanded) {
        BasicAlertDialog(
            onDismissRequest = onDismissRequest,
            modifier = Modifier.fillMaxWidth(),
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    Spacer(
                        modifier = Modifier
                            .height(72.dp)
                            .fillMaxWidth()
                            .then(dismissSheet)
                    )
                }

                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .then(
                            if (orientation == Configuration.ORIENTATION_PORTRAIT)
                                Modifier.fillMaxWidth()
                            else
                                Modifier.width(screenHeight)
                        ),
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(all = 8.dp)
                                    .padding(end = 12.dp)
                            ) {
                                userAvatar(40.dp)
                            }

                            Column {
                                Text(
                                    text = username,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Start
                                )

                                CompositionLocalProvider(
                                    LocalContentColor provides LocalContentColor.current.copy(alpha = ContentAlpha.medium)
                                ) {
                                    Text(
                                        text = serverAddress,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        DropdownMenuItem(
                            onClick = {
                                val intent =
                                    Intent("com.hegocre.nextcloudpasswords.action.settings")
                                        .setPackage(context.packageName)
                                context.startActivity(intent)
                                onDismissRequest()
                            },
                            text = {
                                Text(
                                    text = stringResource(id = R.string.screen_settings),
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = stringResource(id = R.string.screen_settings),
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .padding(start = 16.dp)
                                )
                            }
                        )

                        DropdownMenuItem(
                            onClick = {
                                val intent =
                                    Intent("com.hegocre.nextcloudpasswords.action.accounts")
                                        .setPackage(context.packageName)
                                context.startActivity(intent)
                                onDismissRequest()
                            },
                            text = {
                                Text(
                                    text = stringResource(id = R.string.screen_manage_accounts),
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.ManageAccounts,
                                    contentDescription = stringResource(id = R.string.screen_manage_accounts),
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .padding(start = 16.dp)
                                )
                            }
                        )

                        DropdownMenuItem(
                            onClick = {
                                val intent = Intent("com.hegocre.nextcloudpasswords.action.about")
                                    .setPackage(context.packageName)
                                context.startActivity(intent)
                                onDismissRequest()
                            },
                            text = {
                                Text(
                                    text = stringResource(id = R.string.screen_about),
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = stringResource(id = R.string.screen_about),
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .padding(start = 16.dp)
                                )
                            }
                        )

                        DropdownMenuItem(
                            onClick = {
                                onLogoutClick()
                                onDismissRequest()
                            },
                            text = {
                                Text(
                                    text = stringResource(R.string.action_log_out),
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                                    contentDescription = stringResource(id = R.string.action_log_out),
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .padding(start = 16.dp)
                                )
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        CompositionLocalProvider(
                            LocalContentColor provides LocalContentColor.current.copy(alpha = ContentAlpha.medium)
                        ) {
                            Text(
                                text = "${stringResource(id = R.string.app_name)} v${
                                    stringResource(
                                        id = R.string.version_name
                                    )
                                }(${
                                    stringResource(
                                        id = R.string.version_code
                                    )
                                })",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(dismissSheet)
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