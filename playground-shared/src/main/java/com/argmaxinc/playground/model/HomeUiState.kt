//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground.model

import com.argmaxinc.sdk.ComputeBackend

/**
 * Snapshot of all Home screen UI data derived from the current store state.
 */
data class HomeUiState(
    /** Currently selected model option. */
    val selectedModel: ModelOption,
    /** Custom model path entered by the user. */
    val customModelPath: String,
    /** Target model directory resolved from the selection. */
    val targetModelDir: String,
    /** Model directory that is currently loaded, if any. */
    val loadedModelDir: String?,
    /** Human-friendly status message for display. */
    val statusMessage: String,
    /** Backend selected in Custom model mode for MelSpectrogram initialization. */
    val customMelSpectrogramBackend: ComputeBackend,
    /** Backend selected in Custom model mode for AudioEncoder initialization. */
    val customAudioEncoderBackend: ComputeBackend,
    /** Backend selected in Custom model mode for TextDecoder initialization. */
    val customTextDecoderBackend: ComputeBackend,
    /** Backend selected in Custom model mode for MultimodalLogits initialization. */
    val customMultimodalLogitsBackend: ComputeBackend,
    /** Wall-clock time in milliseconds spent creating the audio encoder during model load. */
    val audioEncoderInitializationTime: Double?,
    /** Wall-clock time in milliseconds spent creating the MelSpectrogram feature extractor during model load. */
    val melSpectrogramInitializationTime: Double?,
    /** Wall-clock time in milliseconds spent creating the text decoder during model load. */
    val textDecoderInitializationTime: Double?,
    /** Backend used for the MelSpectrogram model in the currently loaded runtime config. */
    val melSpectrogramBackend: ComputeBackend?,
    /** Backend used for the AudioEncoder model in the currently loaded runtime config. */
    val audioEncoderBackend: ComputeBackend?,
    /** Backend used for the TextDecoder model in the currently loaded runtime config. */
    val textDecoderBackend: ComputeBackend?,
    /** Backend used for the MultimodalLogits model in the currently loaded runtime config. */
    val multimodalLogitsBackend: ComputeBackend?,
    /** Wall-clock time in milliseconds spent downloading model assets during model load. */
    val modelDownloadTimeMs: Double?,
    /** Model loading progress in `[0.0, 1.0]` when loading is active, otherwise `null`. */
    val loadingProgress: Float?,
    /** True while a model load is in progress. */
    val isLoading: Boolean,
    /** True once the model is ready for transcription. */
    val isReady: Boolean,
    /** True when the Load button should be enabled. */
    val canLoad: Boolean,
    /** True when the Delete model button should be enabled. */
    val canDelete: Boolean,
    /** True when a model-load error should be shown prominently. */
    val isModelLoadErrorVisible: Boolean,
    /** Title displayed in the model-load error screen when [isModelLoadErrorVisible] is true. */
    val modelLoadErrorTitle: String?,
    /** Detailed user-facing model-load error description when [isModelLoadErrorVisible] is true. */
    val modelLoadErrorMessage: String?,
)
