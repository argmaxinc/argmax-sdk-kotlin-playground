//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground.viewmodel

import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.argmaxinc.playground.model.TranscriptSnapshot
import com.argmaxinc.playground.transcription.ArgmaxSDKCoordinator
import com.argmaxinc.playground.transcription.TranscriberStore
import com.argmaxinc.sdk.audio.AudioRecorder
import com.argmaxinc.sdk.transcribe.DecodingOptions
import com.argmaxinc.sdk.transcribe.DefaultStatsCollector
import com.argmaxinc.sdk.transcribe.TranscribeContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Transcribe screen, handling recording and file transcription.
 */
@HiltViewModel
class TranscribeViewModel
    @Inject
    constructor(
        private val recorder: AudioRecorder,
        transcriberStore: TranscriberStore,
        private val argmaxSdkCoordinator: ArgmaxSDKCoordinator,
    ) : ViewModel() {
        private val transcriptionState = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle())

        /**
         * Combined UI state derived from the shared model store and local transcription state.
         */
        val uiState: StateFlow<UiState> =
            combine(transcriberStore.state, transcriptionState) { storeState, localState ->
                val loadStatus = storeState.loadStatus
                when {
                    loadStatus is TranscriberStore.LoadStatus.Loading -> UiState.Loading()
                    storeState.activeSession != null -> localState.toUiState()
                    loadStatus is TranscriberStore.LoadStatus.Failed ->
                        UiState.Error(status = "Model load failed: ${loadStatus.message}")
                    else -> UiState.Unavailable()
                }
            }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Unavailable())

        /**
         * Start capturing microphone audio for a new transcription pass.
         */
        @RequiresPermission(Manifest.permission.RECORD_AUDIO)
        fun startRecording() {
            if (!isReady(uiState.value)) return
            val started = recorder.start()
            if (started) {
                transcriptionState.value = TranscriptionState.Recording()
            } else {
                transcriptionState.value =
                    TranscriptionState.Idle(
                        status = "Failed to start recording",
                        snapshot = snapshotFromState(uiState.value),
                    )
            }
        }

        /**
         * Stop recording and submit the captured audio for transcription.
         */
        fun stopAndTranscribe() {
            val audio = recorder.stop()
            val previousSnapshot = snapshotFromState(uiState.value)
            transcriptionState.value = TranscriptionState.Transcribing()
            viewModelScope.launch {
                try {
                    val statsCollector = DefaultStatsCollector()
                    val results =
                        argmaxSdkCoordinator.transcribeSamples(
                            floatSamples = audio,
                            context = TranscribeContext(statsCollector = statsCollector),
                        )
                    val transcript = results.joinToString(separator = " ") { it.text }.trim()
                    val segments = results.flatMap { it.segments }
                    val snapshot =
                        TranscriptSnapshot(
                            confirmedText = transcript,
                            segments = segments,
                            stats = statsCollector.toStats(),
                        )
                    transcriptionState.value = TranscriptionState.Result(snapshot = snapshot)
                } catch (e: Exception) {
                    transcriptionState.value =
                        TranscriptionState.Idle(
                            status = "Transcription failed: ${e.message}",
                            snapshot = previousSnapshot,
                        )
                }
            }
        }

        /**
         * Transcribe an on-device audio file located at [path].
         */
        fun transcribeFile(path: String) {
            if (!isReady(uiState.value)) return
            val previousSnapshot = snapshotFromState(uiState.value)
            transcriptionState.value = TranscriptionState.Transcribing(status = "Transcribing file...")
            viewModelScope.launch {
                try {
                    val statsCollector = DefaultStatsCollector()
                    val results =
                        argmaxSdkCoordinator.transcribeFile(
                            audioPath = path,
                            options = DecodingOptions(concurrentWorkerCount = 5),
                            context = TranscribeContext(statsCollector = statsCollector),
                        )
                    val transcript = results.joinToString(separator = " ") { it.text }.trim()
                    val segments = results.flatMap { it.segments }
                    val snapshot =
                        TranscriptSnapshot(
                            confirmedText = transcript,
                            segments = segments,
                            stats = statsCollector.toStats(),
                        )
                    transcriptionState.value = TranscriptionState.Result(snapshot = snapshot)
                } catch (e: Exception) {
                    transcriptionState.value =
                        TranscriptionState.Idle(
                            status = "Transcription failed: ${e.message}",
                            snapshot = previousSnapshot,
                        )
                }
            }
        }

        /**
         * Ensure the audio recorder is stopped when the ViewModel is cleared.
         */
        override fun onCleared() {
            recorder.stop()
            super.onCleared()
        }

        /**
         * True when transcription commands are allowed in the current [state].
         */
        private fun isReady(state: UiState): Boolean = state is UiState.Standby || state is UiState.TranscribeResult

        /**
         * Extract a [TranscriptSnapshot] from the provided [state], when possible.
         */
        private fun snapshotFromState(state: UiState): TranscriptSnapshot? =
            when (state) {
                is UiState.Standby -> state.snapshot
                is UiState.TranscribeResult -> state.snapshot
                else -> null
            }

        /**
         * UI-facing state for the Transcribe screen.
         */
        sealed interface UiState {
            /** Human-friendly status message for display. */
            val status: String

            /** Indicates the model has not been loaded yet. */
            data class Unavailable(
                override val status: String = "Model not loaded",
            ) : UiState

            /** Indicates the model pipeline is initializing. */
            data class Loading(
                override val status: String = "Loading model...",
            ) : UiState

            /** Ready state before recording starts. */
            data class Standby(
                val snapshot: TranscriptSnapshot? = null,
                override val status: String = "Ready",
            ) : UiState

            /** Recording is currently active. */
            data class Recording(
                override val status: String = "Recording...",
            ) : UiState

            /** Audio is being transcribed. */
            data class Transcribing(
                override val status: String = "Transcribing...",
            ) : UiState

            /** Transcription results are available for display. */
            data class TranscribeResult(
                val snapshot: TranscriptSnapshot,
                override val status: String = "Idle",
            ) : UiState

            /** The model pipeline failed to load or run. */
            data class Error(
                override val status: String,
            ) : UiState
        }

        /**
         * Internal state machine for recording and transcription actions.
         */
        sealed class TranscriptionState {
            /**
             * Convert this internal state to a UI-facing representation.
             */
            abstract fun toUiState(): UiState

            /** Idle state with optional cached snapshot. */
            data class Idle(
                val snapshot: TranscriptSnapshot? = null,
                val status: String = "Ready",
            ) : TranscriptionState() {
                override fun toUiState(): UiState = UiState.Standby(snapshot = snapshot, status = status)
            }

            /** Recording is active. */
            data class Recording(
                val status: String = "Recording...",
            ) : TranscriptionState() {
                override fun toUiState(): UiState = UiState.Recording(status = status)
            }

            /** Transcription is running. */
            data class Transcribing(
                val status: String = "Transcribing...",
            ) : TranscriptionState() {
                override fun toUiState(): UiState = UiState.Transcribing(status = status)
            }

            /** Final transcription snapshot. */
            data class Result(
                val snapshot: TranscriptSnapshot,
                val status: String = "Idle",
            ) : TranscriptionState() {
                override fun toUiState(): UiState = UiState.TranscribeResult(snapshot = snapshot, status = status)
            }
        }
    }
