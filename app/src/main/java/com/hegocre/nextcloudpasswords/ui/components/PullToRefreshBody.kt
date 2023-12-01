package com.hegocre.nextcloudpasswords.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshBody(
    isRefreshing: Boolean,
    onRefresh: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    val pullRefreshState = rememberAnimatedPullToRefreshState(
        positionalThreshold = 55.dp
    )

    LaunchedEffect(key1 = isRefreshing) {
        if (isRefreshing) {
            pullRefreshState.animateStartRefresh()
        } else {
            pullRefreshState.animateEndRefresh()
        }
    }

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            onRefresh()
        }
    }

    Box(modifier = Modifier.nestedScroll(pullRefreshState.nestedScrollConnection)) {
        content()

        PullToRefreshContainer(
            state = pullRefreshState,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
@ExperimentalMaterial3Api
fun rememberAnimatedPullToRefreshState(
    positionalThreshold: Dp = PullToRefreshDefaults.PositionalThreshold,
    enabled: () -> Boolean = { true },
): AnimatedPullToRefreshState {
    val density = LocalDensity.current
    val positionalThresholdPx = with(density) { positionalThreshold.toPx() }
    return rememberSaveable(
        positionalThresholdPx, enabled,
        saver = AnimatedPullToRefreshState.Saver(
            positionalThreshold = positionalThresholdPx,
            enabled = enabled,
        )
    ) {
        AnimatedPullToRefreshState(
            initialRefreshing = false,
            positionalThreshold = positionalThresholdPx,
            enabled = enabled,
        )
    }
}

@ExperimentalMaterial3Api
class AnimatedPullToRefreshState(
    initialRefreshing: Boolean,
    override val positionalThreshold: Float,
    enabled: () -> Boolean,
) : PullToRefreshState {
    override val progress get() = adjustedDistancePulled / positionalThreshold
    override val verticalOffset get() = _verticalOffset

    override val isRefreshing get() = _refreshing

    override fun startRefresh() {
        _refreshing = true
        _verticalOffset = positionalThreshold
    }

    suspend fun animateStartRefresh() {
        _refreshing = true
        animateTo(positionalThreshold)
    }

    override fun endRefresh() {
        _verticalOffset = 0f
        _refreshing = false
    }

    suspend fun animateEndRefresh() {
        animateTo(0f)
        _refreshing = false
    }

    override var nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(
            available: Offset,
            source: NestedScrollSource,
        ): Offset = when {
            !enabled() -> Offset.Zero
            // Swiping up
            source == NestedScrollSource.Drag && available.y < 0 -> {
                consumeAvailableOffset(available)
            }

            else -> Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset = when {
            !enabled() -> Offset.Zero
            // Swiping down
            source == NestedScrollSource.Drag && available.y > 0 -> {
                consumeAvailableOffset(available)
            }

            else -> Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            return Velocity(0f, onRelease(available.y))
        }
    }

    /** Helper method for nested scroll connection */
    fun consumeAvailableOffset(available: Offset): Offset {
        val y = if (isRefreshing) 0f else {
            val newOffset = (distancePulled + available.y).coerceAtLeast(0f)
            val dragConsumed = newOffset - distancePulled
            distancePulled = newOffset
            _verticalOffset = calculateVerticalOffset()
            dragConsumed
        }
        return Offset(0f, y)
    }

    /** Helper method for nested scroll connection. Calls onRefresh callback when triggered */
    suspend fun onRelease(velocity: Float): Float {
        if (isRefreshing) return 0f // Already refreshing, do nothing
        // Trigger refresh
        if (adjustedDistancePulled > positionalThreshold) {
            animateStartRefresh()
        } else {
            animateTo(0f)
        }

        val consumed = when {
            // We are flinging without having dragged the pull refresh (for example a fling inside
            // a list) - don't consume
            distancePulled == 0f -> 0f
            // If the velocity is negative, the fling is upwards, and we don't want to prevent the
            // the list from scrolling
            velocity < 0f -> 0f
            // We are showing the indicator, and the fling is downwards - consume everything
            else -> velocity
        }
        distancePulled = 0f
        return consumed
    }

    private suspend fun animateTo(offset: Float) {
        animate(initialValue = _verticalOffset, targetValue = offset) { value, _ ->
            _verticalOffset = value
        }
    }

    /** Provides custom vertical offset behavior for [PullToRefreshContainer] */
    private fun calculateVerticalOffset(): Float = when {
        // If drag hasn't gone past the threshold, the position is the adjustedDistancePulled.
        adjustedDistancePulled <= positionalThreshold -> adjustedDistancePulled
        else -> {
            // How far beyond the threshold pull has gone, as a percentage of the threshold.
            val overshootPercent = abs(progress) - 1.0f
            // Limit the overshoot to 200%. Linear between 0 and 200.
            val linearTension = overshootPercent.coerceIn(0f, 2f)
            // Non-linear tension. Increases with linearTension, but at a decreasing rate.
            val tensionPercent = linearTension - linearTension.pow(2) / 4
            // The additional offset beyond the threshold.
            val extraOffset = positionalThreshold * tensionPercent
            positionalThreshold + extraOffset
        }
    }

    companion object {
        fun Saver(
            positionalThreshold: Float,
            enabled: () -> Boolean,
        ) = androidx.compose.runtime.saveable.Saver<AnimatedPullToRefreshState, Boolean>(
            save = { it.isRefreshing },
            restore = { isRefreshing ->
                AnimatedPullToRefreshState(isRefreshing, positionalThreshold, enabled)
            }
        )
    }

    private var distancePulled by mutableFloatStateOf(0f)
    private val adjustedDistancePulled: Float get() = distancePulled * 0.5f
    private var _verticalOffset by mutableFloatStateOf(0f)
    private var _refreshing by mutableStateOf(initialRefreshing)
}