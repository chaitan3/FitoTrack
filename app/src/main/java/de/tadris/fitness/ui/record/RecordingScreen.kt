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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.tadris.fitness.R
import de.tadris.fitness.recording.RecordingState
import de.tadris.fitness.recording.component.HeartRateComponent
import de.tadris.fitness.util.unit.DistanceUnitUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfoSlotsGrid(
    slots: List<InfoSlot>,
    recordingState: RecordingState,
    onSlotClick: (slot: Int, isLongClick: Boolean) -> Unit,
) {
    // 2x2 grid: top row = slots 0,1; bottom row = slots 2,3
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (slot in 0..1) {
                val info = slots.getOrNull(slot)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = { onSlotClick(slot, false) },
                            onLongClick = { onSlotClick(slot, true) }
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    InfoSlotCell(info)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            for (slot in 2..3) {
                val info = slots.getOrNull(slot)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = { onSlotClick(slot, false) },
                            onLongClick = { onSlotClick(slot, true) }
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    InfoSlotCell(info)
                }
            }
        }
    }
}

@Composable
private fun InfoSlotCell(info: InfoSlot?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = info?.title ?: "",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = info?.value ?: "",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun RecordingTimeDisplay(activeDuration: Long, distanceUnitUtils: DistanceUnitUtils) {
    Text(
        text = distanceUnitUtils.getHourMinuteSecondTime(activeDuration),
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun HrStatusIcon(connectionState: HeartRateComponent.HeartRateConnectionState) {
    Icon(
        painter = painterResource(connectionState.iconRes),
        contentDescription = null,
        tint = colorResource(connectionState.colorRes),
        modifier = Modifier.size(24.dp),
    )
}

@Composable
fun AutoStartOverlay(
    autoStartState: AutoStartUiState,
    distanceUnitUtils: DistanceUnitUtils,
    onAbort: () -> Unit,
) {
    if (autoStartState == AutoStartUiState.Idle) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) {
            when (autoStartState) {
                is AutoStartUiState.Countdown -> {
                    val text = if (autoStartState.remainingS > 60) {
                        distanceUnitUtils.getMinuteSecondTime(autoStartState.remainingMs)
                    } else {
                        "${autoStartState.remainingS}s"
                    }
                    Text(
                        text = text,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.autoStartCountdownMsg),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
                is AutoStartUiState.WaitingForGps -> {
                    Text(
                        text = stringResource(R.string.autoStartCountdownMsgGps),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                    )
                }
                is AutoStartUiState.WaitingForMove -> {
                    Text(
                        text = stringResource(R.string.autoStartCountdownMsgMove),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                    )
                }
                else -> {}
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onAbort) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
fun StopConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stopRecordingQuestion)) },
        text = { Text(stringResource(R.string.stopRecordingQuestionMessage)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.stop))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.continue_))
            }
        }
    )
}

@Composable
fun CommentDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var comment by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.enterComment)) },
        text = {
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(comment) }) {
                Text(stringResource(R.string.okay))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
