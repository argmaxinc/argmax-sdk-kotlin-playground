//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground.model

import com.argmaxinc.sdk.transcribe.PipelineStats
import com.argmaxinc.sdk.transcribe.TranscriptionSegment

/**
 * Immutable snapshot of transcription output for rendering in the UI.
 */
data class TranscriptSnapshot(
    /** Confirmed transcript text accumulated so far. */
    val confirmedText: String = "",
    /** Hypothesis text that is still being refined by the decoder. */
    val hypothesisText: String = "",
    /** Full list of timed transcription segments, when available. */
    val segments: List<TranscriptionSegment> = emptyList(),
    /** Optional pipeline performance stats for the latest run. */
    val stats: PipelineStats? = null,
)
