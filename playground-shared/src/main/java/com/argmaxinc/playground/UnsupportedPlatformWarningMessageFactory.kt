//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground

import android.os.Build
import com.argmaxinc.sdk.PlatformValidator
import com.argmaxinc.sdk.hardware.AndroidSoCDetector
import com.argmaxinc.sdk.hardware.SoC

/**
 * Builds condensed unsupported-device details shown when the playground runs on a device outside
 * the SDK's currently validated Android platform set.
 *
 * The produced details are informational only. Callers can embed them in any in-app surface
 * alongside their own warning copy without affecting the app's startup flow.
 */
object UnsupportedPlatformWarningMessageFactory {
    /**
     * Returns condensed unsupported-device details for the current platform when the device falls
     * outside [PlatformValidator]'s validated set.
     *
     * @return Multi-line hardware summary, or `null` when the current device is already validated.
     */
    fun createMessageOrNull(): String? {
        if (PlatformValidator.isValidatedOnCurrentPlatform()) {
            return null
        }
        return buildHardwareSummary()
    }

    /**
     * Builds the condensed hardware summary shown in the unsupported-device dialog.
     *
     * @return Multi-line hardware summary rendered inside the in-app warning dialog.
     */
    private fun buildHardwareSummary(): String =
        buildString {
            appendLine("Model: ${formatHardwareValue(Build.MODEL)}")
            appendLine("Device: ${formatHardwareValue(Build.DEVICE)}")
            appendLine("SoC: ${describeDetectedSoC()}")
            append("Android: ${formatHardwareValue(Build.VERSION.RELEASE)} (API ${Build.VERSION.SDK_INT})")
        }

    /**
     * Formats a Build-derived value into a stable user-facing string for the warning body.
     *
     * @param value Raw platform string that may be null or blank.
     * @return [value] when it is non-empty, otherwise the literal `"unknown"`.
     */
    private fun formatHardwareValue(value: String?): String = value?.trim().orEmpty().ifEmpty { "unknown" }

    /**
     * Converts the SDK SoC detector result into a concise user-facing label for the warning body.
     *
     * @return Stable summary string for the current detected SoC.
     */
    private fun describeDetectedSoC(): String =
        when (val detectedSoC = AndroidSoCDetector().detectSoC()) {
            is SoC.Qualcomm ->
                "${detectedSoC.qualcommChipVersion.value} (${formatHardwareValue(detectedSoC.codeName)})"

            is SoC.Other ->
                listOf(
                    detectedSoC.socModel,
                    detectedSoC.productModel,
                    detectedSoC.productDevice,
                    detectedSoC.description,
                ).mapNotNull { value ->
                    value?.trim()?.takeIf(String::isNotEmpty)
                }.distinct()
                    .joinToString(separator = " | ")
                    .ifEmpty { "unknown" }
        }
}
