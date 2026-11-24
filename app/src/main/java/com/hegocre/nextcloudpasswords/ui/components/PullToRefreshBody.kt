package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PullToRefreshBody(
    isRefreshing: Boolean,
    onRefresh: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        state = pullRefreshState,
        onRefresh = onRefresh,
        isRefreshing = isRefreshing,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        },
        content = { content() }
        //contentColor = MaterialTheme.colorScheme.primary,
        //containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
    )
}