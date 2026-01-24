package com.hegocre.nextcloudpasswords.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.api.Server
import com.hegocre.nextcloudpasswords.data.user.UserController
import com.hegocre.nextcloudpasswords.ui.NCPScreen
import com.hegocre.nextcloudpasswords.ui.activities.LoginActivity
import com.hegocre.nextcloudpasswords.ui.theme.ContentAlpha
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.ui.viewmodels.PasswordsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NCPAccountsScreen(
    onBackPressed: () -> Unit,
    lifecycleScope: LifecycleCoroutineScope? = null,
    passwordsViewModel: PasswordsViewModel = viewModel(),
    onLogoLongPressed: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var servers by remember { mutableStateOf<Set<Server>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var serverForContextMenu by remember { mutableStateOf<Server?>(null) }

    fun loadServers() {
        isLoading = true
        errorMessage = null
        try {
            servers = UserController.getInstance(context).getServers()
        } catch (e: Exception) {
            errorMessage = "Failed to load accounts: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(key1 = Unit) {
        loadServers()
    }

    NextcloudPasswordsTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = stringResource(id = R.string.screen_manage_accounts))
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.navigation_back)
                            )
                        }
                    },
                    windowInsets = WindowInsets.statusBars
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(),
                    exit = scaleOut(),
                ) {
                    FloatingActionButton(
                        onClick = {
                            val intent = Intent(context, LoginActivity::class.java)
                            context.startActivity(intent)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(id = R.string.action_create_element)
                        )
                    }
                }
            },
            contentWindowInsets = WindowInsets.systemBars
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isLoading || errorMessage != null || servers.isEmpty()) Arrangement.Center else Arrangement.Top
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                } else if (servers.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        servers.forEach { server ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                lifecycleScope?.launch {
                                                    val userController =
                                                        UserController.getInstance(context)
                                                    userController.setActiveServer(server)
                                                    loadServers()
                                                    passwordsViewModel.sync()
                                                }
                                            },
                                            onLongClick = {
                                                serverForContextMenu = server
                                            }
                                        )
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(all = 8.dp)
                                            .padding(end = 12.dp)
                                    ) {
                                        Image(
                                            painter = passwordsViewModel.getPainterForAvatar(
                                                server
                                            ),
                                            contentDescription = "",
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .size(40.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = server.username,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Start
                                        )

                                        CompositionLocalProvider(
                                            LocalContentColor provides LocalContentColor.current.copy(
                                                alpha = ContentAlpha.medium
                                            )
                                        ) {
                                            Text(
                                                text = server.url,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                    if (server.isLoggedIn()) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = stringResource(R.string.logged_in_status),
                                            tint = Color.Green,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = serverForContextMenu == server,
                                    onDismissRequest = { serverForContextMenu = null }
                                ) {
                                    DropdownMenuItem(
                                        enabled = !server.isLoggedIn(),
                                        text = {
                                            Text(
                                                stringResource(
                                                    R.string.delete_account_entry,
                                                    server.username
                                                )
                                            )
                                        },
                                        onClick = {
                                            serverForContextMenu = null
                                            try {
                                                UserController.getInstance(context)
                                                    .removeServer(server)
                                                loadServers()
                                            } catch (e: Exception) {
                                                errorMessage =
                                                    "Failed to delete ${server.username}: ${e.localizedMessage}"
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.no_account_logged_in),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}