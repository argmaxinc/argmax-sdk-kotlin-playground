//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground.di

/**
 * Validates app-provided Argmax API keys before the shared playground initializes the SDK.
 *
 * The public-safe sample app intentionally returns [PLACEHOLDER_API_KEY]. This helper converts
 * blank or placeholder values into a deterministic configuration failure so users receive an
 * actionable setup error instead of a vague licensing/network failure.
 */
object ArgmaxApiKeyConfiguration {
    /** Literal placeholder returned by the sample app until the user supplies a real key. */
    const val PLACEHOLDER_API_KEY: String = "REPLACE_WITH_ARGMAX_API_KEY"

    /**
     * Returns a trimmed API key after verifying the app is no longer using placeholder config.
     *
     * @param apiKey Raw API key supplied by the current app.
     * @return Trimmed API key safe to hand to SDK initialization.
     * @throws IllegalStateException When [apiKey] is blank or still set to [PLACEHOLDER_API_KEY].
     */
    fun requireConfigured(apiKey: String): String {
        val normalized = apiKey.trim()
        if (normalized.isEmpty() || normalized == PLACEHOLDER_API_KEY) {
            throw IllegalStateException(
                "Argmax API key is not configured. Replace the placeholder in your app's " +
                    "LicenseModule before launching the playground.",
            )
        }
        return normalized
    }
}
