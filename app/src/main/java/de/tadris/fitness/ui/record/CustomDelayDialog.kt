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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.ui.components.WheelPicker

@Composable
fun CustomDelayDialog(onConfirm: (Long) -> Unit, onDismiss: () -> Unit) {
    val userPreferences = Instance.getInstance(LocalContext.current).userPreferences
    val initialS = userPreferences.lastAutoStartDelayS
    var minutes by remember { mutableIntStateOf(initialS / 60) }
    var seconds by remember { mutableIntStateOf(initialS % 60) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.popup_auto_start_delay)) },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.autoStartDelayMinutes))
                    WheelPicker(range = 0..59, value = minutes, onValueChange = { minutes = it })
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.autoStartDelaySeconds))
                    WheelPicker(range = 0..59, value = seconds, onValueChange = { seconds = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val totalS = minutes * 60 + seconds
                    userPreferences.lastAutoStartDelayS = totalS
                    onConfirm(totalS * 1000L)
                },
            ) {
                Text(stringResource(R.string.okay))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
