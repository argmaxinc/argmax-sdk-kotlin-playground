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
import com.argmaxinc.sdk.audio.AudioRecordingSession
import com.argmaxinc.sdk.transcribe.StreamTranscriptionMode
import com.argmaxinc.sdk.transcribe.StreamingOptions
import com.argmaxinc.sdk.transcribe.TranscribeStreamSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Stream screen, handling live microphone transcription.
 */
@HiltViewModel
class StreamViewModel
    @Inject
    constructor(
        private val recorder: AudioRecorder,
        transcriberStore: TranscriberStore,
        private val argmaxSdkCoordinator: ArgmaxSDKCoordinator,
    ) : ViewModel() {
        private var recordingSession: AudioRecordingSession? = null
        private var streamingSession: TranscribeStreamSession? = null
        private var streamingJob: Job? = null

        private val streamingState = MutableStateFlow<StreamingState>(StreamingState.Idle())

        /**
         * Combined UI state derived from the shared model store and streaming session.
         */
        val uiState: StateFlow<UiState> =
            combine(transcriberStore.state, streamingState) { storeState, localState ->
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
         * Start live streaming transcription from the microphone using the provided [mode].
         */
        @RequiresPermission(Manifest.permission.RECORD_AUDIO)
        fun startStreaming(mode: StreamTranscriptionMode) {
            if (!isReady(uiState.value)) return
            val session =
                runCatching {
                    argmaxSdkCoordinator.createStreamSession(
                        streamingOptions =
                            StreamingOptions(
                                streamTranscriptionMode = mode,
                            ),
                    )
                }.getOrNull()
            if (session == null) {
                streamingState.value =
                    StreamingState.Idle(
                        status = "Pipeline not ready",
                        snapshot = snapshotFromState(uiState.value),
                    )
                return
            }
            val recording = recorder.startRecordingSession()
            recordingSession = recording
            streamingSession = session
            streamingState.value = StreamingState.Streaming(snapshot = TranscriptSnapshot())
            session.start(recording.audioFlow)

            streamingJob =
                viewModelScope.launch {
                    try {
                        session.results.collect { result ->
                            streamingState.update { state ->
                                val baseSnapshot = snapshotFromState(state.toUiState()) ?: TranscriptSnapshot()
                                val updatedConfirmed =
                                    if (result.text.isNotEmpty()) {
                                        if (baseSnapshot.confirmedText.isEmpty()) {
                                            result.text
                                        } else {
                                            baseSnapshot.confirmedText + result.text
                                        }
                                    } else {
                                        baseSnapshot.confirmedText
                                    }
                                val updatedSnapshot =
                                    baseSnapshot.copy(
                                        confirmedText = updatedConfirmed,
                                        hypothesisText = result.hypothesisText.orEmpty(),
                                    )
                                when (state) {
                                    is StreamingState.Finishing -> StreamingState.Finishing(snapshot = updatedSnapshot)
                                    else -> StreamingState.Streaming(snapshot = updatedSnapshot)
                                }
                            }
                        }
                    } finally {
                        val snapshot = snapshotFromState(uiState.value) ?: TranscriptSnapshot()
                        streamingState.value = StreamingState.Idle(snapshot = snapshot.copy(hypothesisText = ""))
                        recordingSession = null
                        streamingSession = null
                        streamingJob = null
                    }
                }
        }

        /**
         * Stop microphone capture gracefully, allowing the transcription pipeline to finish from
         * normal input completion.
         */
        fun stopStreaming() {
            val state = streamingState.value
            if (state !is StreamingState.Streaming && state !is StreamingState.Finishing) return
            val snapshot = snapshotFromState(state.toUiState()) ?: TranscriptSnapshot()
            streamingState.value = StreamingState.Finishing(snapshot = snapshot)
            val recording = recordingSession ?: return
            viewModelScope.launch {
                recording.finish()
            }
        }

        /**
         * Ensure streaming resources are released when the ViewModel is cleared.
         */
        override fun onCleared() {
            recordingSession?.cancel()
            streamingJob?.cancel()
            recordingSession = null
            streamingSession = null
            super.onCleared()
        }

        /**
         * True when starting a new streaming session is allowed in the current [state].
         */
        private fun isReady(state: UiState): Boolean = state is UiState.Standby

        /**
         * Extract a [TranscriptSnapshot] from the provided [state], when possible.
         */
        private fun snapshotFromState(state: UiState): TranscriptSnapshot? =
            when (state) {
                is UiState.Standby -> state.snapshot
                is UiState.Streaming -> state.snapshot
                is UiState.Finishing -> state.snapshot
                else -> null
            }

        /**
         * UI-facing state for the Stream screen.
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

            /** Ready state before streaming begins. */
            data class Standby(
                val snapshot: TranscriptSnapshot? = null,
                override val status: String = "Ready",
            ) : UiState

            /** Streaming is actively producing live results. */
            data class Streaming(
                val snapshot: TranscriptSnapshot,
                override val status: String = "Streaming...",
            ) : UiState

            /** Streaming has been stopped and results are being finalized. */
            data class Finishing(
                val snapshot: TranscriptSnapshot,
                override val status: String = "Finishing...",
            ) : UiState

            /** The model pipeline failed to load or run. */
            data class Error(
                override val status: String,
            ) : UiState
        }

        /**
         * Internal state machine for live streaming sessions.
         */
        sealed class StreamingState {
            /**
             * Convert this internal state to a UI-facing representation.
             */
            abstract fun toUiState(): UiState

            /** Idle state with optional cached snapshot. */
            data class Idle(
                val snapshot: TranscriptSnapshot? = null,
                val status: String = "Ready",
            ) : StreamingState() {
                override fun toUiState(): UiState = UiState.Standby(snapshot = snapshot, status = status)
            }

            /** Streaming is active. */
            data class Streaming(
                val snapshot: TranscriptSnapshot,
                val status: String = "Streaming...",
            ) : StreamingState() {
                override fun toUiState(): UiState = UiState.Streaming(snapshot = snapshot, status = status)
            }

            /** Streaming is stopping and finalizing output. */
            data class Finishing(
                val snapshot: TranscriptSnapshot,
                val status: String = "Finishing...",
            ) : StreamingState() {
                override fun toUiState(): UiState = UiState.Finishing(snapshot = snapshot, status = status)
            }
        }
    }
