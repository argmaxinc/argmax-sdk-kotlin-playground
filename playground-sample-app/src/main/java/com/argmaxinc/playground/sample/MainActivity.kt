//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.argmaxinc.playground.PlaygroundAppInfo
import com.argmaxinc.playground.UnsupportedPlatformWarningMessageFactory
import com.argmaxinc.playground.view.ArgmaxAppContent
import com.argmaxinc.sdk.network.ensureForegroundDownloadNotificationPermission
import dagger.hilt.android.AndroidEntryPoint

/**
 * Sample app launcher activity that bridges Android permissions and app metadata into the
 * shared playground UI module.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val unsupportedPlatformWarningMessage =
            UnsupportedPlatformWarningMessageFactory.createMessageOrNull()
        ensureForegroundDownloadNotificationPermission()
        setContent {
            var hasMicPermission by remember { mutableStateOf(hasRecordPermission()) }
            val permissionLauncher =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    hasMicPermission = granted
                }

            ArgmaxAppContent(
                appInfo =
                    PlaygroundAppInfo(
                        displayName = "Argmax Playground Sample",
                        versionName = BuildConfig.VERSION_NAME,
                        versionCode = BuildConfig.VERSION_CODE,
                        sdkVersion = BuildConfig.SDK_VERSION,
                    ),
                ensureRecordPermission = {
                    if (!hasMicPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        return@ArgmaxAppContent false
                    }
                    true
                },
                unsupportedPlatformWarningMessage = unsupportedPlatformWarningMessage,
            )
        }
    }

    /**
     * Returns whether the activity currently has microphone permission.
     *
     * @return `true` when `RECORD_AUDIO` is already granted.
     */
    private fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
}
