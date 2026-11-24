package com.hegocre.nextcloudpasswords.ui.components

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.utils.AutofillData
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NCPSearchTopBar(
    username: String,
    serverAddress: String,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.app_name),
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        rememberTopAppBarState()
    ),
    userAvatar: @Composable (Dp) -> Unit = { size ->
        Image(
            imageVector = Icons.Outlined.AccountCircle,
            contentDescription = username,
            modifier = Modifier.size(size)
        )
    },
    searchQuery: String = "",
    setSearchQuery: (String) -> Unit = {},
    autofillData: AutofillData? = null,
    onTrashedPasswordsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    searchExpanded: Boolean = false,
    onSearchClick: () -> Unit = {},
    onSearchCloseClick: () -> Unit = {},
) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        SearchPillAppBar(
            searchQuery = searchQuery,
            setSearchQuery = setSearchQuery,
            searchExpanded = searchExpanded,
            onSearchClick = onSearchClick,
            onSearchCloseClick = onSearchCloseClick,
            actions = {
                if (autofillData == null) {
                    AccountMenuButton(
                        username = username,
                        serverAddress = serverAddress,
                        userAvatar = userAvatar,
                        onTrashedPasswordsClick = onTrashedPasswordsClick,
                        onLogoutClick = onLogoutClick
                    )
                }
            }
        )
        CollapsingTitle(title = title, scrollBehavior = scrollBehavior)
    }
}

/**
 * An always visible search field that filters the content below it as the user types, instead of
 * opening a separate results view. [searchExpanded] is true while the user is searching.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPillAppBar(
    searchQuery: String,
    setSearchQuery: (String) -> Unit,
    searchExpanded: Boolean,
    onSearchClick: () -> Unit,
    onSearchCloseClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val searchBarState = rememberSearchBarState(
        initialValue = if (searchExpanded) SearchBarValue.Expanded else SearchBarValue.Collapsed
    )
    val textFieldState = rememberTextFieldState(initialText = searchQuery)

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }.collect { setSearchQuery(it) }
    }
    LaunchedEffect(searchQuery) {
        if (textFieldState.text.toString() != searchQuery) {
            textFieldState.setTextAndPlaceCursorAtEnd(searchQuery)
        }
    }
    LaunchedEffect(searchBarState.currentValue) {
        if (searchBarState.currentValue == SearchBarValue.Expanded) onSearchClick()
    }
    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            searchBarState.animateToExpanded()
        } else {
            searchBarState.animateToCollapsed()
            focusManager.clearFocus()
        }
    }

    // AppBarWithSearch is not used, because it disables the keyboard in its collapsed field and
    // expects a separate expanded results view, while search here filters the list in place
    Row(
        modifier = Modifier
            .windowInsetsPadding(SearchBarDefaults.windowInsets)
            .padding(vertical = 4.dp)
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = SearchBarDefaults.inputFieldShape,
            color = SearchBarDefaults.colors().containerColor,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 8.dp)
        ) {
            SearchBarDefaults.InputField(
                textFieldState = textFieldState,
                searchBarState = searchBarState,
                onSearch = { keyboardController?.hide() },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = stringResource(id = R.string.action_search)) },
                leadingIcon = {
                    if (searchExpanded) {
                        IconButton(onClick = onSearchCloseClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.navigation_back)
                            )
                        }
                    } else {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                    }
                },
                trailingIcon = if (searchExpanded) {
                    {
                        ClearSearchButton(
                            searchQuery = searchQuery,
                            setSearchQuery = setSearchQuery,
                            onSearchCloseClick = onSearchCloseClick
                        )
                    }
                } else null
            )
        }
        actions()
    }
}

@Composable
private fun ClearSearchButton(
    searchQuery: String,
    setSearchQuery: (String) -> Unit,
    onSearchCloseClick: () -> Unit
) {
    // A clear button that does nothing on an empty query reads as a broken close button
    IconButton(
        onClick = { if (searchQuery.isEmpty()) onSearchCloseClick() else setSearchQuery("") }
    ) {
        Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = stringResource(
                id = if (searchQuery.isEmpty()) R.string.action_close_search
                else R.string.action_clear_search_query
            )
        )
    }
}

@Composable
private fun AccountMenuButton(
    username: String,
    serverAddress: String,
    userAvatar: @Composable (Dp) -> Unit,
    onTrashedPasswordsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    Box {
        IconButton(onClick = { menuExpanded = true }) {
            userAvatar(26.dp)
        }

        PopupAppMenu(
            username = username,
            serverAddress = serverAddress,
            menuExpanded = menuExpanded,
            userAvatar = userAvatar,
            onDismissRequest = { menuExpanded = false },
            onTrashedPasswordsClick = onTrashedPasswordsClick,
            onLogoutClick = onLogoutClick
        )
    }
}

/**
 * A large title that scrolls away under the search field, driven by [scrollBehavior] in the same
 * way as the collapsing part of a large top app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollapsingTitle(title: String, scrollBehavior: TopAppBarScrollBehavior) {
    val state = scrollBehavior.state

    Layout(
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 12.dp)
                    .graphicsLayer { alpha = 1f - state.collapsedFraction }
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
    ) { measurables, constraints ->
        val placeable = measurables.single().measure(constraints.copy(minHeight = 0))
        val expandedHeight = placeable.height.toFloat()
        if (state.heightOffsetLimit != -expandedHeight) state.heightOffsetLimit = -expandedHeight

        val height = (expandedHeight + state.heightOffset).roundToInt().coerceIn(0, placeable.height)
        layout(constraints.maxWidth, height) {
            placeable.placeRelative(0, height - placeable.height)
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
    onTrashedPasswordsClick: () -> Unit,
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
                                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
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
                                onTrashedPasswordsClick()
                                onDismissRequest()
                            },
                            text = {
                                Text(
                                    text = stringResource(id = R.string.deleted_passwords),
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(id = R.string.deleted_passwords),
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .padding(start = 16.dp)
                                )
                            }
                        )

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
                            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
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
        NCPSearchTopBar("", "")
    }
}

@Preview
@Composable
fun SearchBarPreview() {
    NextcloudPasswordsTheme {
        SearchPillAppBar(
            searchQuery = "Query",
            setSearchQuery = {},
            searchExpanded = true,
            onSearchClick = {},
            onSearchCloseClick = {}
        )
    }
}