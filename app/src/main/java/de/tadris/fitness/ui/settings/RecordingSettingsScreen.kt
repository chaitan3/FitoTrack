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

package de.tadris.fitness.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.recording.announcement.TTSController
import de.tadris.fitness.recording.event.TTSReadyEvent
import de.tadris.fitness.ui.dialog.ChooseAutoTimeoutDialog
import de.tadris.fitness.ui.settings.interval.ManageIntervalSetsActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingSettingsScreen(
    onBack: () -> Unit,
    onNavigateToVoiceAnnouncements: () -> Unit = {},
) {
    val context = LocalContext.current
    val prefs = remember { Instance.getInstance(context).userPreferences }

    // TTS availability — register on EventBus before creating TTSController so we don't miss the event
    var ttsAvailable by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val subscriber = object {
            @Subscribe(threadMode = ThreadMode.MAIN)
            fun onTTSReady(e: TTSReadyEvent) {
                ttsAvailable = e.ttsAvailable
            }
        }
        EventBus.getDefault().register(subscriber)
        val ttsController = TTSController(context)
        onDispose {
            EventBus.getDefault().unregister(subscriber)
            ttsController.destroy()
        }
    }

    var autoPause by remember { mutableStateOf(prefs.useAutoPause) }
    var autoTimeout by remember { mutableStateOf(prefs.autoTimeout) }
    var showOnLockScreen by remember { mutableStateOf(prefs.showOnLockScreen) }
    var showZoomControls by remember { mutableStateOf(prefs.showWorkoutZoomControls) }
    var zoomWithVolume by remember { mutableStateOf(prefs.zoomWithVolumeButtons) }
    var askForComment by remember { mutableStateOf(prefs.askForComment) }
    var intervalsIncludePauses by remember { mutableStateOf(prefs.intervalsIncludePauses) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val autoTimeoutLabel = if (autoTimeout == 0) {
        stringResource(R.string.notimeout)
    } else {
        "$autoTimeout ${stringResource(R.string.timeMinuteShort)}"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preferencesRecordingTitle)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SwitchSettingsItem(
                    title = stringResource(R.string.autoPauseTitle),
                    supporting = stringResource(R.string.autoPauseSummary),
                    checked = autoPause,
                    onCheckedChange = { autoPause = it; prefs.useAutoPause = it },
                )
            }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.intervalAutoTimeoutTraining),
                    supporting = autoTimeoutLabel,
                    onClick = {
                        ChooseAutoTimeoutDialog(
                            context as Activity,
                            { timeoutM ->
                                prefs.autoTimeout = timeoutM
                                autoTimeout = timeoutM
                            },
                            autoTimeout,
                        ).show()
                    },
                )
            }
            item {
                SwitchSettingsItem(
                    title = stringResource(R.string.prefShowOnLockScreenTitle),
                    supporting = stringResource(R.string.prefShowOnLockScreenSummary),
                    checked = showOnLockScreen,
                    onCheckedChange = { showOnLockScreen = it; prefs.showOnLockScreen = it },
                )
            }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.preferenceCurrentSpeedAverageTimeTitle),
                    supporting = stringResource(R.string.preferenceCurrentSpeedAverageTimeSummary),
                    onClick = { showSpeedDialog = true },
                )
            }
            item {
                SwitchSettingsItem(
                    title = stringResource(R.string.showWorkoutZoomControlsTitle),
                    supporting = stringResource(R.string.showWorkoutZoomControlsSummary),
                    checked = showZoomControls,
                    onCheckedChange = { showZoomControls = it; prefs.showWorkoutZoomControls = it },
                )
            }
            item {
                SwitchSettingsItem(
                    title = stringResource(R.string.zoomWithVolumeButtonsTitle),
                    supporting = stringResource(R.string.zoomWithVolumeButtonsSummary),
                    checked = zoomWithVolume,
                    onCheckedChange = { zoomWithVolume = it; prefs.zoomWithVolumeButtons = it },
                )
            }
            item {
                SwitchSettingsItem(
                    title = stringResource(R.string.askForComment),
                    supporting = stringResource(R.string.askForCommentSummary),
                    checked = askForComment,
                    onCheckedChange = { askForComment = it; prefs.askForComment = it },
                )
            }

            item {
                SettingsCategory(title = stringResource(R.string.preferenceCategoryVoiceFeedback))
            }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.voiceAnnouncementsTitle),
                    supporting = if (ttsAvailable) stringResource(R.string.pref_voice_announcements_summary)
                                 else stringResource(R.string.ttsNotAvailable),
                    enabled = ttsAvailable,
                    onClick = onNavigateToVoiceAnnouncements,
                )
            }
            item {
                ClickableSettingsItem(
                    title = stringResource(R.string.intervalTraining),
                    supporting = if (ttsAvailable) stringResource(R.string.manageIntervalsSummary)
                                 else stringResource(R.string.ttsNotAvailable),
                    enabled = ttsAvailable,
                    onClick = {
                        context.startActivity(
                            Intent(
                                context,
                                ManageIntervalSetsActivity::class.java
                            )
                        )
                    },
                )
            }
            item {
                SwitchSettingsItem(
                    title = stringResource(R.string.intervalsIncludePauseTitle),
                    supporting = stringResource(R.string.intervalsIncludePauseSummary),
                    checked = intervalsIncludePauses,
                    onCheckedChange = { intervalsIncludePauses = it; prefs.intervalsIncludePauses = it },
                )
            }
        }
    }

    if (showSpeedDialog) {
        AveragedSpeedDialog(
            initialUseAverage = prefs.useAverageForCurrentSpeed,
            initialSeconds = prefs.timeForCurrentSpeed,
            onDismiss = { showSpeedDialog = false },
            onConfirm = { useAverage, seconds ->
                prefs.useAverageForCurrentSpeed = useAverage
                if (useAverage) prefs.timeForCurrentSpeed = seconds
                showSpeedDialog = false
            },
        )
    }
}

@Composable
private fun AveragedSpeedDialog(
    initialUseAverage: Boolean,
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (useAverage: Boolean, seconds: Int) -> Unit,
) {
    var useAverage by remember { mutableStateOf(initialUseAverage) }
    var seconds by remember { mutableStateOf(initialSeconds.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.preferenceCurrentSpeedTime)) },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.preferenceCurrentSpeedAverageTimeTitle),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = useAverage, onCheckedChange = { useAverage = it })
                }
                if (useAverage) {
                    Text(
                        text = pluralStringResource(R.plurals.seconds, seconds.toInt(), seconds.toInt()),
                        modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally),
                    )
                    Slider(
                        value = seconds,
                        onValueChange = { seconds = it },
                        valueRange = 0f..120f,
                        steps = 119,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(useAverage, seconds.toInt()) }) {
                Text(stringResource(R.string.okay))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
