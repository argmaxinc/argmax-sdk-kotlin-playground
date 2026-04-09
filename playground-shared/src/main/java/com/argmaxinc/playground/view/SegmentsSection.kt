//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

@file:Suppress("FunctionName")

package com.argmaxinc.playground.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import com.argmaxinc.sdk.transcribe.TranscriptionSegment
import java.util.Locale

/**
 * Render a colored list of transcription segments with timing tags.
 */
@Composable
fun SegmentsSection(segments: List<TranscriptionSegment>) {
    if (segments.isEmpty()) {
        Text("No segments available.", style = MaterialTheme.typography.bodyMedium)
        return
    }

    val annotated =
        buildSegmentAnnotatedString(
            segments = segments,
            primary = MaterialTheme.colorScheme.primary,
            secondary = MaterialTheme.colorScheme.secondary,
            tertiary = MaterialTheme.colorScheme.tertiary,
            tagFontSize = MaterialTheme.typography.bodySmall.fontSize,
        )

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge,
    )
}

/**
 * Build a styled string of segments with per-segment color tags.
 */
private fun buildSegmentAnnotatedString(
    segments: List<TranscriptionSegment>,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    tagFontSize: TextUnit,
) = buildAnnotatedString {
    val palette = listOf(primary, secondary, tertiary)
    segments.forEachIndexed { index, segment ->
        val color = palette[index % palette.size]
        val tag = "[#${segment.id} ${segment.start.formatSeconds()}-${segment.end.formatSeconds()}] "
        withStyle(
            SpanStyle(
                color = color,
                fontWeight = FontWeight.SemiBold,
                fontSize = tagFontSize,
            ),
        ) {
            append(tag)
        }
        withStyle(SpanStyle(color = color)) {
            append(segment.text.trimStart())
        }
        if (index != segments.lastIndex) append("\n")
    }
}

/**
 * Format a seconds value for display.
 */
private fun Float.formatSeconds(): String = String.format(Locale.US, "%.2f", this)

/**
 * Provide sample segments for preview rendering.
 */
private fun previewSegments(): List<TranscriptionSegment> =
    listOf(
        TranscriptionSegment(id = 1, start = 0.0f, end = 1.4f, text = "Hello world"),
        TranscriptionSegment(id = 2, start = 1.4f, end = 2.8f, text = "from Argmax"),
    )

/**
 * Preview the segments section with sample data.
 */
@Preview(showBackground = true)
@Composable
private fun SegmentsSectionPreview() {
    ArgmaxTheme {
        SegmentsSection(previewSegments())
    }
}
