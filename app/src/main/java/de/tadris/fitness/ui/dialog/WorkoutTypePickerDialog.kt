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
package de.tadris.fitness.ui.dialog

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.tadris.fitness.R
import de.tadris.fitness.data.RecordingType
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.ui.settings.workout_type.WorkoutTypeActivity
import de.tadris.fitness.util.Icon as WorkoutIcon

@Composable
fun WorkoutTypePickerDialog(
    types: List<WorkoutType>,
    onDismiss: () -> Unit,
    onSelect: (WorkoutType) -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            LazyColumn {
                item {
                    ListItem(
                        leadingContent = {
                            Icon(
                                painter = painterResource(WorkoutIcon.getIcon(WorkoutIcon.ADD.name)),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        headlineContent = { Text(stringResource(R.string.workoutTypeAdd)) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            onDismiss()
                            context.startActivity(
                                Intent(context, WorkoutTypeActivity::class.java)
                                    .putExtra(WorkoutTypeActivity.EXTRA_START_CREATE, true)
                            )
                        },
                    )
                }
                items(types) { type ->
                    ListItem(
                        leadingContent = {
                            Icon(
                                painter = painterResource(WorkoutIcon.getIcon(type.icon)),
                                contentDescription = null,
                                tint = Color(type.color),
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        headlineContent = { Text(type.title) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onSelect(type) },
                    )
                }
            }
        },
        confirmButton = {},
    )
}
