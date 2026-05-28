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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.util.Icon

@Composable
fun PreStartBottomSheet(
    workoutType: WorkoutType,
    canStart: Boolean,
    voiceFeedbackAvailable: Boolean,
    useAutoPause: Boolean,
    onStart: () -> Unit,
    onAutoStartCustomDelay: (() -> Unit)? = null,
    onAutoStartWaitForGps: (() -> Unit)? = null,
    onAutoStartOnMove: (() -> Unit)? = null,
    onBluetooth: () -> Unit,
    onIntervalSet: () -> Unit,
    onSettings: () -> Unit,
) {
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = navBottomPadding + 16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Workout type icon box
            val iconResId = Icon.getIcon(workoutType.icon)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(workoutType.color),
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (iconResId != 0) {
                        Icon(
                            painter = painterResource(iconResId),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }

            // Title and subtitle
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = workoutType.title,
                    style = MaterialTheme.typography.titleLarge,
                )
                val subtitle = if (useAutoPause) "Auto-pause on" else "Auto-pause off"
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                )
            }
        }

        // Chip row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            AssistChip(
                onClick = onBluetooth,
                label = { Text(stringResource(R.string.recording_pair_sensor)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_bluetooth),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
            AssistChip(
                onClick = onIntervalSet,
                label = { Text(stringResource(R.string.recording_intervals)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_list),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // GPS hint text (shown when GPS is not ready)
        val gpsHintAlpha by animateFloatAsState(if (canStart) 0f else 1f)
        Text(
            text = stringResource(R.string.recording_waiting_gps_start_anyway),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .alpha(gpsHintAlpha),
        )

        // Start button row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onStart() },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(workoutType.color),
                ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start ${workoutType.title}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            val showTimerButton = onAutoStartCustomDelay != null || onAutoStartWaitForGps != null || onAutoStartOnMove != null
            if (showTimerButton) {
                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    FilledTonalIconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_horiz),
                            contentDescription = stringResource(R.string.pref_auto_start_delay_title),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (onAutoStartCustomDelay != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.popup_auto_start_delay)) },
                                onClick = { menuExpanded = false; onAutoStartCustomDelay() },
                            )
                        }
                        if (onAutoStartWaitForGps != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.popup_auto_start_wait_for_gps)) },
                                onClick = { menuExpanded = false; onAutoStartWaitForGps() },
                            )
                        }
                        if (onAutoStartOnMove != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.popup_auto_start_on_move)) },
                                onClick = { menuExpanded = false; onAutoStartOnMove() },
                            )
                        }
                    }
                }
            }
        }
    }
}
