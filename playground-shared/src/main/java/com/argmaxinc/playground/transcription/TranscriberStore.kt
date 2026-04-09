//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground.transcription

import com.argmaxinc.playground.di.AppScope
import com.argmaxinc.sdk.parakeet.ParakeetConfig
import com.argmaxinc.sdk.transcribe.PipelineStats
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Home-screen-facing facade that orchestrates model loading and asset state for the playground.
 *
 * The store deliberately does not expose direct runtime APIs such as `TranscriberPro.transcribe`
 * or `TranscriberPro.makeStreamSession`. Those SDK interactions live in [ArgmaxSDKCoordinator] so
 * readers can inspect one class to understand how the app integrates with the SDK, while this
 * store focuses on the Home screen's load lifecycle, asset availability, and user-visible state.
 */
@Singleton
class TranscriberStore
    @Inject
    constructor(
        @param:AppScope private val scope: CoroutineScope,
        private val assetsRepository: TranscriberAssetsRepository,
        private val coordinator: ArgmaxSDKCoordinator,
    ) {
        /**
         * Source of a Home-screen load request.
         */
        enum class LoadRequestSource {
            /** User is loading app-managed model assets. */
            MANAGED_MODEL_ASSETS,

            /** User is loading a custom model directory and explicit [ParakeetConfig]. */
            CUSTOM,
        }

        /**
         * Immutable description of a requested model load.
         *
         * @property source Origin of the load request shown to the Home screen.
         * @property modelDir Canonical absolute directory requested by the user.
         * @property requestedConfig Explicit config requested by the user for custom loads, or `null`
         * for managed loads where the effective config is resolved from download metadata.
         */
        data class LoadRequest(
            val source: LoadRequestSource,
            val modelDir: String,
            val requestedConfig: ParakeetConfig?,
        )

        /**
         * User-visible load status for the latest Home-screen request.
         */
        sealed interface LoadStatus {
            /** No load is currently running and no active request is in an error state. */
            data object Idle : LoadStatus

            /**
             * A model load is currently running.
             *
             * @property request Request currently in progress.
             * @property progress Aggregate progress in `[0.0, 1.0]`.
             */
            data class Loading(
                val request: LoadRequest,
                val progress: Float,
            ) : LoadStatus

            /**
             * The latest load request failed.
             *
             * @property request Request that failed.
             * @property throwable Original failure propagated from download, licensing, or transcriber
             * creation logic.
             * @property message User-facing message derived from [throwable].
             */
            data class Failed(
                val request: LoadRequest,
                val throwable: Throwable,
                val message: String = throwable.message?.takeIf { it.isNotBlank() } ?: "Unknown error",
            ) : LoadStatus
        }

        /**
         * Summary of the transcriber currently loaded in [ArgmaxSDKCoordinator].
         *
         * @property modelDir Canonical absolute model directory currently loaded.
         * @property parakeetConfig Runtime backend configuration bound to the loaded transcriber.
         * @property initializationStats Pipeline initialization metrics captured when the transcriber
         * was created.
         */
        data class ActiveSessionInfo(
            val modelDir: String,
            val parakeetConfig: ParakeetConfig,
            val initializationStats: PipelineStats,
        )

        /**
         * Aggregate Home-screen state published by [TranscriberStore].
         *
         * @property activeSession Loaded transcriber metadata, or `null` when no model is available.
         * @property loadStatus Status of the latest requested load.
         * @property managedModelAssetsAvailable `true` when default managed model assets exist on
         * disk and can be deleted from the Home screen.
         * @property lastManagedDownloadTimeMs Download duration for the currently active managed model,
         * or `null` when the active session came from a custom model load or no model is active.
         */
        data class State(
            val activeSession: ActiveSessionInfo?,
            val loadStatus: LoadStatus,
            val managedModelAssetsAvailable: Boolean,
            val lastManagedDownloadTimeMs: Double?,
        )

        private data class PreparedLoad(
            val modelDir: String,
            val parakeetConfig: ParakeetConfig,
            val managedDownloadTimeMs: Double?,
            val managedAssetsAvailable: Boolean,
        )

        private var loadJob: Job? = null
        private var loadGeneration: Long = 0
        private val mutableState =
            MutableStateFlow(
                State(
                    activeSession = coordinator.currentLoadedTranscriber()?.toActiveSessionInfo(),
                    loadStatus = LoadStatus.Idle,
                    managedModelAssetsAvailable = false,
                    lastManagedDownloadTimeMs = null,
                ),
            )

        /**
         * Aggregate observable state used by the Home screen.
         */
        val state: StateFlow<State> = mutableState.asStateFlow()

        init {
            scope.launch {
                refreshManagedModelAssetsAvailability()
            }
        }

        /**
         * Resolve the default directory used for managed model-asset downloads.
         *
         * This method is surfaced through the store because the Home screen uses it when configuring
         * the default managed model option.
         */
        fun defaultManagedModelDir(): String = assetsRepository.defaultManagedModelDir()

        /**
         * Resolve the default directory suggested to users for custom model files.
         */
        fun defaultCustomModelDir(): String = assetsRepository.defaultCustomModelDir()

        /**
         * Start loading a user-provided custom model directory with the supplied [config].
         *
         * The resulting `TranscriberPro` is created by [ArgmaxSDKCoordinator] so the SDK integration
         * remains visible in one place, while this store updates Home-screen state around that load.
         *
         * @param modelDir Directory that contains custom model files.
         * @param config Runtime backend configuration for the custom model.
         */
        fun loadCustomModel(
            modelDir: String = defaultCustomModelDir(),
            config: ParakeetConfig = ParakeetConfig.DEFAULT,
        ) {
            val request =
                LoadRequest(
                    source = LoadRequestSource.CUSTOM,
                    modelDir = java.io.File(modelDir).absolutePath,
                    requestedConfig = config,
                )
            startLoad(request) {
                PreparedLoad(
                    modelDir = request.modelDir,
                    parakeetConfig = config,
                    managedDownloadTimeMs = null,
                    managedAssetsAvailable = mutableState.value.managedModelAssetsAvailable,
                )
            }
        }

        /**
         * Start loading the managed model-assets flow.
         *
         * This path asks [ArgmaxSDKCoordinator] to download or reuse the managed assets and then load
         * the transcriber that uses those assets.
         *
         * @param modelDir Directory where managed assets should be stored.
         */
        fun loadManagedModelAssets(modelDir: String = defaultManagedModelDir()) {
            val request =
                LoadRequest(
                    source = LoadRequestSource.MANAGED_MODEL_ASSETS,
                    modelDir = java.io.File(modelDir).absolutePath,
                    requestedConfig = null,
                )
            startLoad(request) { progress ->
                val preparedModel =
                    coordinator.prepareManagedModelAssets(
                        modelDir = request.modelDir,
                        onProgress = { fraction ->
                            progress(fraction.coerceIn(0.0f, 1.0f) * DOWNLOAD_WEIGHT)
                        },
                    )
                PreparedLoad(
                    modelDir = preparedModel.modelDir,
                    parakeetConfig = preparedModel.parakeetConfig,
                    managedDownloadTimeMs = preparedModel.downloadTimeMs,
                    managedAssetsAvailable =
                        preparedModel.modelDir == java.io.File(defaultManagedModelDir()).absolutePath,
                )
            }
        }

        /**
         * Cancel the active Home-screen load request.
         *
         * Cancellation restores the current state to the last successfully loaded session, if one
         * exists, while keeping asset availability metadata intact.
         */
        fun cancelLoad() {
            if (mutableState.value.loadStatus !is LoadStatus.Loading) {
                return
            }
            loadGeneration += 1
            loadJob?.cancel()
            loadJob = null
            mutableState.update { current ->
                current.copy(
                    activeSession = coordinator.currentLoadedTranscriber()?.toActiveSessionInfo(),
                    loadStatus = LoadStatus.Idle,
                )
            }
        }

        /**
         * Delete managed model assets rooted at [modelDir] and clear any loaded session first.
         *
         * This is intended for the Home screen's explicit "Delete model" action.
         *
         * @param modelDir Managed assets directory to remove.
         */
        suspend fun deleteManagedModelAssets(modelDir: String = defaultManagedModelDir()) {
            val normalizedDir = java.io.File(modelDir).absolutePath
            val defaultManagedDir = java.io.File(defaultManagedModelDir()).absolutePath
            shutdown()
            assetsRepository.deleteManagedModelAssets(normalizedDir)
            mutableState.update { current ->
                current.copy(
                    managedModelAssetsAvailable =
                        if (normalizedDir == defaultManagedDir) false else current.managedModelAssetsAvailable,
                )
            }
        }

        /**
         * Close the loaded transcriber and clear Home-screen state.
         *
         * This unloads the active model from [ArgmaxSDKCoordinator] but does not delete files from disk.
         */
        fun shutdown() {
            loadGeneration += 1
            loadJob?.cancel()
            loadJob = null
            coordinator.close()
            mutableState.update { current ->
                current.copy(
                    activeSession = null,
                    loadStatus = LoadStatus.Idle,
                    lastManagedDownloadTimeMs = null,
                )
            }
        }

        /**
         * Start a Home-screen load request and update [state] as progress or failures occur.
         */
        private fun startLoad(
            request: LoadRequest,
            prepareLoad: suspend ((Float) -> Unit) -> PreparedLoad,
        ) {
            val currentState = mutableState.value
            if ((currentState.loadStatus as? LoadStatus.Loading)?.request == request) {
                return
            }
            if (
                currentState.loadStatus !is LoadStatus.Failed &&
                request.requestedConfig != null &&
                currentState.activeSession.matches(request)
            ) {
                return
            }

            loadGeneration += 1
            val generation = loadGeneration
            loadJob?.cancel()
            mutableState.update { current ->
                current.copy(
                    loadStatus = LoadStatus.Loading(request = request, progress = 0.0f),
                )
            }
            loadJob =
                scope.launch {
                    try {
                        coordinator.ensureSdkInitialized()
                        if (request.source == LoadRequestSource.CUSTOM) {
                            updateProgress(generation = generation, request = request, progress = 0.5f)
                        }
                        val preparedLoad =
                            prepareLoad { progress ->
                                updateProgress(generation = generation, request = request, progress = progress)
                            }
                        updateProgress(generation = generation, request = request, progress = DOWNLOAD_WEIGHT)
                        updateProgress(generation = generation, request = request, progress = DOWNLOAD_WEIGHT + 0.05f)
                        val loadedTranscriber =
                            coordinator.loadOrReuseTranscriber(
                                modelDir = preparedLoad.modelDir,
                                config = preparedLoad.parakeetConfig,
                            )
                        updateProgress(generation = generation, request = request, progress = 1.0f)
                        if (generation == loadGeneration) {
                            mutableState.update { current ->
                                current.copy(
                                    activeSession = loadedTranscriber.toActiveSessionInfo(),
                                    loadStatus = LoadStatus.Idle,
                                    managedModelAssetsAvailable =
                                        if (preparedLoad.managedAssetsAvailable) true else current.managedModelAssetsAvailable,
                                    lastManagedDownloadTimeMs = preparedLoad.managedDownloadTimeMs,
                                )
                            }
                        }
                    } catch (_: CancellationException) {
                        // Cancellation is expected when the request is superseded or explicitly cancelled.
                    } catch (exception: Exception) {
                        if (generation == loadGeneration) {
                            mutableState.update { current ->
                                current.copy(
                                    activeSession = coordinator.currentLoadedTranscriber()?.toActiveSessionInfo(),
                                    loadStatus = LoadStatus.Failed(request = request, throwable = exception),
                                )
                            }
                        }
                    } finally {
                        if (generation == loadGeneration) {
                            loadJob = null
                        }
                    }
                }
        }

        /**
         * Refresh whether the default managed model assets currently exist on disk.
         */
        private suspend fun refreshManagedModelAssetsAvailability() {
            val hasAssets =
                withContext(Dispatchers.IO) {
                    assetsRepository.hasManagedModelAssets(defaultManagedModelDir())
                }
            mutableState.update { current ->
                current.copy(managedModelAssetsAvailable = hasAssets)
            }
        }

        /**
         * Update the in-flight load progress monotonically for the active [generation].
         */
        private fun updateProgress(
            generation: Long,
            request: LoadRequest,
            progress: Float,
        ) {
            if (generation != loadGeneration) {
                return
            }
            val normalizedProgress = progress.coerceIn(0.0f, 1.0f)
            mutableState.update { current ->
                val loading = current.loadStatus as? LoadStatus.Loading ?: return@update current
                if (loading.request != request) {
                    return@update current
                }
                current.copy(
                    loadStatus =
                        loading.copy(
                            progress = maxOf(loading.progress, normalizedProgress),
                        ),
                )
            }
        }

        /**
         * Returns `true` when this active session already matches [request].
         */
        private fun ActiveSessionInfo?.matches(request: LoadRequest): Boolean =
            this != null &&
                modelDir == request.modelDir &&
                request.requestedConfig != null &&
                parakeetConfig == request.requestedConfig

        /**
         * Convert [ArgmaxSDKCoordinator.LoadedTranscriber] to the Home-screen state representation.
         */
        private fun ArgmaxSDKCoordinator.LoadedTranscriber.toActiveSessionInfo(): ActiveSessionInfo =
            ActiveSessionInfo(
                modelDir = modelDir,
                parakeetConfig = parakeetConfig,
                initializationStats = initializationStats,
            )

        private companion object {
            /** Initialization weight reserved for managed asset preparation. */
            const val DOWNLOAD_WEIGHT: Float = 0.9f
        }
    }
