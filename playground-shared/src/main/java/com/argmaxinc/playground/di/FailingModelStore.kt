//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground.di

import com.argmaxinc.sdk.network.Model
import com.argmaxinc.sdk.network.ModelDownloadEvent
import com.argmaxinc.sdk.network.ModelDownloadSession
import com.argmaxinc.sdk.network.ModelStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * [ModelStore] implementation that always fails with a predefined reason.
 *
 * This fallback keeps app startup stable when Hugging Face configuration is missing
 * or invalid. The load failure then appears through the normal UI error state instead
 * of failing dependency graph creation.
 *
 * @property failureMessage Human-readable message surfaced to callers on download.
 */
class FailingModelStore(
    private val failureMessage: String,
) : ModelStore {
    /**
     * Starts a session that fails immediately for every requested [model], regardless of [rootPath].
     *
     * @throws IllegalStateException always, with [failureMessage].
     */
    override fun download(
        model: Model,
        rootPath: String,
    ): ModelDownloadSession =
        object : ModelDownloadSession {
            override val events: Flow<ModelDownloadEvent> =
                flow {
                    throw IllegalStateException(failureMessage)
                }

            override fun cancel() = Unit
        }
}
