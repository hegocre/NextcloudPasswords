package com.hegocre.nextcloudpasswords.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.http.SslError
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import com.hegocre.nextcloudpasswords.utils.PreferencesManager

@Composable
fun NCPLoginScreen(
    loginIntent: Intent,
    onLoginSuccess: () -> Unit,
    onLoginFailed: () -> Unit
) {
    NextcloudPasswordsTheme {
        Scaffold(
            topBar = {
                Spacer(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                )
            },
            bottomBar = {
                Spacer(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                )
            }
        ) { innerPadding ->
            LoginView(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                loginIntent = loginIntent,
                onLoginSuccess = onLoginSuccess,
                onLoginFailed = onLoginFailed
            )
        }
    }
}

@Composable
fun LoginView(
    modifier: Modifier = Modifier,
    loginIntent: Intent,
    onLoginSuccess: () -> Unit,
    onLoginFailed: () -> Unit
) {
    val launchLoginWebView =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (result.data?.getBooleanExtra("loggedIn", false) == true) {
                    onLoginSuccess()
                }
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                if (result.data?.getBooleanExtra("loggedIn", false) == false) {
                    onLoginFailed()
                }
            }
        }

    val (urlText, setUrlText) = remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    val errorMessages = listOf(
        stringResource(R.string.url_cannot_be_empty),
        stringResource(R.string.url_must_start_https)
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LoginCard(
            text = urlText,
            onTextChange = setUrlText,
            errorText = errorText,
            onLoginButtonClick = {
                when {
                    urlText.isBlank() -> {
                        errorText = errorMessages[0]
                    }
                    urlText.startsWith("http://") -> {
                        errorText = errorMessages[1]
                    }
                    else -> {
                        errorText = ""

                        if (!urlText.startsWith("https://"))
                            setUrlText(String.format("https://%s", urlText))

                        loginIntent.putExtra(
                            "login_url",
                            if (urlText.startsWith("https://")) urlText else "https://$urlText"
                        )
                        launchLoginWebView.launch(loginIntent)
                    }
                }
            }
        )
    }
}

@Composable
fun LoginCard(
    text: String,
    onTextChange: (String) -> Unit,
    errorText: String,
    onLoginButtonClick: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .padding(all = 20.dp)
        ) {
            Image(
                modifier = Modifier
                    .height(70.dp)
                    .width(70.dp)
                    .clip(CircleShape)
                    .align(Alignment.CenterHorizontally),
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = stringResource(id = R.string.app_name)
            )

            OutlinedTextFieldWithCaption(
                text = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .padding(vertical = 8.dp),
                label = stringResource(id = R.string.server_url),
                captionText = "${stringResource(R.string.example)}: https://cloud.example.com/",
                errorText = errorText,
                onDone = onLoginButtonClick
            )

            Button(
                modifier = Modifier.align(Alignment.End),
                onClick = onLoginButtonClick
            ) {
                Text(text = stringResource(R.string.login))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NCPWebLoginScreen(
    onLoginUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    url: String = ""
) {
    NextcloudPasswordsTheme {
        val context = LocalContext.current

        var showTlsDialog by rememberSaveable { mutableStateOf(false) }

        var skipTlsValidation by rememberSaveable { mutableStateOf(false) }

        val (title, setTitle) = rememberSaveable {
            mutableStateOf(url)
        }

        val webViewClient = remember(skipTlsValidation) {
            object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    request?.url.toString().let { url ->
                        setTitle(url)
                        if (url.startsWith("nc://")) {
                            //Login credentials captured, clear all login data
                            view?.clearCache(true)
                            view?.clearFormData()
                            view?.clearHistory()
                            view?.visibility = View.GONE
                            if (skipTlsValidation) {
                                PreferencesManager.getInstance(context)
                                    .setSkipCertificateValidation(true)
                            }
                            onLoginUrl(url)
                        } else view?.loadUrl(url, mapOf("OCS-APIREQUEST" to "true"))
                    }
                    return false
                }

                @SuppressLint("WebViewClientOnReceivedSslError")
                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler?,
                    error: SslError?
                ) {
                    if (skipTlsValidation) {
                        handler?.proceed()
                    } else {
                        showTlsDialog = true
                        super.onReceivedSslError(view, handler, error)
                    }
                }
            }
        }

        val (loadingProgress, setLoadingProgress) = remember { mutableIntStateOf(0) }

        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            maxLines = 1,
                            fontSize = 14.sp,
                            overflow = TextOverflow.Clip
                        )
                    },
                    windowInsets = WindowInsets.statusBars
                )
            },
            bottomBar = {
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            },
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                AndroidView(
                    factory = {
                        WebView(it).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            val webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    setLoadingProgress(newProgress)
                                    super.onProgressChanged(view, newProgress)
                                }
                            }

                            this.webChromeClient = webChromeClient

                            this.webViewClient = webViewClient

                            settings.domStorageEnabled = true
                            settings.javaScriptEnabled = true
                            settings.userAgentString = it.getString(R.string.app_name)

                            loadUrl(url, mapOf("OCS-APIREQUEST" to "true"))
                        }
                    },
                    update = {
                        it.webViewClient = webViewClient
                        it.loadUrl(url, mapOf("OCS-APIREQUEST" to "true"))
                    },
                )
                if (loadingProgress < 100) {
                    LinearProgressIndicator(
                        progress = (loadingProgress.toFloat() / 100),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (showTlsDialog) {
                AlertDialog(
                    onDismissRequest = { showTlsDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                skipTlsValidation = true
                                showTlsDialog = false
                            }
                        ) {
                            Text(text = stringResource(id = android.R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showTlsDialog = false
                            }
                        ) {
                            Text(text = stringResource(id = android.R.string.cancel))
                        }
                    },
                    title = { Text(stringResource(id = R.string.invalid_certificate)) },
                    text = { Text(text = stringResource(id = R.string.invalid_certificate_dialog_text)) }
                )
            }
        }
    }
}

@Preview(name = "Login card")
@Composable
fun PreviewCard() {
    NextcloudPasswordsTheme {
        LoginCard("", {}, "") {}
    }
}
