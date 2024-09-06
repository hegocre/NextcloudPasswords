package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
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
        content = { content() }
        //contentColor = MaterialTheme.colorScheme.primary,
        //containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
    )
}