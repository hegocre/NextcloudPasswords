package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
        // The default loading indicator nearly fills its 48dp container, which looks heavy above
        // a list, so a smaller one is placed in the 40dp container of the circular indicator
        indicator = {
            PullToRefreshDefaults.IndicatorBox(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = PullToRefreshDefaults.loadingIndicatorContainerColor,
                elevation = PullToRefreshDefaults.LoadingIndicatorElevation
            ) {
                val indicatorModifier = Modifier.size(LoadingIndicatorSize)
                val indicatorColor = PullToRefreshDefaults.loadingIndicatorColor
                if (isRefreshing) {
                    LoadingIndicator(modifier = indicatorModifier, color = indicatorColor)
                } else {
                    LoadingIndicator(
                        progress = { pullRefreshState.distanceFraction },
                        modifier = indicatorModifier,
                        color = indicatorColor
                    )
                }
            }
        },
        content = { content() }
        //contentColor = MaterialTheme.colorScheme.primary,
        //containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
    )
}

private val LoadingIndicatorSize = 26.dp
