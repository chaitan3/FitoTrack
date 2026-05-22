/*
 * Copyright (c) 2026 Jannis Scheibe <jannis@tadris.de>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     FitoTrack is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.tadris.fitness.ui.record

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.tadris.fitness.Instance
import de.tadris.fitness.recording.GpsState
import de.tadris.fitness.recording.RecordingState
import de.tadris.fitness.recording.StartReason
import de.tadris.fitness.recording.autostart.AutoStartCancelEvent
import org.greenrobot.eventbus.EventBus
import de.tadris.fitness.recording.StopReason

@Composable
fun RecordingScaffold(
    vm: RecordingViewModel,
    canStart: Boolean = true,
    gpsState: GpsState = GpsState.SIGNAL_GOOD,
    onFinish: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onManageIntervalSets: () -> Unit = {},
    onAutoStartCustomDelay: (() -> Unit)? = null,
    onAutoStartWaitForGps: (() -> Unit)? = null,
    onAutoStartOnMove: (() -> Unit)? = null,
    mainContent: @Composable BoxScope.() -> Unit,
    mapOverlays: @Composable BoxScope.() -> Unit = {},
) {
    val context = LocalContext.current

    val recordingState by vm.recordingState.collectAsState()
    val values by vm.values.collectAsState()
    val hrConnectionState by vm.hrConnectionState.collectAsState()
    val voiceFeedbackAvailable by vm.voiceFeedbackAvailable.collectAsState()
    val autoStartUiState by vm.autoStartUiState.collectAsState()
    val infoSlots by vm.infoSlots.collectAsState()
    val shouldFinish by vm.shouldFinish.collectAsState()
    val showCommentDialog by vm.showCommentDialog.collectAsState()
    val showStopConfirmation by vm.showStopConfirmation.collectAsState()
    val showIntervalSetDialog by vm.showIntervalSetDialog.collectAsState()
    val workoutType by vm.currentWorkoutType.collectAsState()

    val instance = remember { Instance.getInstance(context) }

    val onBack: () -> Unit = {
        if (recordingState == RecordingState.IDLE) vm.finish()
        else vm.requestStopConfirmation()
    }

    BackHandler(onBack = onBack)

    LaunchedEffect(shouldFinish) {
        if (shouldFinish) {
            onFinish()
        }
    }

    if (showStopConfirmation) {
        StopConfirmDialog(
            onConfirm = { vm.dismissStopConfirmation(); vm.stop(StopReason.STOP_BUTTON_PRESSED) },
            onDismiss = { vm.dismissStopConfirmation() }
        )
    }
    if (showCommentDialog) {
        CommentDialog(
            onConfirm = { vm.onCommentEntered(it) },
            onDismiss = { vm.onCommentDismissed() }
        )
    }
    if (showIntervalSetDialog) {
        SelectIntervalSetDialog(
            onSelect = { vm.onIntervalSetSelected(it) },
            onDismiss = { vm.dismissIntervalSetDialog() },
            onManage = { vm.dismissIntervalSetDialog(); onManageIntervalSets() },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            mainContent()

            RecordingTopOverlay(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(12.dp),
                recordingState = recordingState,
                gpsState = gpsState,
                canStart = canStart,
                hrConnectionState = hrConnectionState,
                onBack = onBack,
                onChooseBluetooth = { vm.requestChooseBluetoothDevice() },
            )

            mapOverlays()

            AutoStartOverlay(
                autoStartState = autoStartUiState,
                distanceUnitUtils = instance.distanceUnitUtils,
                onAbort = { EventBus.getDefault().post(AutoStartCancelEvent) }
            )
        }

        Surface(
            shape = RectangleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
        ) {
            AnimatedContent(
                targetState = recordingState == RecordingState.IDLE,
                transitionSpec = {
                    val enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 6 }
                    val exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 6 }
                    enter.togetherWith(exit).using(SizeTransform(clip = true, sizeAnimationSpec = { _, _ -> tween(350) }))
                },
                label = "BottomSheet",
            ) { isIdle ->
                if (isIdle) {
                    workoutType?.let { wt ->
                        PreStartBottomSheet(
                            workoutType = wt,
                            canStart = canStart,
                            voiceFeedbackAvailable = voiceFeedbackAvailable,
                            useAutoPause = instance.userPreferences.useAutoPause,
                            onStart = { vm.start(StartReason.START_BUTTON) },
                            onAutoStartCustomDelay = onAutoStartCustomDelay,
                            onAutoStartWaitForGps = onAutoStartWaitForGps,
                            onAutoStartOnMove = onAutoStartOnMove,
                            onBluetooth = { vm.requestChooseBluetoothDevice() },
                            onIntervalSet = { vm.requestIntervalSetDialog() },
                            onSettings = onOpenSettings,
                        )
                    }
                } else {
                    RecordingActiveBottomSheet(
                        activeDuration = values.activeDuration,
                        infoSlots = infoSlots,
                        recordingState = recordingState,
                        distanceUnitUtils = instance.distanceUnitUtils,
                        onPauseResume = { vm.toggleManualPause() },
                        onStop = { vm.stop(StopReason.STOP_BUTTON_PRESSED) },
                        onSlotClick = { slot, isLongClick ->
                            if (isLongClick) vm.requestSelectInfoDialog(slot)
                        },
                    )
                }
            }
        }
    }
}
