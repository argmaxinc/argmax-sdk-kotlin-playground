//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

@file:Suppress("FunctionName")

package com.argmaxinc.playground.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

private val ArgmaxDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFFFFFFF),
        onPrimary = Color(0xFF0B0B0B),
        secondary = Color(0xFFBDBDBD),
        onSecondary = Color(0xFF0B0B0B),
        tertiary = Color(0xFFE0E0E0),
        onTertiary = Color(0xFF0B0B0B),
        background = Color(0xFF050505),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF0F0F0F),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF1B1B1B),
        onSurfaceVariant = Color(0xFFCDCDCD),
        outline = Color(0xFF3A3A3A),
    )

private val ArgmaxLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF0B0B0B),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF4A4A4A),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFF1C1C1C),
        onTertiary = Color(0xFFFFFFFF),
        background = Color(0xFFF6F6F6),
        onBackground = Color(0xFF0B0B0B),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF0B0B0B),
        surfaceVariant = Color(0xFFE6E6E6),
        onSurfaceVariant = Color(0xFF3A3A3A),
        outline = Color(0xFFB0B0B0),
    )

/**
 * Apply the Argmax Material 3 theme tuned to the logo's black and white palette.
 */
@Composable
fun ArgmaxTheme(content: @Composable () -> Unit) {
    val useDarkTheme = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (useDarkTheme) ArgmaxDarkColorScheme else ArgmaxLightColorScheme,
        content = content,
    )
}

/**
 * Preview the Argmax theme with a simple text sample.
 */
@Preview(showBackground = true)
@Composable
private fun ArgmaxThemePreview() {
    ArgmaxTheme {
        Text("Argmax theme preview")
    }
}
