//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

@file:Suppress("FunctionName")

package com.argmaxinc.playground.view

import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.argmaxinc.playground.PlaygroundAppInfo
import com.argmaxinc.playground.viewmodel.HomeViewModel
import com.argmaxinc.playground.viewmodel.NavigationViewModel
import com.argmaxinc.playground.viewmodel.StreamViewModel
import com.argmaxinc.playground.viewmodel.TranscribeViewModel

/**
 * Root Compose entry point that wires Navigation3 destinations and the app theme.
 *
 * @param appInfo Shell-owned metadata rendered by shared UI surfaces.
 * @param ensureRecordPermission Requests `RECORD_AUDIO` when a feature needs it and reports
 * whether recording can proceed immediately.
 * @param unsupportedPlatformWarningMessage Optional startup warning body shown once in an in-app
 * dialog when the current device is outside the SDK's validated platform set.
 */
@Composable
@RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
fun ArgmaxAppContent(
    appInfo: PlaygroundAppInfo,
    ensureRecordPermission: () -> Boolean,
    unsupportedPlatformWarningMessage: String? = null,
) {
    if (LocalInspectionMode.current) {
        ArgmaxAppPreviewPlaceholder(appInfo = appInfo)
        return
    }
    ArgmaxTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            var showUnsupportedPlatformWarning by
                rememberSaveable(unsupportedPlatformWarningMessage) {
                    mutableStateOf(!unsupportedPlatformWarningMessage.isNullOrBlank())
                }
            val navigationViewModel: NavigationViewModel = hiltViewModel()

            NavDisplay(
                backStack = navigationViewModel.backStack,
                onBack = { navigationViewModel.pop() },
                entryDecorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                    ),
                entryProvider =
                    entryProvider {
                        entry<Screen.Home> {
                            val viewModel: HomeViewModel = hiltViewModel()
                            HomeScreen(
                                appInfo = appInfo,
                                viewModel = viewModel,
                                onTranscribe = { navigationViewModel.navigateTo(Screen.Transcribe) },
                                onStream = { navigationViewModel.navigateTo(Screen.Stream) },
                            )
                        }
                        entry<Screen.Transcribe> {
                            val viewModel: TranscribeViewModel = hiltViewModel()
                            TranscribeScreen(
                                viewModel = viewModel,
                                onBack = { navigationViewModel.pop() },
                                ensureRecordPermission = ensureRecordPermission,
                            )
                        }
                        entry<Screen.Stream> {
                            val viewModel: StreamViewModel = hiltViewModel()
                            StreamScreen(
                                viewModel = viewModel,
                                onBack = { navigationViewModel.pop() },
                                ensureRecordPermission = ensureRecordPermission,
                            )
                        }
                    },
            )

            if (showUnsupportedPlatformWarning && unsupportedPlatformWarningMessage != null) {
                UnsupportedPlatformWarningDialog(
                    message = unsupportedPlatformWarningMessage,
                    onDismiss = { showUnsupportedPlatformWarning = false },
                )
            }
        }
    }
}

/**
 * Dialog shown at startup when the current device is outside the SDK's validated platform set.
 *
 * @param message Multi-line device summary shown below the general warning copy.
 * @param onDismiss Called when the user closes the dialog.
 */
@Composable
private fun UnsupportedPlatformWarningDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    val linkText =
        buildAnnotatedString {
            append("You could continue testing the app, but your experience might vary until we validate this device. See ")
            withLink(
                LinkAnnotation.Url(
                    url = SUPPORTED_ANDROID_PLATFORMS_URL,
                    styles =
                        TextLinkStyles(
                            style =
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                ),
                        ),
                ),
            ) {
                append("supported Android platforms")
            }
            append(".")
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "This device is not yet validated by Argmax")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = linkText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                )
                Text(
                    text = message,
                    textAlign = TextAlign.Start,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Continue")
            }
        },
    )
}

/**
 * Preview-friendly placeholder for the app when Hilt is unavailable.
 *
 * @param appInfo App-owned metadata shown inside the preview placeholder.
 */
@Composable
private fun ArgmaxAppPreviewPlaceholder(appInfo: PlaygroundAppInfo) {
    ArgmaxTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${appInfo.displayName} Preview",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
    }
}

/**
 * Preview the app using a preview-safe placeholder view.
 */
@Preview(showBackground = true)
@Composable
@RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
private fun ArgmaxAppContentPreview() {
    ArgmaxAppContent(
        appInfo =
            PlaygroundAppInfo(
                displayName = "Argmax Playground Sample",
                versionName = "1.0.0",
                versionCode = 1,
                sdkVersion = "1.2.0",
            ),
        ensureRecordPermission = { true },
        unsupportedPlatformWarningMessage =
            "Model: Pixel 9 Pro\nDevice: caimito\nSoC: SM8750 (SUN)\nAndroid: 15 (API 35)",
    )
}

private const val SUPPORTED_ANDROID_PLATFORMS_URL: String =
    "https://app.argmaxinc.com/docs/wiki/supported-platforms#android"
