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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.tadris.fitness.R

@Composable
fun StartButtonRow(
    canStart: Boolean,
    isAutoStartSupported: Boolean,
    onStart: () -> Unit,
    onStartImmediate: () -> Unit,
    onStartOnMove: () -> Unit,
    onStartWaitForGps: () -> Unit,
    onStartWithDelay: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = onStart, enabled = canStart, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.start))
        }
        if (isAutoStartSupported) {
            Spacer(modifier = Modifier.width(8.dp))
            var showPopup by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showPopup = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = showPopup, onDismissRequest = { showPopup = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.popup_auto_start_immediately)) },
                        onClick = { showPopup = false; onStartImmediate() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.popup_auto_start_on_move)) },
                        onClick = { showPopup = false; onStartOnMove() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.popup_auto_start_wait_for_gps)) },
                        onClick = { showPopup = false; onStartWaitForGps() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.popup_auto_start_delay)) },
                        onClick = { showPopup = false; onStartWithDelay() }
                    )
                }
            }
        }
    }
}
