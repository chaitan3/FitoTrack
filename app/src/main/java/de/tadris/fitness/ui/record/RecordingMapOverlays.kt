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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.tadris.fitness.R
import de.tadris.fitness.recording.GpsState
import de.tadris.fitness.recording.RecordingState
import de.tadris.fitness.recording.component.HeartRateComponent

@Composable
fun RecordingTopOverlay(
    recordingState: RecordingState,
    gpsState: GpsState,
    canStart: Boolean,
    hrConnectionState: HeartRateComponent.HeartRateConnectionState,
    onBack: () -> Unit,
    onChooseBluetooth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // Back pill button — CenterStart
        MapPillButton(
            modifier = Modifier.align(Alignment.CenterStart),
            onClick = onBack,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
            )
        }

        // Status pill — Center
        StatusPill(
            recordingState = recordingState,
            gpsState = gpsState,
            canStart = canStart,
            modifier = Modifier.align(Alignment.Center),
        )

        // HR pill button — CenterEnd
        MapPillButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = onChooseBluetooth,
        ) {
            HrStatusIcon(hrConnectionState)
        }
    }
}

@Composable
fun StatusPill(
    recordingState: RecordingState,
    gpsState: GpsState,
    canStart: Boolean,
    modifier: Modifier = Modifier,
) {
    val (dotColor, label) = when {
        recordingState == RecordingState.RUNNING ->
            Color(0xFF4CAF50) to stringResource(R.string.recording_status_recording)
        recordingState == RecordingState.AUTO_PAUSE ->
            Color(0xFFFF9800) to stringResource(R.string.recording_status_paused_auto)
        recordingState == RecordingState.MANUAL_PAUSE ->
            Color(0xFFFF9800) to stringResource(R.string.recording_status_paused_manual)

        recordingState == RecordingState.STOPPED ->
            Color(0xFFF44336) to stringResource(R.string.recording_status_stopped)
        canStart ->
            Color(0xFF4CAF50) to stringResource(R.string.recording_status_ready)
        gpsState == GpsState.SIGNAL_BAD ->
            Color(0xFFFF9800) to stringResource(R.string.recording_status_gps_weak)
        else ->
            Color(0xFFFF9800) to stringResource(R.string.recording_status_gps_searching)
    }

    Surface(
        shape = RoundedCornerShape(50),
        shadowElevation = 4.dp,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = dotColor,
                modifier = Modifier.size(8.dp),
            ) {}
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
fun GpsInfoPill(
    gpsState: GpsState,
    satelliteCount: Int,
    modifier: Modifier = Modifier,
) {
    val (iconRes, tintColor, label) = when (gpsState) {
        GpsState.SIGNAL_GOOD ->
            Triple(
                R.drawable.ic_gps_fixed,
                Color(0xFF4CAF50),
                stringResource(R.string.recording_gps_accurate)
            )
        GpsState.SIGNAL_BAD ->
            Triple(
                R.drawable.ic_gps_fixed,
                Color(0xFFFF9800),
                stringResource(R.string.recording_gps_weak, satelliteCount)
            )
        else ->
            Triple(
                R.drawable.ic_gps_no_fix,
                Color(0xFF9E9E9E),
                stringResource(R.string.recording_gps_searching, satelliteCount)
            )
    }

    Surface(
        shape = RoundedCornerShape(50),
        shadowElevation = 4.dp,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
fun MapControlButtons(
    mapController: RecordingMapController,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        MapPillButton(
            onClick = { mapController.focusGps() },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_gps_fixed),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun MapPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = modifier,
    ) {
        IconButton(onClick = onClick) {
            content()
        }
    }
}
