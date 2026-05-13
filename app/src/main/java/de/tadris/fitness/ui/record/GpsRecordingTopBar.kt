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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.tadris.fitness.R
import de.tadris.fitness.recording.GpsState
import de.tadris.fitness.recording.RecordingState
import de.tadris.fitness.recording.component.HeartRateComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsRecordingTopBar(
    recordingState: RecordingState,
    hrConnectionState: HeartRateComponent.HeartRateConnectionState,
    voiceFeedbackAvailable: Boolean,
    gpsState: GpsState,
    useAutoPause: Boolean,
    onStop: () -> Unit,
    onIntervalSet: () -> Unit,
    onChooseBluetooth: () -> Unit,
    onTogglePause: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.recordWorkout)) },
        actions = {
            if (recordingState != RecordingState.IDLE) {
                TextButton(onClick = onStop) {
                    Text(stringResource(R.string.stop))
                }
            }
            if (recordingState == RecordingState.IDLE && voiceFeedbackAvailable) {
                IconButton(onClick = onIntervalSet) {
                    Icon(painterResource(R.drawable.ic_list), contentDescription = stringResource(R.string.intervalSet))
                }
            }
            if (recordingState == RecordingState.IDLE) {
                IconButton(onClick = { /* show edit hint */ }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.editDisplayedInformation))
                }
            }
            IconButton(onClick = onChooseBluetooth) {
                HrStatusIcon(hrConnectionState)
            }
            if (!useAutoPause && recordingState != RecordingState.IDLE) {
                IconButton(onClick = onTogglePause) {
                    Icon(
                        painterResource(
                            if (recordingState == RecordingState.RUNNING) R.drawable.ic_pause
                            else R.drawable.ic_resume
                        ),
                        contentDescription = null
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color(gpsState.color), shape = CircleShape)
                    .padding(horizontal = 8.dp)
            )
        }
    )
}
