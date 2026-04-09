//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

@file:Suppress("FunctionName")

package com.argmaxinc.playground.view

import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.argmaxinc.playground.model.TranscriptSnapshot
import com.argmaxinc.playground.viewmodel.StreamViewModel
import com.argmaxinc.sdk.transcribe.StreamTranscriptionMode
import kotlin.math.roundToInt

/**
 * Screen that runs live streaming transcription without segments or stats.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
fun StreamScreen(
    viewModel: StreamViewModel,
    onBack: () -> Unit,
    ensureRecordPermission: () -> Boolean,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedMode by remember { mutableStateOf(StreamModeUiOption.AlwaysOn) }
    var silenceThreshold by remember {
        mutableFloatStateOf(StreamTranscriptionMode.DEFAULT_SILENCE_THRESHOLD)
    }
    var maxBufferLength by remember {
        mutableFloatStateOf(StreamTranscriptionMode.DEFAULT_MAX_BUFFER_LENGTH)
    }
    var minProcessInterval by remember {
        mutableFloatStateOf(StreamTranscriptionMode.DEFAULT_MIN_PROCESS_INTERVAL)
    }
    StreamScreenContent(
        uiState = uiState,
        selectedMode = selectedMode,
        onModeSelected = { selectedMode = it },
        voiceTriggeredParams =
            VoiceTriggeredParams(
                silenceThreshold = silenceThreshold,
                maxBufferLengthSeconds = maxBufferLength,
                minProcessIntervalSeconds = minProcessInterval,
            ),
        onSilenceThresholdChanged = { silenceThreshold = it },
        onMaxBufferLengthChanged = { maxBufferLength = it },
        onMinProcessIntervalChanged = { minProcessInterval = it },
        onBack = onBack,
        onToggleStreaming = { mode ->
            val isStreaming = uiState is StreamViewModel.UiState.Streaming
            val isFinishing = uiState is StreamViewModel.UiState.Finishing
            if (isStreaming || isFinishing) {
                viewModel.stopStreaming()
            } else {
                if (!ensureRecordPermission()) return@StreamScreenContent
                viewModel.startStreaming(mode)
            }
        },
    )
}

/**
 * Stateless content for the Stream screen, suitable for previews.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamScreenContent(
    uiState: StreamViewModel.UiState,
    selectedMode: StreamModeUiOption,
    onModeSelected: (StreamModeUiOption) -> Unit,
    voiceTriggeredParams: VoiceTriggeredParams,
    onSilenceThresholdChanged: (Float) -> Unit,
    onMaxBufferLengthChanged: (Float) -> Unit,
    onMinProcessIntervalChanged: (Float) -> Unit,
    onBack: () -> Unit,
    onToggleStreaming: (StreamTranscriptionMode) -> Unit,
) {
    var isFullscreen by remember { mutableStateOf(false) }
    val snapshot =
        when (val state = uiState) {
            is StreamViewModel.UiState.Standby -> state.snapshot
            is StreamViewModel.UiState.Streaming -> state.snapshot
            is StreamViewModel.UiState.Finishing -> state.snapshot
            else -> null
        }
    val confirmedText = snapshot?.confirmedText.orEmpty()
    val hypothesisText = snapshot?.hypothesisText.orEmpty()
    val isStreaming = uiState is StreamViewModel.UiState.Streaming
    val isFinishing = uiState is StreamViewModel.UiState.Finishing
    val canToggle =
        uiState is StreamViewModel.UiState.Standby ||
            uiState is StreamViewModel.UiState.Streaming ||
            uiState is StreamViewModel.UiState.Finishing
    val canSelectMode = uiState is StreamViewModel.UiState.Standby
    val toggleLabel =
        when {
            isFinishing -> "Finishing..."
            isStreaming -> "Stop Streaming"
            else -> "Start Streaming"
        }
    val scrollState = rememberScrollState()
    val shouldAutoScroll = isStreaming || isFinishing

    LaunchedEffect(confirmedText, hypothesisText, shouldAutoScroll) {
        if (shouldAutoScroll && (confirmedText.isNotEmpty() || hypothesisText.isNotEmpty())) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    if (isFullscreen) {
        BackHandler {
            isFullscreen = false
        }

        val fullscreenTranscript =
            buildAnnotatedString {
                if (confirmedText.isEmpty() && hypothesisText.isEmpty()) {
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        ),
                    ) {
                        append("No transcript yet.")
                    }
                } else {
                    if (confirmedText.isNotEmpty()) {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                            append(confirmedText)
                        }
                    }
                    if (hypothesisText.isNotEmpty()) {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f),
                            ),
                        ) {
                            append(hypothesisText)
                        }
                    }
                }
            }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Playground",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text =
                                buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.48f),
                                        ),
                                    ) {
                                        append("by ")
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.48f),
                                            fontWeight = FontWeight.Normal,
                                        ),
                                    ) {
                                        append("Argmax")
                                    }
                                },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    IconButton(
                        onClick = { isFullscreen = false },
                        modifier =
                            Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.24f)),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close fullscreen",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }

                Text(
                    text = fullscreenTranscript,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier =
                        Modifier
                            .weight(1f)
                            .verticalScroll(scrollState),
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canToggle,
                    onClick = { onToggleStreaming(selectedMode.toMode(voiceTriggeredParams)) },
                ) {
                    Text(toggleLabel)
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stream") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isFullscreen = true },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fullscreen,
                            contentDescription = "Enter fullscreen",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(24.dp)
                    .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RecordingIndicator(isActive = isStreaming || isFinishing)
                Text(text = uiState.status, style = MaterialTheme.typography.bodyLarge)
            }

            StreamModeToggle(
                selectedMode = selectedMode,
                enabled = canSelectMode,
                onModeSelected = onModeSelected,
                voiceTriggeredParams = voiceTriggeredParams,
                onSilenceThresholdChanged = onSilenceThresholdChanged,
                onMaxBufferLengthChanged = onMaxBufferLengthChanged,
                onMinProcessIntervalChanged = onMinProcessIntervalChanged,
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canToggle,
                onClick = { onToggleStreaming(selectedMode.toMode(voiceTriggeredParams)) },
            ) {
                Text(toggleLabel)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TranscriptSection(
                confirmedText = confirmedText,
                hypothesisText = hypothesisText,
            )
        }
    }
}

/**
 * Segmented control for selecting the stream transcription mode, with a
 * collapsible parameter panel when Voice Triggered is active.
 */
@Composable
private fun StreamModeToggle(
    selectedMode: StreamModeUiOption,
    enabled: Boolean,
    onModeSelected: (StreamModeUiOption) -> Unit,
    voiceTriggeredParams: VoiceTriggeredParams,
    onSilenceThresholdChanged: (Float) -> Unit,
    onMaxBufferLengthChanged: (Float) -> Unit,
    onMinProcessIntervalChanged: (Float) -> Unit,
) {
    val modes = StreamModeUiOption.entries
    var paramsExpanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Transcription Mode",
            style = MaterialTheme.typography.labelLarge,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    onClick = { onModeSelected(mode) },
                    selected = mode == selectedMode,
                    enabled = enabled,
                    icon = {},
                    label = {
                        Text(
                            text = mode.label,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }

        Text(
            text = selectedMode.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        AnimatedVisibility(
            visible = selectedMode == StreamModeUiOption.VoiceTriggered,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            VoiceTriggeredParamsPanel(
                params = voiceTriggeredParams,
                expanded = paramsExpanded,
                enabled = enabled,
                onToggleExpanded = { paramsExpanded = !paramsExpanded },
                onSilenceThresholdChanged = onSilenceThresholdChanged,
                onMaxBufferLengthChanged = onMaxBufferLengthChanged,
                onMinProcessIntervalChanged = onMinProcessIntervalChanged,
            )
        }
    }
}

/**
 * Collapsible parameter panel for Voice Triggered mode.
 * When collapsed, shows a one-line summary of the current parameter values.
 */
@Composable
private fun VoiceTriggeredParamsPanel(
    params: VoiceTriggeredParams,
    expanded: Boolean,
    enabled: Boolean,
    onToggleExpanded: () -> Unit,
    onSilenceThresholdChanged: (Float) -> Unit,
    onMaxBufferLengthChanged: (Float) -> Unit,
    onMinProcessIntervalChanged: (Float) -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(surfaceColor),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (expanded) {
                Text(
                    text = "Parameters",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            } else {
                Text(
                    text = params.summaryText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
            Icon(
                imageVector =
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LabeledSlider(
                    label = "Silence Threshold",
                    value = params.silenceThreshold,
                    valueRange = 0.05f..0.95f,
                    enabled = enabled,
                    formatValue = { "%.2f".format(it) },
                    onValueChange = onSilenceThresholdChanged,
                )
                LabeledSlider(
                    label = "Max Buffer Length",
                    value = params.maxBufferLengthSeconds,
                    valueRange = 0f..60f,
                    enabled = enabled,
                    formatValue = { "${it.toInt()}s" },
                    onValueChange = onMaxBufferLengthChanged,
                )
                NonLinearIntervalSlider(
                    label = "Min Process Interval",
                    value = params.minProcessIntervalSeconds,
                    enabled = enabled,
                    onValueChange = onMinProcessIntervalChanged,
                )
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    formatValue: (Float) -> String,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Text(
                text = formatValue(value),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            colors =
                SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                ),
        )
    }
}

/**
 * Slider with a non-linear scale for Min Interval:
 * first half (0..0.5 position) covers 0.0–2.0s in 0.1 increments,
 * second half (0.5..1.0 position) covers 2.0–15.0s in 1.0 increments.
 */
@Composable
private fun NonLinearIntervalSlider(
    label: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
) {
    val sliderPosition = intervalToPosition(value)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Text(
                text = formatInterval(value),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = sliderPosition,
            onValueChange = { onValueChange(positionToInterval(it)) },
            valueRange = 0f..1f,
            enabled = enabled,
            colors =
                SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                ),
        )
    }
}

private fun intervalToPosition(value: Float): Float =
    if (value <= 2.0f) {
        value / 4.0f
    } else {
        0.5f + (value - 2.0f) / 26.0f
    }

private fun positionToInterval(position: Float): Float =
    if (position <= 0.5f) {
        val raw = position * 4.0f
        (raw * 10).roundToInt() / 10f
    } else {
        val raw = 2.0f + (position - 0.5f) * 26.0f
        raw.roundToInt().toFloat().coerceAtMost(15f)
    }

private fun formatInterval(value: Float): String =
    if (value <= 2.0f) {
        "%.1fs".format(value)
    } else {
        "${value.toInt()}s"
    }

/**
 * Mutable parameters for the Voice Triggered transcription mode.
 */
private data class VoiceTriggeredParams(
    val silenceThreshold: Float = StreamTranscriptionMode.DEFAULT_SILENCE_THRESHOLD,
    val maxBufferLengthSeconds: Float = StreamTranscriptionMode.DEFAULT_MAX_BUFFER_LENGTH,
    val minProcessIntervalSeconds: Float = StreamTranscriptionMode.DEFAULT_MIN_PROCESS_INTERVAL,
) {
    fun summaryText(): String =
        "threshold: ${"%.2f".format(silenceThreshold)}  ·  " +
            "buffer: ${maxBufferLengthSeconds.toInt()}s  ·  " +
            "interval: ${formatInterval(minProcessIntervalSeconds)}"
}

/**
 * Example-app selectable stream modes.
 */
private enum class StreamModeUiOption(
    val label: String,
    val description: String,
) {
    AlwaysOn(
        label = "AlwaysOn",
        description = "Processes audio continuously as it arrives. Lowest latency, highest resource usage.",
    ),
    VoiceTriggered(
        label = "VoiceTriggered",
        description = "Buffers audio and only processes when voice activity is detected.",
    ),
    BatteryOptimized(
        label = "BatteryOptimized",
        description = "Uses voice gating with a longer interval to reduce battery consumption.",
    ),
}

/**
 * Converts a screen mode selection into SDK streaming mode configuration.
 */
private fun StreamModeUiOption.toMode(voiceTriggeredParams: VoiceTriggeredParams): StreamTranscriptionMode =
    when (this) {
        StreamModeUiOption.AlwaysOn -> StreamTranscriptionMode.AlwaysOn
        StreamModeUiOption.VoiceTriggered ->
            StreamTranscriptionMode.VoiceTriggered(
                silenceThreshold = voiceTriggeredParams.silenceThreshold,
                maxBufferLength = voiceTriggeredParams.maxBufferLengthSeconds,
                minProcessInterval = voiceTriggeredParams.minProcessIntervalSeconds,
            )

        StreamModeUiOption.BatteryOptimized -> StreamTranscriptionMode.BatteryOptimized
    }

/**
 * Provide a sample streaming snapshot for previews.
 */
private fun previewStreamSnapshot(): TranscriptSnapshot =
    TranscriptSnapshot(
        confirmedText = "Streaming transcript appears here.",
        hypothesisText = "Live hypothesis…",
    )

@Preview(showBackground = true, name = "AlwaysOn – Standby")
@Composable
private fun StreamScreenAlwaysOnPreview() {
    ArgmaxTheme {
        StreamScreenContent(
            uiState = StreamViewModel.UiState.Standby(snapshot = previewStreamSnapshot()),
            selectedMode = StreamModeUiOption.AlwaysOn,
            onModeSelected = {},
            voiceTriggeredParams = VoiceTriggeredParams(),
            onSilenceThresholdChanged = {},
            onMaxBufferLengthChanged = {},
            onMinProcessIntervalChanged = {},
            onBack = {},
            onToggleStreaming = { _ -> },
        )
    }
}

@Preview(showBackground = true, name = "VoiceTriggered – Streaming")
@Composable
private fun StreamScreenVoiceTriggeredPreview() {
    ArgmaxTheme {
        StreamScreenContent(
            uiState = StreamViewModel.UiState.Streaming(snapshot = previewStreamSnapshot()),
            selectedMode = StreamModeUiOption.VoiceTriggered,
            onModeSelected = {},
            voiceTriggeredParams =
                VoiceTriggeredParams(
                    silenceThreshold = 0.35f,
                    maxBufferLengthSeconds = 45f,
                    minProcessIntervalSeconds = 1.0f,
                ),
            onSilenceThresholdChanged = {},
            onMaxBufferLengthChanged = {},
            onMinProcessIntervalChanged = {},
            onBack = {},
            onToggleStreaming = { _ -> },
        )
    }
}

@Preview(showBackground = true, name = "BatteryOptimized – Standby")
@Composable
private fun StreamScreenBatteryOptimizedPreview() {
    ArgmaxTheme {
        StreamScreenContent(
            uiState = StreamViewModel.UiState.Standby(),
            selectedMode = StreamModeUiOption.BatteryOptimized,
            onModeSelected = {},
            voiceTriggeredParams = VoiceTriggeredParams(),
            onSilenceThresholdChanged = {},
            onMaxBufferLengthChanged = {},
            onMinProcessIntervalChanged = {},
            onBack = {},
            onToggleStreaming = { _ -> },
        )
    }
}
