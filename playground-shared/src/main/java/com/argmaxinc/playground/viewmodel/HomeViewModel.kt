//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.argmaxinc.playground.model.HomeUiState
import com.argmaxinc.playground.model.ModelOption
import com.argmaxinc.playground.transcription.TranscriberStore
import com.argmaxinc.sdk.ComputeBackend
import com.argmaxinc.sdk.ExecutionConfig
import com.argmaxinc.sdk.license.LicenseException
import com.argmaxinc.sdk.parakeet.ParakeetConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel that drives the Home screen model selection and loading workflow.
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val transcriberStore: TranscriberStore,
    ) : ViewModel() {
        /**
         * User-facing model-load error content rendered as a dedicated Home screen panel.
         *
         * @property title Short heading that summarizes the failure mode.
         * @property message Actionable explanation with recovery guidance.
         */
        private data class ModelLoadErrorUi(
            val title: String,
            val message: String,
        )

        private val mutableNotificationMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
        private val selectedModel = MutableStateFlow(ModelOption.PARAKEET_V2)
        private val defaultManagedModelDir: String = transcriberStore.defaultManagedModelDir()
        private val defaultCustomModelPath: String = transcriberStore.defaultCustomModelDir()
        private val customModelPath = MutableStateFlow(defaultCustomModelPath)
        private val customMelSpectrogramBackend =
            MutableStateFlow(ParakeetConfig.DEFAULT.melspectrogramConfig.computeBackend)
        private val customAudioEncoderBackend =
            MutableStateFlow(ParakeetConfig.DEFAULT.audioEncoderConfig.computeBackend)
        private val customTextDecoderBackend =
            MutableStateFlow(ParakeetConfig.DEFAULT.textDecoderConfig.computeBackend)
        private val customMultimodalLogitsBackend =
            MutableStateFlow(ParakeetConfig.DEFAULT.multimodalLogitsConfig.computeBackend)

        /**
         * One-shot user-facing notification messages for transient UI feedback.
         */
        val notificationMessages: SharedFlow<String> = mutableNotificationMessages.asSharedFlow()

        /**
         * Aggregated UI state for the Home screen.
         */
        val uiState: StateFlow<HomeUiState> =
            combine(
                selectedModel,
                customModelPath,
                customMelSpectrogramBackend,
                customAudioEncoderBackend,
                customTextDecoderBackend,
                customMultimodalLogitsBackend,
                transcriberStore.state,
            ) { values ->
                val selected = values[0] as ModelOption
                val customPath = values[1] as String
                val selectedCustomMelSpectrogramBackend = values[2] as ComputeBackend
                val selectedCustomAudioEncoderBackend = values[3] as ComputeBackend
                val selectedCustomTextDecoderBackend = values[4] as ComputeBackend
                val selectedCustomMultimodalLogitsBackend = values[5] as ComputeBackend
                val storeState = values[6] as TranscriberStore.State
                val activeSession = storeState.activeSession
                val loadStatus = storeState.loadStatus
                val loadedModelDir = activeSession?.modelDir
                val initializationStats = activeSession?.initializationStats
                val runtimeParakeetConfig = activeSession?.parakeetConfig
                val targetModelDir = resolveTargetModelDir(selected, customPath)
                val isLoading = loadStatus is TranscriberStore.LoadStatus.Loading
                val isReady = activeSession != null
                val modelLoadErrorUi = resolveModelLoadErrorUi(selected = selected, loadStatus = loadStatus)
                val selectedCustomParakeetConfig =
                    buildCustomParakeetConfig(
                        melSpectrogramBackend = selectedCustomMelSpectrogramBackend,
                        audioEncoderBackend = selectedCustomAudioEncoderBackend,
                        textDecoderBackend = selectedCustomTextDecoderBackend,
                        multimodalLogitsBackend = selectedCustomMultimodalLogitsBackend,
                    )
                val sameTargetAndConfigAlreadyReady =
                    loadStatus !is TranscriberStore.LoadStatus.Failed &&
                        isReady &&
                        loadedModelDir == targetModelDir &&
                        (
                            selected == ModelOption.PARAKEET_V2 ||
                                runtimeParakeetConfig == selectedCustomParakeetConfig
                        )
                val canLoad = !isLoading && !sameTargetAndConfigAlreadyReady
                val loadingProgress =
                    (loadStatus as? TranscriberStore.LoadStatus.Loading)
                        ?.progress
                        ?.coerceIn(0.0f, 1.0f)
                val canDelete =
                    selected == ModelOption.PARAKEET_V2 &&
                        !isLoading &&
                        storeState.managedModelAssetsAvailable
                val statusMessage =
                    when {
                        modelLoadErrorUi != null -> modelLoadErrorUi.title
                        isLoading -> {
                            val progressPercent = ((loadingProgress ?: 0.0f) * 100.0f).toInt()
                            when ((loadStatus as TranscriberStore.LoadStatus.Loading).request.source) {
                                TranscriberStore.LoadRequestSource.MANAGED_MODEL_ASSETS ->
                                    "Downloading and loading model... $progressPercent%"
                                TranscriberStore.LoadRequestSource.CUSTOM ->
                                    "Loading model... $progressPercent%"
                            }
                        }
                        loadStatus is TranscriberStore.LoadStatus.Failed ->
                            "Load failed: ${loadStatus.message}"
                        isReady -> "Model ready"
                        else -> "Model is not loaded"
                    }
                HomeUiState(
                    selectedModel = selected,
                    customModelPath = customPath,
                    targetModelDir = targetModelDir,
                    loadedModelDir = loadedModelDir,
                    statusMessage = statusMessage,
                    customMelSpectrogramBackend = selectedCustomMelSpectrogramBackend,
                    customAudioEncoderBackend = selectedCustomAudioEncoderBackend,
                    customTextDecoderBackend = selectedCustomTextDecoderBackend,
                    customMultimodalLogitsBackend = selectedCustomMultimodalLogitsBackend,
                    audioEncoderInitializationTime = initializationStats?.audioEncoderInitializationTime,
                    melSpectrogramInitializationTime = initializationStats?.featureExtractorInitializationTime,
                    textDecoderInitializationTime = initializationStats?.textDecoderInitializationTime,
                    melSpectrogramBackend = runtimeParakeetConfig?.melspectrogramConfig?.computeBackend,
                    audioEncoderBackend = runtimeParakeetConfig?.audioEncoderConfig?.computeBackend,
                    textDecoderBackend = runtimeParakeetConfig?.textDecoderConfig?.computeBackend,
                    multimodalLogitsBackend = runtimeParakeetConfig?.multimodalLogitsConfig?.computeBackend,
                    modelDownloadTimeMs = storeState.lastManagedDownloadTimeMs,
                    loadingProgress = loadingProgress,
                    isLoading = isLoading,
                    isReady = isReady,
                    canLoad = canLoad,
                    canDelete = canDelete,
                    isModelLoadErrorVisible = modelLoadErrorUi != null,
                    modelLoadErrorTitle = modelLoadErrorUi?.title,
                    modelLoadErrorMessage = modelLoadErrorUi?.message,
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                HomeUiState(
                    selectedModel = ModelOption.PARAKEET_V2,
                    customModelPath = defaultCustomModelPath,
                    targetModelDir = defaultManagedModelDir,
                    loadedModelDir = null,
                    statusMessage = "Model not loaded",
                    customMelSpectrogramBackend = ParakeetConfig.DEFAULT.melspectrogramConfig.computeBackend,
                    customAudioEncoderBackend = ParakeetConfig.DEFAULT.audioEncoderConfig.computeBackend,
                    customTextDecoderBackend = ParakeetConfig.DEFAULT.textDecoderConfig.computeBackend,
                    customMultimodalLogitsBackend = ParakeetConfig.DEFAULT.multimodalLogitsConfig.computeBackend,
                    audioEncoderInitializationTime = null,
                    melSpectrogramInitializationTime = null,
                    textDecoderInitializationTime = null,
                    melSpectrogramBackend = null,
                    audioEncoderBackend = null,
                    textDecoderBackend = null,
                    multimodalLogitsBackend = null,
                    modelDownloadTimeMs = null,
                    loadingProgress = null,
                    isLoading = false,
                    isReady = false,
                    canLoad = true,
                    canDelete = transcriberStore.state.value.managedModelAssetsAvailable,
                    isModelLoadErrorVisible = false,
                    modelLoadErrorTitle = null,
                    modelLoadErrorMessage = null,
                ),
            )

        /**
         * Update the selected model option displayed in the dropdown.
         */
        fun selectModel(option: ModelOption) {
            val previousTarget = resolveTargetModelDir(selectedModel.value, customModelPath.value)
            val nextTarget = resolveTargetModelDir(option, customModelPath.value)
            if (previousTarget != nextTarget) {
                clearLoadedModelIfDifferent(nextTarget)
            }
            selectedModel.value = option
        }

        /**
         * Persist a new custom model path after user confirmation.
         */
        fun updateCustomModelPath(path: String) {
            val sanitized = path.trim().ifEmpty { defaultCustomModelPath }
            customModelPath.value = sanitized
            if (selectedModel.value == ModelOption.CUSTOM) {
                clearLoadedModelIfDifferent(sanitized)
            }
        }

        /**
         * Updates the Custom model MelSpectrogram backend selection.
         */
        fun updateCustomMelSpectrogramBackend(backend: ComputeBackend) {
            customMelSpectrogramBackend.value = backend
        }

        /**
         * Updates the Custom model AudioEncoder backend selection.
         */
        fun updateCustomAudioEncoderBackend(backend: ComputeBackend) {
            customAudioEncoderBackend.value = backend
        }

        /**
         * Updates the Custom model TextDecoder backend selection.
         */
        fun updateCustomTextDecoderBackend(backend: ComputeBackend) {
            customTextDecoderBackend.value = backend
        }

        /**
         * Updates the Custom model MultimodalLogits backend selection.
         */
        fun updateCustomMultimodalLogitsBackend(backend: ComputeBackend) {
            customMultimodalLogitsBackend.value = backend
        }

        /**
         * Trigger model loading based on the currently selected option.
         */
        fun loadModel() {
            val option = selectedModel.value
            val customPath = customModelPath.value
            when (option) {
                ModelOption.PARAKEET_V2 ->
                    transcriberStore.loadManagedModelAssets(resolveTargetModelDir(option, customPath))
                ModelOption.CUSTOM ->
                    transcriberStore.loadCustomModel(
                        modelDir = resolveTargetModelDir(option, customPath),
                        config =
                            buildCustomParakeetConfig(
                                melSpectrogramBackend = customMelSpectrogramBackend.value,
                                audioEncoderBackend = customAudioEncoderBackend.value,
                                textDecoderBackend = customTextDecoderBackend.value,
                                multimodalLogitsBackend = customMultimodalLogitsBackend.value,
                            ),
                    )
            }
        }

        /**
         * Cancels the active model load, if one is currently running.
         */
        fun cancelModelLoad() {
            transcriberStore.cancelLoad()
        }

        /**
         * Deletes downloaded managed model files for the selected built-in model option and emits
         * a user-facing notification when the operation completes.
         */
        fun deleteModel() {
            if (selectedModel.value != ModelOption.PARAKEET_V2) {
                return
            }
            viewModelScope.launch {
                try {
                    transcriberStore.deleteManagedModelAssets(defaultManagedModelDir)
                    mutableNotificationMessages.tryEmit("Model deleted")
                } catch (_: CancellationException) {
                    // Ignore cancellation; this happens when the current operation is superseded.
                } catch (e: Exception) {
                    val message = e.message?.takeIf { it.isNotBlank() } ?: "Unknown error"
                    mutableNotificationMessages.tryEmit("Delete failed: $message")
                }
            }
        }

        /**
         * Builds Home-screen model-load error UI metadata.
         */
        private fun resolveModelLoadErrorUi(
            selected: ModelOption,
            loadStatus: TranscriberStore.LoadStatus,
        ): ModelLoadErrorUi? {
            val failedStatus = loadStatus as? TranscriberStore.LoadStatus.Failed ?: return null
            val throwable = failedStatus.throwable

            if (throwable is LicenseException) {
                return ModelLoadErrorUi(
                    title = "Argmax license authentication failed",
                    message =
                        throwable.message?.takeIf { it.isNotBlank() }
                            ?: "The configured Argmax runtime API key was rejected. Update the key and retry loading.",
                )
            }

            if (
                selected != ModelOption.PARAKEET_V2 ||
                failedStatus.request.source != TranscriberStore.LoadRequestSource.MANAGED_MODEL_ASSETS
            ) {
                return null
            }

            val errorMessage = failedStatus.message.trim()
            if (errorMessage.isBlank()) {
                return null
            }

            val normalizedMessage = errorMessage.lowercase()
            return when {
                normalizedMessage.contains("hugging face") -> {
                    ModelLoadErrorUi(
                        title = "Hugging Face download error",
                        message = errorMessage,
                    )
                }

                else -> null
            }
        }

        /**
         * Resolve the model directory to load for the given [option] and [customPath].
         */
        private fun resolveTargetModelDir(
            option: ModelOption,
            customPath: String,
        ): String =
            when (option) {
                ModelOption.PARAKEET_V2 -> defaultManagedModelDir
                ModelOption.CUSTOM -> customPath
            }

        /**
         * Clear the loaded model if the requested [targetModelDir] differs from the current one.
         */
        private fun clearLoadedModelIfDifferent(targetModelDir: String) {
            val loadedModelDir =
                transcriberStore.state.value.activeSession
                    ?.modelDir
            if (loadedModelDir != null && loadedModelDir != targetModelDir) {
                transcriberStore.shutdown()
            }
        }

        /**
         * Builds [ParakeetConfig] from user-selected Custom model backend choices.
         */
        private fun buildCustomParakeetConfig(
            melSpectrogramBackend: ComputeBackend,
            audioEncoderBackend: ComputeBackend,
            textDecoderBackend: ComputeBackend,
            multimodalLogitsBackend: ComputeBackend,
        ): ParakeetConfig =
            ParakeetConfig(
                melspectrogramConfig = executionConfigForBackend(melSpectrogramBackend),
                audioEncoderConfig = executionConfigForBackend(audioEncoderBackend),
                textDecoderConfig = executionConfigForBackend(textDecoderBackend),
                multimodalLogitsConfig = executionConfigForBackend(multimodalLogitsBackend),
            )

        /**
         * Maps [ComputeBackend] to the default [ExecutionConfig] profile for that backend.
         */
        private fun executionConfigForBackend(backend: ComputeBackend): ExecutionConfig =
            when (backend) {
                ComputeBackend.CPU -> ExecutionConfig.CPU
                ComputeBackend.GPU -> ExecutionConfig.GPU
                ComputeBackend.NPU -> ExecutionConfig.NPU
            }
    }
