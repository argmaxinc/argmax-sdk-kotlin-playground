//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

@file:Suppress("FunctionName")

package com.argmaxinc.playground.view

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview

/**
 * Render a transcript block with confirmed and hypothesis styling.
 */
@Composable
fun TranscriptSection(
    confirmedText: String,
    hypothesisText: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Transcript",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )

    val annotated =
        buildAnnotatedString {
            if (confirmedText.isEmpty() && hypothesisText.isEmpty()) {
                append("No transcript yet.")
            } else {
                if (confirmedText.isNotEmpty()) {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                        append(confirmedText)
                    }
                }
                if (hypothesisText.isNotEmpty()) {
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f),
                        ),
                    ) {
                        append(hypothesisText)
                    }
                }
            }
        }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier,
    )
}

/**
 * Preview the transcript section with confirmed and hypothesis text.
 */
@Preview(showBackground = true)
@Composable
private fun TranscriptSectionPreview() {
    ArgmaxTheme {
        Column {
            TranscriptSection(
                confirmedText = "Confirmed transcript text.",
                hypothesisText = " Hypothesis text",
            )
        }
    }
}
