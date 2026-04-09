//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

@file:Suppress("FunctionName")

package com.argmaxinc.playground.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.argmaxinc.sdk.transcribe.PipelineStats
import java.util.Locale

/**
 * Render a compact summary of [PipelineStats] for the current run.
 */
@Composable
fun StatsSection(stats: PipelineStats?) {
    if (stats == null) {
        Text("No run yet.")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StatRow(
            title = "MelSpectrogram",
            runCount = stats.melRuns,
            runMs = stats.melRunAverageMs,
            copyMs = stats.melCopyAverageMs,
        )
        StatRow(
            title = "AudioEncoder",
            runCount = stats.encoderRuns,
            runMs = stats.encoderRunAverageMs,
        )
        StatRow(
            title = "TextDecoder",
            runCount = stats.decoderRuns,
            runMs = stats.decoderRunAverageMs,
            copyMs = stats.decoderCopyAverageMs,
        )
        StatRow(
            title = "MultimodalLogits",
            runCount = stats.logitsRuns,
            runMs = stats.logitsRunAverageMs,
            copyMs = stats.logitsCopyAverageMs,
        )
        Text("Windows processed: ${stats.windows}", style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Render a single metric row with run counts and average timings.
 */
@Composable
fun StatRow(
    title: String,
    runCount: Int,
    runMs: Double,
    copyMs: Double? = null,
) {
    Column {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Runs: $runCount",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "Model running time: ${runMs.formatMs()} ms",
            style = MaterialTheme.typography.bodySmall,
        )
        copyMs?.let {
            Text(
                "Tensor copy time: ${copyMs.formatMs()} ms",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * Format a millisecond duration for display.
 */
private fun Double.formatMs(): String = String.format(Locale.US, "%.2f", this)

/**
 * Provide sample pipeline stats for preview rendering.
 */
private fun previewStats(): PipelineStats =
    PipelineStats(
        windows = 12,
        melRuns = 12,
        melRunAverageMs = 4.8,
        melCopyAverageMs = 0.4,
        encoderRuns = 12,
        encoderRunAverageMs = 12.2,
        decoderRuns = 24,
        decoderRunAverageMs = 6.3,
        decoderCopyAverageMs = 0.6,
        logitsRuns = 24,
        logitsRunAverageMs = 5.5,
        logitsCopyAverageMs = 0.5,
    )

/**
 * Preview the stats section with representative metrics.
 */
@Preview(showBackground = true)
@Composable
private fun StatsSectionPreview() {
    ArgmaxTheme {
        StatsSection(previewStats())
    }
}

/**
 * Preview a single stats row with sample values.
 */
@Preview(showBackground = true)
@Composable
private fun StatRowPreview() {
    ArgmaxTheme {
        StatRow(
            title = "AudioEncoder",
            runCount = 8,
            runMs = 11.2,
            copyMs = 0.7,
        )
    }
}
