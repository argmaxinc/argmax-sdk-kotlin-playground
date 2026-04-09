//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground.transcription

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that manages the playground app's on-device model directories and filesystem helpers.
 *
 * This type intentionally stays below the SDK integration layer. It exposes only local path and
 * directory operations so [TranscriberStore] can orchestrate Home-screen state while
 * [ArgmaxSDKCoordinator] owns direct SDK calls such as model downloads and transcriber creation.
 */
@Singleton
class TranscriberAssetsRepository
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        /**
         * Resolve the default directory used for managed model-asset downloads.
         *
         * The directory lives under app-private storage so it remains valid across devices and can be
         * safely deleted by the app when the user chooses to remove managed assets.
         *
         * @return Absolute directory path for managed model assets.
         */
        fun defaultManagedModelDir(): String {
            val dir = context.getExternalFilesDir("parakeet") ?: context.filesDir
            dir.mkdirs()
            return dir.absolutePath
        }

        /**
         * Resolve the default directory suggested to users for custom model files.
         *
         * This path is separate from managed downloads so the sample clearly distinguishes between
         * app-managed assets and user-provided model directories.
         *
         * @return Absolute directory path suggested for custom models.
         */
        fun defaultCustomModelDir(): String {
            val externalRoot = context.getExternalFilesDir(null)
            val dir = externalRoot?.resolve("models/custom") ?: File(context.filesDir, "models/custom")
            dir.mkdirs()
            return dir.absolutePath
        }

        /**
         * Returns `true` when [modelDir] currently contains any managed model-asset files.
         *
         * @param modelDir Root directory that should contain managed assets.
         * @return `true` when at least one regular file exists under [modelDir].
         */
        fun hasManagedModelAssets(modelDir: String = defaultManagedModelDir()): Boolean {
            val modelRoot = File(modelDir)
            return directoryContainsAnyFile(modelRoot)
        }

        /**
         * Delete all managed model assets rooted at [modelDir].
         *
         * This method performs the filesystem deletion on `Dispatchers.IO` and throws when the
         * directory cannot be removed completely.
         *
         * @param modelDir Root directory that should be deleted.
         * @throws IllegalStateException When filesystem deletion fails.
         */
        suspend fun deleteManagedModelAssets(modelDir: String = defaultManagedModelDir()) {
            val normalizedDir = File(modelDir).absolutePath
            withContext(Dispatchers.IO) {
                val modelRoot = File(normalizedDir)
                deleteDirectoryIfPresent(modelRoot)
            }
        }

        /**
         * Returns `true` when [directory] contains at least one regular file.
         */
        private fun directoryContainsAnyFile(directory: File): Boolean {
            if (!directory.exists() || !directory.isDirectory) {
                return false
            }
            directory.walkTopDown().forEach { file ->
                if (file.isFile) {
                    return true
                }
            }
            return false
        }

        /**
         * Deletes [directory] recursively when it exists, throwing on filesystem failure.
         */
        private fun deleteDirectoryIfPresent(directory: File) {
            if (!directory.exists()) {
                return
            }
            if (!directory.deleteRecursively()) {
                throw IllegalStateException("Unable to delete model directory: ${directory.absolutePath}")
            }
        }
    }
