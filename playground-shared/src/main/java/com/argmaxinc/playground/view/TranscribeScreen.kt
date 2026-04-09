//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

@file:Suppress("FunctionName")

package com.argmaxinc.playground.view

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.argmaxinc.playground.model.TranscriptSnapshot
import com.argmaxinc.playground.viewmodel.TranscribeViewModel
import com.argmaxinc.sdk.transcribe.TranscriptionSegment
import kotlinx.coroutines.launch

/**
 * Screen that allows recording audio and transcribing audio files.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
fun TranscribeScreen(
    viewModel: TranscribeViewModel,
    onBack: () -> Unit,
    ensureRecordPermission: () -> Boolean,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickAudioLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    val path = copyUriToCache(context, uri)
                    viewModel.transcribeFile(path)
                }
            }
        }

    TranscribeScreenContent(
        uiState = uiState,
        onBack = onBack,
        onToggleRecording = {
            val isRecording = uiState is TranscribeViewModel.UiState.Recording
            if (isRecording) {
                viewModel.stopAndTranscribe()
            } else {
                if (!ensureRecordPermission()) return@TranscribeScreenContent
                viewModel.startRecording()
            }
        },
        onPickAudio = { pickAudioLauncher.launch("audio/*") },
    )
}

/**
 * Stateless content for the Transcribe screen, suitable for previews.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranscribeScreenContent(
    uiState: TranscribeViewModel.UiState,
    onBack: () -> Unit,
    onToggleRecording: () -> Unit,
    onPickAudio: () -> Unit,
) {
    val snapshot =
        when (val state = uiState) {
            is TranscribeViewModel.UiState.Standby -> state.snapshot
            is TranscribeViewModel.UiState.TranscribeResult -> state.snapshot
            else -> null
        }
    val confirmedText = snapshot?.confirmedText.orEmpty()
    val hypothesisText = snapshot?.hypothesisText.orEmpty()
    val segments = snapshot?.segments.orEmpty()
    val stats = snapshot?.stats
    val isRecording = uiState is TranscribeViewModel.UiState.Recording
    val isTranscribing = uiState is TranscribeViewModel.UiState.Transcribing
    val canRecord =
        uiState is TranscribeViewModel.UiState.Standby ||
            uiState is TranscribeViewModel.UiState.TranscribeResult ||
            uiState is TranscribeViewModel.UiState.Recording
    val canPickFile =
        uiState is TranscribeViewModel.UiState.Standby ||
            uiState is TranscribeViewModel.UiState.TranscribeResult

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transcribe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RecordingIndicator(isActive = isRecording)
                Text(text = uiState.status, style = MaterialTheme.typography.bodyLarge)
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canRecord,
                onClick = onToggleRecording,
            ) {
                val label =
                    when {
                        isRecording -> "Stop Recording"
                        isTranscribing -> "Transcribing..."
                        else -> "Start Recording"
                    }
                Text(label)
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canPickFile,
                onClick = onPickAudio,
            ) {
                Text("Pick Audio File")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TranscriptSection(
                confirmedText = confirmedText,
                hypothesisText = hypothesisText,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Segments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SegmentsSection(segments)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Run statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            StatsSection(stats)
        }
    }
}

/**
 * Copy a content [Uri] into the app's cache directory and return the file path.
 */
private fun copyUriToCache(
    context: Context,
    uri: Uri,
): String {
    context.contentResolver.openInputStream(uri).use { input ->
        input ?: throw IllegalStateException("Unable to open $uri")
        val cacheFile =
            kotlin.io.path
                .createTempFile(
                    directory = context.cacheDir.toPath(),
                    prefix = "audio_reader_",
                    suffix = ".bin",
                ).toFile()
        cacheFile.outputStream().use { out ->
            input.copyTo(out)
        }
        return cacheFile.absolutePath
    }
}

/**
 * Provide a sample transcription snapshot for previews.
 */
private fun previewTranscriptSnapshot(): TranscriptSnapshot =
    TranscriptSnapshot(
        confirmedText = "The quick brown fox jumps over the lazy dog.",
        hypothesisText = "Hypothesis text",
        segments =
            listOf(
                TranscriptionSegment(id = 1, start = 0.0f, end = 1.2f, text = "The quick"),
                TranscriptionSegment(id = 2, start = 1.2f, end = 2.4f, text = "brown fox"),
            ),
    )

/**
 * Preview the Transcribe screen with a completed transcript.
 */
@Preview(showBackground = true)
@Composable
private fun TranscribeScreenPreview() {
    ArgmaxTheme {
        TranscribeScreenContent(
            uiState =
                TranscribeViewModel.UiState.TranscribeResult(
                    snapshot = previewTranscriptSnapshot(),
                ),
            onBack = {},
            onToggleRecording = {},
            onPickAudio = {},
        )
    }
}
