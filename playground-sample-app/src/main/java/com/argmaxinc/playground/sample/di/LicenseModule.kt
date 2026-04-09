//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground.sample.di

import com.argmaxinc.playground.di.ArgmaxApiKey
import com.argmaxinc.playground.di.ArgmaxApiKeyConfiguration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Sample app API-key binding used by the public-safe playground app.
 *
 * This module intentionally returns a literal placeholder so the sample stays safe to publish.
 * Long-lived client API keys can be extracted from compiled apps, so production apps should
 * treat them as recoverable client credentials even if they are loaded from somewhere other than
 * source control.
 *
 * If you still decide to ship a client-side key, obfuscate it before checking it into source.
 * That only raises the extraction cost and does not make the key secret.
 */
@Module
@InstallIn(SingletonComponent::class)
object LicenseModule {
    /**
     * Provides the sample app's runtime API key placeholder.
     *
     * Replace the returned value with your own credential-loading strategy before attempting real
     * SDK licensing. If you do not want to keep a long-lived client key inline in source, load it
     * from your own configuration or backend at runtime. If you choose to embed a client key,
     * obfuscate it before checking it into source rather than leaving the raw value inline here.
     *
     * @return Literal placeholder string that forces the shared module to fail fast until the user
     * configures a real key.
     */
    @Provides
    @Singleton
    @ArgmaxApiKey
    fun provideArgmaxApiKey(): String = ArgmaxApiKeyConfiguration.PLACEHOLDER_API_KEY
}
