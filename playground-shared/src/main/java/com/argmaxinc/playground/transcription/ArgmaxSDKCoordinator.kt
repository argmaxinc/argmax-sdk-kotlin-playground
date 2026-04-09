//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground.transcription

import android.content.Context
import android.os.SystemClock
import com.argmaxinc.playground.di.ArgmaxApiKey
import com.argmaxinc.playground.di.ArgmaxApiKeyConfiguration
import com.argmaxinc.sdk.ArgmaxConfig
import com.argmaxinc.sdk.ArgmaxSDK
import com.argmaxinc.sdk.audio.AudioReader
import com.argmaxinc.sdk.network.Model
import com.argmaxinc.sdk.network.ModelStore
import com.argmaxinc.sdk.network.downloadAndAwait
import com.argmaxinc.sdk.parakeet.ParakeetConfig
import com.argmaxinc.sdk.transcribe.DecodingOptions
import com.argmaxinc.sdk.transcribe.PipelineStats
import com.argmaxinc.sdk.transcribe.StreamingOptions
import com.argmaxinc.sdk.transcribe.TranscribeContext
import com.argmaxinc.sdk.transcribe.TranscribeStreamSession
import com.argmaxinc.sdk.transcribe.TranscriberConfig
import com.argmaxinc.sdk.transcribe.TranscriberPro
import com.argmaxinc.sdk.transcribe.TranscriptionResult
import com.argmaxinc.sdk.transcribe.transcribe
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe coordinator that owns the playground app's direct SDK integration points.
 *
 * This class is intentionally explicit so readers can inspect one file and see which SDK call is
 * responsible for each part of the playground app's transcription flow, including SDK
 * initialization, managed model downloads, transcriber creation, and runtime inference entrypoints.
 *
 * For the end-to-end real-time transcription sample, see
 * [the basic example](https://app.argmaxinc.com/docs/examples/real-time-transcription#basic-example).
 *
 * Integration map:
 * - [ensureSdkInitialized] calls [ArgmaxSDK.with] to initialize the shared SDK runtime and validate the configured API key.
 * - [prepareManagedModelAssets] calls [ModelStore.downloadAndAwait] to download or reuse managed model assets and resolve the resulting [ParakeetConfig].
 * - [loadOrReuseTranscriber] creates [TranscriberPro] with [TranscriberConfig] and reads [TranscriberPro.snapshotStats] after construction.
 * - [currentLoadedTranscriber] and [requireLoadedTranscriber] expose the currently active [TranscriberPro] instance and its model metadata.
 * - [transcribeSamples] calls [TranscriberPro.transcribe] for one-shot in-memory transcription.
 * - [transcribeFile] calls [transcribe] to read an audio file through [AudioReader] and route it into the SDK transcription pipeline.
 * - [createStreamSession] calls [TranscriberPro.makeStreamSession] to create a [TranscribeStreamSession] for live streaming.
 * - [close] calls [TranscriberPro.close] to unload the active transcriber while leaving the shared [ArgmaxSDK] runtime initialized.
 */
@Singleton
class ArgmaxSDKCoordinator
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:ArgmaxApiKey private val argmaxApiKey: String,
        private val audioReader: AudioReader,
        private val modelStore: ModelStore,
    ) {
        /**
         * Immutable snapshot of the currently loaded transcriber runtime.
         *
         * @property transcriber Active [TranscriberPro] instance that callers can use directly when
         * they want to work with the SDK surface in a lower-level way.
         * @property modelDir Canonical absolute model directory bound to [transcriber].
         * @property parakeetConfig Runtime backend configuration used to create [transcriber].
         * @property initializationStats Pipeline initialization metrics captured immediately after the
         * transcriber was constructed.
         */
        data class LoadedTranscriber(
            val transcriber: TranscriberPro,
            val modelDir: String,
            val parakeetConfig: ParakeetConfig,
            val initializationStats: PipelineStats,
        )

        /**
         * Result returned after managed model assets are available on disk.
         *
         * @property modelDir Canonical absolute directory containing the usable local assets.
         * @property parakeetConfig Runtime backend configuration resolved for those downloaded assets.
         * @property downloadTimeMs Wall-clock duration in milliseconds spent waiting for the managed
         * model assets to become available locally for this request.
         */
        data class PreparedManagedModel(
            val modelDir: String,
            val parakeetConfig: ParakeetConfig,
            val downloadTimeMs: Double,
        )

        private val loadedTranscriberMutex = Mutex()
        private val sdkInitializationMutex = Mutex()
        private var sdkInitialized = false

        @Volatile private var loadedTranscriber: LoadedTranscriber? = null

        /**
         * Ensure the process-scope [ArgmaxSDK] has been initialized.
         *
         * This is the explicit sample-app equivalent of calling `ArgmaxSDK.with(...)` during app
         * startup. The method is idempotent so callers can safely invoke it before every load request
         * without reinitializing the shared SDK state.
         */
        suspend fun ensureSdkInitialized() {
            sdkInitializationMutex.withLock {
                if (sdkInitialized) return@withLock
                val configuredApiKey = ArgmaxApiKeyConfiguration.requireConfigured(argmaxApiKey)
                ArgmaxSDK.with(
                    context = context,
                    argmaxConfig = ArgmaxConfig(apiKey = configuredApiKey),
                )
                sdkInitialized = true
            }
        }

        /**
         * Ensure managed model assets exist locally under [modelDir].
         *
         * This is the playground's managed-download entrypoint. It delegates to
         * [ModelStore.downloadAndAwait] so callers receive the exact local model directory and
         * resolved [ParakeetConfig] produced by the SDK downloader.
         *
         * @param modelDir Root directory where managed assets should be stored.
         * @param onProgress Callback that receives aggregate download progress in `[0.0, 1.0]`.
         * @return Metadata describing the now-available managed model assets.
         */
        suspend fun prepareManagedModelAssets(
            modelDir: String,
            onProgress: (Float) -> Unit = {},
        ): PreparedManagedModel =
            withContext(Dispatchers.IO) {
                val modelRoot = File(modelDir).apply { mkdirs() }
                ensureSdkInitialized()
                val startNanos = SystemClock.elapsedRealtimeNanos()
                val (localModelRootPath, parakeetConfig) =
                    modelStore.downloadAndAwait(
                        model = Model.PARAKEET_V2,
                        rootPath = modelRoot.absolutePath,
                        onProgress = onProgress,
                    )
                val elapsedMs = (SystemClock.elapsedRealtimeNanos() - startNanos) / 1_000_000.0
                PreparedManagedModel(
                    modelDir = File(localModelRootPath).absolutePath,
                    parakeetConfig = parakeetConfig,
                    downloadTimeMs = elapsedMs,
                )
            }

        /**
         * Load a [TranscriberPro] for [modelDir] and [config], reusing the current one when possible.
         *
         * This method demonstrates the SDK's transcriber-construction step by creating
         * `TranscriberPro(TranscriberConfig(...))` after ensuring `ArgmaxSDK.with(...)` has already
         * been called. If the currently loaded transcriber already matches the requested directory and
         * config, that existing instance is returned. Otherwise, a replacement is fully created first,
         * then swapped in atomically, and only then is the previous transcriber closed.
         *
         * @param modelDir Local model directory to load.
         * @param config Runtime backend configuration for the transcriber.
         * @return Immutable snapshot describing the active transcriber after the call completes.
         */
        suspend fun loadOrReuseTranscriber(
            modelDir: String,
            config: ParakeetConfig,
        ): LoadedTranscriber {
            val normalizedDir = File(modelDir).absolutePath
            var previousLoadedTranscriber: LoadedTranscriber? = null
            val activeLoadedTranscriber =
                loadedTranscriberMutex.withLock {
                    val existing = loadedTranscriber
                    if (existing != null && existing.modelDir == normalizedDir && existing.parakeetConfig == config) {
                        return@withLock existing
                    }
                    val replacement = createLoadedTranscriber(normalizedDir, config)
                    previousLoadedTranscriber = loadedTranscriber
                    loadedTranscriber = replacement
                    replacement
                }
            previousLoadedTranscriber?.transcriber?.close()
            return activeLoadedTranscriber
        }

        /**
         * Return the currently loaded transcriber snapshot, if one exists.
         *
         * Callers that only need read access to the active session metadata can use this instead of
         * keeping their own mirrored state.
         */
        fun currentLoadedTranscriber(): LoadedTranscriber? = loadedTranscriber

        /**
         * Return the active transcriber snapshot or throw when no model has been loaded yet.
         *
         * This gives callers access to the exact [TranscriberPro] instance created by
         * [loadOrReuseTranscriber] together with the model metadata that produced it.
         */
        fun requireLoadedTranscriber(): LoadedTranscriber =
            currentLoadedTranscriber() ?: throw IllegalStateException("Transcriber is not loaded")

        /**
         * Transcribe in-memory audio samples with the active [TranscriberPro].
         *
         * This demonstrates the SDK's one-shot in-memory transcription call by invoking
         * `TranscriberPro.transcribe(...)` on the active transcriber.
         *
         * @param floatSamples Normalized 16 kHz mono PCM samples.
         * @param options Optional decoding settings applied to this call.
         * @param context Optional per-call [TranscribeContext] for stats collection.
         * @return SDK transcription results for the provided audio samples.
         */
        suspend fun transcribeSamples(
            floatSamples: FloatArray,
            options: DecodingOptions = DecodingOptions(),
            context: TranscribeContext? = null,
        ): List<TranscriptionResult> =
            withContext(Dispatchers.IO) {
                requireLoadedTranscriber().transcriber.transcribe(
                    floatSamples = floatSamples,
                    options = options,
                    context = context,
                )
            }

        /**
         * Transcribe an audio file path with the active [TranscriberPro].
         *
         * This demonstrates the Android SDK's file-based transcription helper by invoking the
         * extension overload `TranscriberPro.transcribe(audioPath = ..., audioReader = ...)`.
         *
         * @param audioPath Absolute path to an audio file on the device.
         * @param options Optional decoding settings applied to this call.
         * @param context Optional per-call [TranscribeContext] for stats collection.
         * @return SDK transcription results for the audio file.
         */
        suspend fun transcribeFile(
            audioPath: String,
            options: DecodingOptions = DecodingOptions(),
            context: TranscribeContext? = null,
        ): List<TranscriptionResult> =
            withContext(Dispatchers.IO) {
                requireLoadedTranscriber().transcriber.transcribe(
                    audioPath = audioPath,
                    audioReader = audioReader,
                    options = options,
                    context = context,
                )
            }

        /**
         * Create a new [TranscribeStreamSession] from the active [TranscriberPro].
         *
         * This demonstrates the SDK's live-streaming entrypoint by calling
         * `TranscriberPro.makeStreamSession(...)`. Callers are expected to start the returned session
         * with an input flow and collect from `session.results`.
         *
         * @param streamingOptions Streaming behavior and decoding configuration for the new session.
         * @param context Optional per-session [TranscribeContext] for stats collection.
         * @return A new SDK stream session ready to be started by the caller.
         */
        fun createStreamSession(
            streamingOptions: StreamingOptions = StreamingOptions(),
            context: TranscribeContext? = null,
        ): TranscribeStreamSession =
            requireLoadedTranscriber().transcriber.makeStreamSession(
                streamingOptions = streamingOptions,
                context = context,
            )

        /**
         * Close the active [TranscriberPro] and clear the loaded session snapshot.
         *
         * This is the explicit teardown point for the sample app's transcription runtime. The shared
         * `ArgmaxSDK` process state remains initialized; only the currently loaded transcriber is
         * released.
         */
        fun close() {
            val previousLoadedTranscriber =
                runBlocking {
                    loadedTranscriberMutex.withLock {
                        val current = loadedTranscriber
                        loadedTranscriber = null
                        current
                    }
                }
            previousLoadedTranscriber?.transcriber?.close()
        }

        /**
         * Create a fully loaded [LoadedTranscriber] snapshot for the provided model inputs.
         */
        private suspend fun createLoadedTranscriber(
            modelDir: String,
            config: ParakeetConfig,
        ): LoadedTranscriber =
            withContext(Dispatchers.IO) {
                val modelDirectory = File(modelDir).apply { mkdirs() }
                ensureSdkInitialized()
                val transcriber =
                    TranscriberPro(
                        TranscriberConfig(
                            modelDir = modelDirectory.absolutePath,
                            parakeetConfig = config,
                        ),
                    )
                LoadedTranscriber(
                    transcriber = transcriber,
                    modelDir = modelDirectory.absolutePath,
                    parakeetConfig = config,
                    initializationStats = transcriber.snapshotStats(),
                )
            }
    }
