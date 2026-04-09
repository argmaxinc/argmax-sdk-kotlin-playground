//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

@file:Suppress("FunctionName")

package com.argmaxinc.playground.view

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Small visual indicator for recording/streaming activity.
 *
 * When [isActive] is true, a pulsing red dot is shown. Otherwise, a placeholder
 * spacer keeps layout alignment stable.
 */
@Composable
fun RecordingIndicator(isActive: Boolean) {
    if (!isActive) {
        Spacer(modifier = Modifier.size(10.dp))
        return
    }

    val transition = rememberInfiniteTransition(label = "recording")
    val alpha =
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 700),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "recordingAlpha",
        )
    Box(
        modifier =
            Modifier
                .size(10.dp)
                .background(color = Color.Red.copy(alpha = alpha.value), shape = CircleShape),
    )
}

/**
 * Preview the recording indicator in its active state.
 */
@Preview(showBackground = true)
@Composable
private fun RecordingIndicatorPreview() {
    RecordingIndicator(isActive = true)
}
