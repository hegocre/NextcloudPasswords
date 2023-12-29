package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme

data class LicenseNotice(
    val name: String,
    val copyright: String,
    val licenseName: String,
    val licenseUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NCPAboutScreen(
    onBackPressed: () -> Unit,
    onLogoLongPressed: (() -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current

    var showLicensesDialog by rememberSaveable { mutableStateOf(false) }

    NextcloudPasswordsTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = stringResource(id = R.string.about))
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.back)
                            )
                        }
                    },
                    windowInsets = WindowInsets.statusBars
                )
            },
            contentWindowInsets = WindowInsets.systemBars
        ) { paddingValues ->
            Column(
                modifier = Modifier.padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        OutlinedCard(
                            border = CardDefaults.outlinedCardBorder(enabled = false),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        modifier = Modifier
                                            .padding(20.dp)
                                            .height(50.dp)
                                            .width(50.dp)
                                            .clip(CircleShape),
                                        painter = painterResource(id = R.drawable.app_icon),
                                        contentDescription = stringResource(id = R.string.app_name)
                                    )
                                    Text(
                                        text = stringResource(id = R.string.app_name),
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                }
                                AboutTextField(
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = stringResource(id = R.string.version)
                                        )
                                    },
                                    primaryText = { Text(text = stringResource(id = R.string.version)) },
                                    secondaryText = {
                                        Text(
                                            text = "v${stringResource(id = R.string.version_name)} " +
                                                    "(${stringResource(id = R.string.version_code)})"
                                        )
                                    },
                                    onLongClick = {
                                        onLogoLongPressed?.invoke()
                                    }
                                )
                                AboutTextField(
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Code,
                                            contentDescription = stringResource(id = R.string.source_code)
                                        )
                                    },
                                    primaryText = { Text(text = stringResource(id = R.string.source_code)) },
                                    onClick = { uriHandler.openUri(repoUrl) }
                                )

                                AboutTextField(
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Outlined.History,
                                            contentDescription = stringResource(id = R.string.changelog)
                                        )
                                    },
                                    primaryText = { Text(text = stringResource(id = R.string.changelog)) },
                                    onClick = { uriHandler.openUri(changelogUrl) }
                                )

                                AboutTextField(
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Campaign,
                                            contentDescription = stringResource(id = R.string.help_suggestions)
                                        )
                                    },
                                    primaryText = { Text(text = stringResource(id = R.string.help_suggestions)) },
                                    onClick = { uriHandler.openUri("$repoUrl/issues") }
                                )

                                AboutTextField(
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Handshake,
                                            contentDescription = stringResource(id = R.string.contribute)
                                        )
                                    },
                                    primaryText = { Text(text = stringResource(id = R.string.contribute)) },
                                    onClick = { uriHandler.openUri(contributeUrl) }
                                )

                                AboutTextField(
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Description,
                                            contentDescription = stringResource(id = R.string.licenses)
                                        )
                                    },
                                    primaryText = { Text(text = stringResource(id = R.string.licenses)) },
                                    onClick = { showLicensesDialog = true }
                                )

                                AboutTextField(
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Policy,
                                            contentDescription = stringResource(id = R.string.privacy_policy)
                                        )
                                    },
                                    primaryText = { Text(text = stringResource(id = R.string.privacy_policy)) },
                                    onClick = { uriHandler.openUri(policyUrl) }
                                )
                            }
                        }
                    }

                    //Authors card
                    item {
                        OutlinedCard(
                            border = CardDefaults.outlinedCardBorder(enabled = false),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = stringResource(id = R.string.authors),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                )

                                for (author in authors) {
                                    AboutTextField(
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Person,
                                                contentDescription = stringResource(id = R.string.authors)
                                            )
                                        },
                                        primaryText = { Text(text = author.key) },
                                        onClick = { uriHandler.openUri(author.value) }
                                    )
                                }

                                AboutTextField(
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Web,
                                            contentDescription = stringResource(id = R.string.website)
                                        )
                                    },
                                    primaryText = { Text(text = stringResource(id = R.string.website)) },
                                    onClick = { uriHandler.openUri(websiteUrl) }
                                )
                            }
                        }
                    }
                }
            }

            if (showLicensesDialog) {
                LicensesDialog(
                    licenses = licenses,
                    onDismissRequest = { showLicensesDialog = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AboutTextField(
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    primaryText: (@Composable () -> Unit)? = null,
    secondaryText: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(
            LocalContentColor provides LocalContentColor.current.copy(alpha = 0.7f),
        ) {
            icon?.invoke()
        }
        Column(modifier = Modifier.padding(start = if (icon == null) 0.dp else 24.dp)) {
            primaryText?.let { content ->
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodyLarge
                ) {
                    content()
                }
            }
            secondaryText?.let { content ->
                CompositionLocalProvider(
                    LocalContentColor provides LocalContentColor.current.copy(alpha = 0.7f),
                    LocalTextStyle provides MaterialTheme.typography.bodyMedium
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun LicensesDialog(
    licenses: List<LicenseNotice>,
    onDismissRequest: (() -> Unit)? = null
) {

    Dialog(
        onDismissRequest = { onDismissRequest?.invoke() },
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = contentColorFor(backgroundColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(modifier = Modifier.padding(vertical = 24.dp)) {
                Text(
                    text = stringResource(id = R.string.licenses),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .padding(horizontal = 24.dp)
                )

                val uriHandler = LocalUriHandler.current

                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(items = licenses, key = { it.name }) { license ->
                        ListItem(
                            headlineContent = {
                                Text(text = license.name)
                            },
                            overlineContent = if (license.copyright.isNotBlank()) {
                                {
                                    Text(text = license.copyright)
                                }
                            } else null,
                            supportingContent = {
                                Text(text = license.licenseName)
                            },
                            modifier = Modifier
                                .clickable {
                                    uriHandler.openUri(license.licenseUrl)
                                }
                                .padding(horizontal = 10.dp)
                        )
                    }
                }

                TextButton(
                    onClick = { onDismissRequest?.invoke() },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 24.dp)
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        }
    }
}

const val policyUrl = "https://hegocre.com/nextcloudpasswords/privacy.html"
const val repoUrl = "https://github.com/hegocre/NextcloudPasswords"
const val websiteUrl = "https://hegocre.com/"
const val changelogUrl = "https://github.com/hegocre/NextcloudPasswords/releases/latest"
const val contributeUrl = "https://github.com/hegocre/NextcloudPasswords#contribute"
val authors = mapOf(
    "Hector Godoy" to "https://github.com/hegocre",
)
val licenses = listOf(
    LicenseNotice(
        name = "Kotlin Programming Language",
        copyright = "Copyright (C) 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.",
        licenseName = "Apache License 2.0",
        licenseUrl = "https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt"
    ),
    LicenseNotice(
        name = "Android Jetpack",
        copyright = "Copyright (C) 2023 The Android Open Source Project",
        licenseName = "Apache License 2.0",
        licenseUrl = "https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt"
    ),
    LicenseNotice(
        name = "OkHttp",
        copyright = "Copyright (C) 2019 Square, Inc.",
        licenseName = "Apache License 2.0",
        licenseUrl = "https://github.com/square/okhttp/blob/master/LICENSE.txt"
    ),
    LicenseNotice(
        name = "Java Native Access",
        copyright = "",
        licenseName = "Apache License 2.0",
        licenseUrl = "https://github.com/java-native-access/jna/blob/master/AL2.0"
    ),
    LicenseNotice(
        name = "Lazysodium Android",
        copyright = "Copyright (C) 2022 Terl Tech Ltd • goterl.com",
        licenseName = "Mozilla Public License 2.0",
        licenseUrl = "https://github.com/terl/lazysodium-android/blob/master/LICENSE.md"
    ),
    LicenseNotice(
        name = "Markdown Composer",
        copyright = "Copyright (C) 2021 Erik Hellman",
        licenseName = "MIT License",
        licenseUrl = "https://github.com/ErikHellman/MarkdownComposer/blob/master/LICENSE.txt"
    ),
    LicenseNotice(
        name = "MaterialKolor",
        copyright = "Copyright (c) 2023 Jordon de Hoog",
        licenseName = "MIT License",
        licenseUrl = "https://github.com/jordond/MaterialKolor/blob/main/LICENSE"
    ),
)

@Preview
@Composable
fun NCPAboutPreview() {
    NCPAboutScreen({})
}

@Preview
@Composable
fun LicensesDialogPreview() {
    NextcloudPasswordsTheme {
        LicensesDialog(licenses = licenses)
    }
}