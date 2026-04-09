//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground.model

/**
 * Represents the model source choices available on the Home screen.
 */
enum class ModelOption(
    /** Human-friendly label shown in the UI dropdown. */
    val displayName: String,
) {
    /** Use the managed Parakeet v2 model directory (downloaded from Hugging Face). */
    PARAKEET_V2("parakeet-v2"),

    /** Use a user-specified custom model directory. */
    CUSTOM("custom model"),
}
