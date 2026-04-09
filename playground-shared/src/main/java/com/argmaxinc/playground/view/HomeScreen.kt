//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

@file:Suppress("FunctionName")

package com.argmaxinc.playground.view

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.argmaxinc.playground.PlaygroundAppInfo
import com.argmaxinc.playground.model.HomeUiState
import com.argmaxinc.playground.model.ModelOption
import com.argmaxinc.playground.viewmodel.HomeViewModel
import com.argmaxinc.sdk.ComputeBackend
import java.util.Locale

/**
 * Home screen that presents model loading controls and navigation options.
 *
 * @param appInfo Shell-owned metadata displayed alongside device details.
 */
@Composable
fun HomeScreen(
    appInfo: PlaygroundAppInfo,
    viewModel: HomeViewModel,
    onTranscribe: () -> Unit,
    onStream: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentContext by rememberUpdatedState(context)
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customPathDraft by remember { mutableStateOf(uiState.customModelPath) }
    var melSpectrogramBackendMenuExpanded by remember { mutableStateOf(false) }
    var audioEncoderBackendMenuExpanded by remember { mutableStateOf(false) }
    var textDecoderBackendMenuExpanded by remember { mutableStateOf(false) }
    var multimodalLogitsBackendMenuExpanded by remember { mutableStateOf(false) }

    /**
     * Collect one-shot notification events and display them as short toasts.
     */
    LaunchedEffect(viewModel) {
        viewModel.notificationMessages.collect { message ->
            Toast.makeText(currentContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    HomeScreenContent(
        appInfo = appInfo,
        uiState = uiState,
        modelMenuExpanded = modelMenuExpanded,
        onModelMenuExpandedChange = { modelMenuExpanded = it },
        onSelectModel = { selected ->
            viewModel.selectModel(selected)
            modelMenuExpanded = false
            if (selected == ModelOption.CUSTOM) {
                customPathDraft = uiState.customModelPath
                showCustomDialog = true
            }
        },
        customPathDraft = customPathDraft,
        showCustomDialog = showCustomDialog,
        onCustomPathDraftChange = { customPathDraft = it },
        onConfirmCustomPath = {
            viewModel.updateCustomModelPath(customPathDraft)
            showCustomDialog = false
        },
        onDismissCustomPath = { showCustomDialog = false },
        customMelSpectrogramBackend = uiState.customMelSpectrogramBackend,
        customAudioEncoderBackend = uiState.customAudioEncoderBackend,
        customTextDecoderBackend = uiState.customTextDecoderBackend,
        customMultimodalLogitsBackend = uiState.customMultimodalLogitsBackend,
        melSpectrogramBackendMenuExpanded = melSpectrogramBackendMenuExpanded,
        onMelSpectrogramBackendMenuExpandedChange = { melSpectrogramBackendMenuExpanded = it },
        onSelectMelSpectrogramBackend = { viewModel.updateCustomMelSpectrogramBackend(it) },
        audioEncoderBackendMenuExpanded = audioEncoderBackendMenuExpanded,
        onAudioEncoderBackendMenuExpandedChange = { audioEncoderBackendMenuExpanded = it },
        onSelectAudioEncoderBackend = { viewModel.updateCustomAudioEncoderBackend(it) },
        textDecoderBackendMenuExpanded = textDecoderBackendMenuExpanded,
        onTextDecoderBackendMenuExpandedChange = { textDecoderBackendMenuExpanded = it },
        onSelectTextDecoderBackend = { viewModel.updateCustomTextDecoderBackend(it) },
        multimodalLogitsBackendMenuExpanded = multimodalLogitsBackendMenuExpanded,
        onMultimodalLogitsBackendMenuExpandedChange = { multimodalLogitsBackendMenuExpanded = it },
        onSelectMultimodalLogitsBackend = { viewModel.updateCustomMultimodalLogitsBackend(it) },
        onLoadModel = { viewModel.loadModel() },
        onCancelModelLoad = { viewModel.cancelModelLoad() },
        onDeleteModel = { viewModel.deleteModel() },
        onTranscribe = onTranscribe,
        onStream = onStream,
    )
}

/**
 * Stateless Home screen content used by both runtime and Preview rendering.
 */
@Composable
private fun HomeScreenContent(
    appInfo: PlaygroundAppInfo,
    uiState: HomeUiState,
    modelMenuExpanded: Boolean,
    onModelMenuExpandedChange: (Boolean) -> Unit,
    onSelectModel: (ModelOption) -> Unit,
    customPathDraft: String,
    showCustomDialog: Boolean,
    onCustomPathDraftChange: (String) -> Unit,
    onConfirmCustomPath: () -> Unit,
    onDismissCustomPath: () -> Unit,
    customMelSpectrogramBackend: ComputeBackend,
    customAudioEncoderBackend: ComputeBackend,
    customTextDecoderBackend: ComputeBackend,
    customMultimodalLogitsBackend: ComputeBackend,
    melSpectrogramBackendMenuExpanded: Boolean,
    onMelSpectrogramBackendMenuExpandedChange: (Boolean) -> Unit,
    onSelectMelSpectrogramBackend: (ComputeBackend) -> Unit,
    audioEncoderBackendMenuExpanded: Boolean,
    onAudioEncoderBackendMenuExpandedChange: (Boolean) -> Unit,
    onSelectAudioEncoderBackend: (ComputeBackend) -> Unit,
    textDecoderBackendMenuExpanded: Boolean,
    onTextDecoderBackendMenuExpandedChange: (Boolean) -> Unit,
    onSelectTextDecoderBackend: (ComputeBackend) -> Unit,
    multimodalLogitsBackendMenuExpanded: Boolean,
    onMultimodalLogitsBackendMenuExpandedChange: (Boolean) -> Unit,
    onSelectMultimodalLogitsBackend: (ComputeBackend) -> Unit,
    onLoadModel: () -> Unit,
    onCancelModelLoad: () -> Unit,
    onDeleteModel: () -> Unit,
    onTranscribe: () -> Unit,
    onStream: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LogoHeader(appInfo = appInfo)

            SdkDetailsSection(appInfo = appInfo)

            if (uiState.isModelLoadErrorVisible) {
                ModelLoadErrorScreen(
                    title = uiState.modelLoadErrorTitle.orEmpty(),
                    message = uiState.modelLoadErrorMessage.orEmpty(),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

            ModelSelector(
                selected = uiState.selectedModel,
                expanded = modelMenuExpanded,
                onExpandedChange = onModelMenuExpandedChange,
                onSelect = onSelectModel,
            )

            if (uiState.selectedModel == ModelOption.CUSTOM) {
                Text(
                    text = "Custom model path: ${uiState.customModelPath}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BackendSelector(
                    label = "MelSpectrogram backend",
                    selected = customMelSpectrogramBackend,
                    expanded = melSpectrogramBackendMenuExpanded,
                    onExpandedChange = onMelSpectrogramBackendMenuExpandedChange,
                    onSelect = onSelectMelSpectrogramBackend,
                )
                BackendSelector(
                    label = "AudioEncoder backend",
                    selected = customAudioEncoderBackend,
                    expanded = audioEncoderBackendMenuExpanded,
                    onExpandedChange = onAudioEncoderBackendMenuExpandedChange,
                    onSelect = onSelectAudioEncoderBackend,
                )
                BackendSelector(
                    label = "TextDecoder backend",
                    selected = customTextDecoderBackend,
                    expanded = textDecoderBackendMenuExpanded,
                    onExpandedChange = onTextDecoderBackendMenuExpandedChange,
                    onSelect = onSelectTextDecoderBackend,
                )
                BackendSelector(
                    label = "MultimodalLogits backend",
                    selected = customMultimodalLogitsBackend,
                    expanded = multimodalLogitsBackendMenuExpanded,
                    onExpandedChange = onMultimodalLogitsBackendMenuExpandedChange,
                    onSelect = onSelectMultimodalLogitsBackend,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = uiState.isLoading || uiState.canLoad,
                    onClick = if (uiState.isLoading) onCancelModelLoad else onLoadModel,
                ) {
                    Text(if (uiState.isLoading) "Cancel load" else "Load model")
                }
                if (uiState.selectedModel == ModelOption.PARAKEET_V2) {
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = uiState.canDelete,
                        onClick = onDeleteModel,
                    ) {
                        Text("Delete model")
                    }
                }
            }

            if (uiState.isLoading) {
                val progress = (uiState.loadingProgress ?: 0.0f).coerceIn(0.0f, 1.0f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${(progress * 100.0f).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (uiState.statusMessage.isNotBlank() && !uiState.isModelLoadErrorVisible) {
                Text(
                    text = uiState.statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (uiState.loadedModelDir != null) {
                Text(
                    text = "Loaded model: ${uiState.loadedModelDir}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (uiState.modelDownloadTimeMs != null && uiState.isReady) {
                    Text(
                        text = "Model download: ${String.format(Locale.US, "%.2f s", uiState.modelDownloadTimeMs / 1000.0)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (uiState.melSpectrogramInitializationTime != null && uiState.melSpectrogramBackend != null) {
                    Text(
                        text =
                            "MelSpectrogram init ${String.format(
                                Locale.US,
                                "%.2f",
                                uiState.melSpectrogramInitializationTime,
                            )}ms on ${uiState.melSpectrogramBackend.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (uiState.audioEncoderInitializationTime != null && uiState.audioEncoderBackend != null) {
                    Text(
                        text =
                            "AudioEncoder init ${String.format(
                                Locale.US,
                                "%.2f",
                                uiState.audioEncoderInitializationTime,
                            )}ms on ${uiState.audioEncoderBackend.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (uiState.textDecoderInitializationTime != null && uiState.textDecoderBackend != null) {
                    Text(
                        text =
                            "TextDecoder init ${String.format(
                                Locale.US,
                                "%.2f",
                                uiState.textDecoderInitializationTime,
                            )}ms on ${uiState.textDecoderBackend.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (uiState.multimodalLogitsBackend != null) {
                    Text(
                        text = "MultimodalLogits on ${uiState.multimodalLogitsBackend.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                enabled = uiState.isReady,
                onClick = onTranscribe,
            ) {
                Text("Transcribe")
            }
            Button(
                modifier = Modifier.weight(1f),
                enabled = uiState.isReady,
                onClick = onStream,
            ) {
                Text("Stream")
            }
        }
    }

    if (showCustomDialog) {
        CustomModelPathDialog(
            path = customPathDraft,
            onPathChange = onCustomPathDraftChange,
            onConfirm = onConfirmCustomPath,
            onDismiss = onDismissCustomPath,
        )
    }
}

/**
 * Dropdown control used to pick one [ComputeBackend] for a Custom model stage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackendSelector(
    label: String,
    selected: ComputeBackend,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (ComputeBackend) -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            ComputeBackend.entries.forEach { backend ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(backend.name) },
                    onClick = {
                        onSelect(backend)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

/**
 * Header that centers the playground title without a leading brand mark.
 *
 * @param appInfo Shell-owned metadata that provides the display name.
 */
@Composable
private fun LogoHeader(appInfo: PlaygroundAppInfo) {
    Text(
        text = appInfo.displayName,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Card that surfaces device and SDK details for context.
 *
 * @param appInfo Shell-owned metadata rendered in the details card.
 */
@Composable
private fun SdkDetailsSection(appInfo: PlaygroundAppInfo) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "SDK Version: ${appInfo.sdkVersion}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "App Version: ${appInfo.versionName} (${appInfo.versionCode})",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Device: ${Build.MANUFACTURER} ${Build.MODEL}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Prominent error surface shown when model loading fails with actionable guidance.
 */
@Composable
private fun ModelLoadErrorScreen(
    title: String,
    message: String,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Dropdown control for selecting between bundled and custom models.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelector(
    selected: ModelOption,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (ModelOption) -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Model") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            ModelOption.entries.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

/**
 * Dialog used to capture a custom model directory path from the user.
 */
@Composable
private fun CustomModelPathDialog(
    path: String,
    onPathChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom model path") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter the directory that contains your custom model files.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = path,
                    onValueChange = onPathChange,
                    label = { Text("Model directory") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Save")
            }
        },
    )
}

/**
 * Provide a representative Home screen UI state for previews.
 */
private fun previewHomeUiState(): HomeUiState =
    HomeUiState(
        selectedModel = ModelOption.PARAKEET_V2,
        customModelPath = PREVIEW_CUSTOM_MODEL_PATH,
        targetModelDir = PREVIEW_CUSTOM_MODEL_PATH,
        loadedModelDir = PREVIEW_CUSTOM_MODEL_PATH,
        statusMessage = "Model ready",
        customMelSpectrogramBackend = ComputeBackend.NPU,
        customAudioEncoderBackend = ComputeBackend.NPU,
        customTextDecoderBackend = ComputeBackend.CPU,
        customMultimodalLogitsBackend = ComputeBackend.CPU,
        audioEncoderInitializationTime = 450.25,
        melSpectrogramInitializationTime = 163.5,
        textDecoderInitializationTime = 932.15,
        melSpectrogramBackend = ComputeBackend.NPU,
        audioEncoderBackend = ComputeBackend.NPU,
        textDecoderBackend = ComputeBackend.CPU,
        multimodalLogitsBackend = ComputeBackend.CPU,
        modelDownloadTimeMs = 4_820.0,
        loadingProgress = null,
        isLoading = false,
        isReady = true,
        canLoad = false,
        canDelete = true,
        isModelLoadErrorVisible = false,
        modelLoadErrorTitle = null,
        modelLoadErrorMessage = null,
    )

/**
 * Preview the Home screen layout with a ready model loaded.
 */
@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ArgmaxTheme {
        HomeScreenContent(
            appInfo =
                PlaygroundAppInfo(
                    displayName = "Argmax Playground Sample",
                    versionName = "1.0.0",
                    versionCode = 1,
                    sdkVersion = "1.2.0",
                ),
            uiState = previewHomeUiState(),
            modelMenuExpanded = false,
            onModelMenuExpandedChange = {},
            onSelectModel = {},
            customPathDraft = PREVIEW_CUSTOM_MODEL_PATH,
            showCustomDialog = false,
            onCustomPathDraftChange = {},
            onConfirmCustomPath = {},
            onDismissCustomPath = {},
            customMelSpectrogramBackend = ComputeBackend.NPU,
            customAudioEncoderBackend = ComputeBackend.NPU,
            customTextDecoderBackend = ComputeBackend.CPU,
            customMultimodalLogitsBackend = ComputeBackend.CPU,
            melSpectrogramBackendMenuExpanded = false,
            onMelSpectrogramBackendMenuExpandedChange = {},
            onSelectMelSpectrogramBackend = {},
            audioEncoderBackendMenuExpanded = false,
            onAudioEncoderBackendMenuExpandedChange = {},
            onSelectAudioEncoderBackend = {},
            textDecoderBackendMenuExpanded = false,
            onTextDecoderBackendMenuExpandedChange = {},
            onSelectTextDecoderBackend = {},
            multimodalLogitsBackendMenuExpanded = false,
            onMultimodalLogitsBackendMenuExpandedChange = {},
            onSelectMultimodalLogitsBackend = {},
            onLoadModel = {},
            onCancelModelLoad = {},
            onDeleteModel = {},
            onTranscribe = {},
            onStream = {},
        )
    }
}

/**
 * Preview the Argmax logo header.
 */
@Preview(showBackground = true)
@Composable
private fun LogoHeaderPreview() {
    ArgmaxTheme {
        LogoHeader(
            appInfo =
                PlaygroundAppInfo(
                    displayName = "Argmax Playground Sample",
                    versionName = "1.0.0",
                    versionCode = 1,
                    sdkVersion = "1.2.0",
                ),
        )
    }
}

/**
 * Preview the SDK details card.
 */
@Preview(showBackground = true)
@Composable
private fun SdkDetailsSectionPreview() {
    ArgmaxTheme {
        SdkDetailsSection(
            appInfo =
                PlaygroundAppInfo(
                    displayName = "Argmax Playground Sample",
                    versionName = "1.0.0",
                    versionCode = 1,
                    sdkVersion = "1.2.0",
                ),
        )
    }
}

/**
 * Preview the model selector dropdown in its collapsed state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun ModelSelectorPreview() {
    ArgmaxTheme {
        ModelSelector(
            selected = ModelOption.PARAKEET_V2,
            expanded = false,
            onExpandedChange = {},
            onSelect = {},
        )
    }
}

/**
 * Preview the custom model path dialog.
 */
@Preview(showBackground = true)
@Composable
private fun CustomModelPathDialogPreview() {
    ArgmaxTheme {
        CustomModelPathDialog(
            path = PREVIEW_CUSTOM_MODEL_PATH,
            onPathChange = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}

/**
 * Static custom model path used only in Compose preview rendering.
 */
private const val PREVIEW_CUSTOM_MODEL_PATH: String = "/storage/emulated/0/Android/data/com.argmaxinc.playground/files/models/custom"
