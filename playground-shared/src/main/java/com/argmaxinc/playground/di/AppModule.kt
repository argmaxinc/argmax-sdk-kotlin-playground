//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground.di

import android.content.Context
import com.argmaxinc.sdk.audio.AndroidAudioReader
import com.argmaxinc.sdk.audio.AudioReader
import com.argmaxinc.sdk.audio.AudioRecorder
import com.argmaxinc.sdk.network.AndroidModelStoreExecutionMode
import com.argmaxinc.sdk.network.ModelStore
import com.argmaxinc.sdk.network.ModelStoreFactory
import com.argmaxinc.sdk.network.ModelStoreOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAudioRecorder(): AudioRecorder = AudioRecorder()

    @Provides
    @Singleton
    fun provideAudioReader(): AudioReader = AndroidAudioReader()

    /**
     * Provides the shared model store used by the app to fetch model assets.
     *
     * When store setup fails, this method returns a fallback that reports a readable
     * runtime error. This keeps app startup stable and lets the Home screen present an
     * actionable error state.
     */
    @Provides
    @Singleton
    fun provideModelStore(
        @ApplicationContext context: Context,
    ): ModelStore =
        runCatching {
            ModelStoreFactory.create(
                context = context,
                options =
                    ModelStoreOptions(
                        executionMode = AndroidModelStoreExecutionMode.ForegroundService(),
                    ),
            )
        }.getOrElse { throwable ->
            val cause = throwable.message?.takeIf { it.isNotBlank() } ?: "Unknown error"
            FailingModelStore(
                failureMessage = "Unable to initialize model store: $cause",
            )
        }

    @Provides
    @Singleton
    @AppScope
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
