//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground.di

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ArgmaxApiKeyConfiguration].
 */
class ArgmaxApiKeyConfigurationTest {
    /**
     * Verifies configured keys are trimmed before SDK initialization uses them.
     */
    @Test
    fun requireConfigured_returnsTrimmedApiKey() {
        val resolved = ArgmaxApiKeyConfiguration.requireConfigured("  live-key  ")

        assertEquals("live-key", resolved)
    }

    /**
     * Verifies the shared module rejects the sample app's placeholder key immediately.
     */
    @Test
    fun requireConfigured_throwsForPlaceholderKey() {
        val failure =
            runCatching {
                ArgmaxApiKeyConfiguration.requireConfigured(ArgmaxApiKeyConfiguration.PLACEHOLDER_API_KEY)
            }.exceptionOrNull()

        requireNotNull(failure)
        assertTrue(failure.message.orEmpty().contains("Argmax API key is not configured"))
    }

    /**
     * Verifies blank key values fail with the same actionable setup guidance.
     */
    @Test
    fun requireConfigured_throwsForBlankKey() {
        val failure =
            runCatching {
                ArgmaxApiKeyConfiguration.requireConfigured("   ")
            }.exceptionOrNull()

        requireNotNull(failure)
        assertTrue(failure.message.orEmpty().contains("Argmax API key is not configured"))
    }
}
