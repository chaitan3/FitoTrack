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

package de.tadris.fitness.ui.workout.show

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.tadris.fitness.R
import de.tadris.fitness.data.WorkoutEvent
import de.tadris.fitness.ui.workout.diagram.HeartRateConverter
import de.tadris.fitness.ui.workout.diagram.HeightConverter
import de.tadris.fitness.ui.workout.diagram.SampleConverter
import de.tadris.fitness.ui.workout.diagram.SpeedConverter
import de.tadris.fitness.ui.workout.diagram.WorkoutDiagram
import de.tadris.fitness.ui.map.WorkoutMapColoring
import de.tadris.fitness.ui.map.WorkoutMapContentState
import de.tadris.fitness.ui.map.WorkoutMapScreen
import kotlinx.serialization.Serializable

@Serializable
enum class DiagramType { SPEED, HEIGHT, HEART_RATE }

@Composable
fun ShowWorkoutMapDiagramScreen(
    id: Long,
    diagramType: DiagramType,
    onBack: () -> Unit,
) {
    WorkoutMapScreen(
        id = id,
        onBack = onBack,
        isDiagramScreen = true,
        bottomContent = { state ->
            DiagramPanel(state = state, defaultDiagramType = diagramType)
        },
    )
}

@Composable
private fun DiagramPanel(
    state: WorkoutMapContentState,
    defaultDiagramType: DiagramType,
) {
    val data = state.data
    val hasIntervals = remember(data) {
        data.events.any { it.typeId == WorkoutEvent.TYPE_ID_INTERVAL }
    }

    val defaultConverter = remember(defaultDiagramType) {
        state.converterManager.availableConverters.firstOrNull { converter ->
            when (defaultDiagramType) {
                DiagramType.HEIGHT -> converter is HeightConverter
                DiagramType.HEART_RATE -> converter is HeartRateConverter
                DiagramType.SPEED -> converter is SpeedConverter
            }
        } ?: state.converterManager.availableConverters.firstOrNull()
    }

    var selectedConverters by remember {
        mutableStateOf(listOfNotNull(defaultConverter))
    }

    LaunchedEffect(defaultConverter) {
        defaultConverter?.let { state.onColoringChange(WorkoutMapColoring.Custom(it)) }
    }
    var showIntervals by remember { mutableStateOf(true) }
    var showPickerDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // Interval toggle
        if (hasIntervals) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            ) {
                Checkbox(checked = showIntervals, onCheckedChange = { showIntervals = it })
                Text(stringResource(R.string.showIntervals))
            }
        }

        // Converter selector row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPickerDialog = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (selectedConverters.isEmpty()) {
                    stringResource(R.string.nothingSelected)
                } else {
                    selectedConverters.joinToString(", ") { it.name }
                },
                modifier = Modifier.padding(start = 4.dp),
                maxLines = 1,
            )
        }

        // Diagram
        WorkoutDiagram(
            converters = selectedConverters.ifEmpty { listOfNotNull(defaultConverter) },
            data = data,
            workout = data.workout,
            interactive = true,
            showIntervals = showIntervals,
            onSelectionChanged = state.onSampleSelected,
            modifier = Modifier.fillMaxSize(),
        )
    }

    if (showPickerDialog) {
        ConverterPickerDialog(
            available = state.converterManager.availableConverters,
            selected = selectedConverters,
            onDismiss = { showPickerDialog = false },
            onConfirm = { newSelection ->
                selectedConverters = newSelection
                newSelection.firstOrNull()?.let {
                    state.onColoringChange(WorkoutMapColoring.Custom(it))
                }
                showPickerDialog = false
            },
        )
    }
}

@Composable
private fun ConverterPickerDialog(
    available: List<SampleConverter>,
    selected: List<SampleConverter>,
    onDismiss: () -> Unit,
    onConfirm: (List<SampleConverter>) -> Unit,
) {
    var current by remember { mutableStateOf(selected.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.selectColoring)) },
        text = {
            Column {
                available.forEach { converter ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                current = if (current.contains(converter)) {
                                    if (current.size > 1) current - converter else current
                                } else {
                                    if (current.size < 2) current + converter else current
                                }
                            },
                    ) {
                        Checkbox(
                            checked = current.contains(converter),
                            onCheckedChange = null,
                        )
                        Text(converter.name, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(available.filter { current.contains(it) }) }) {
                Text(stringResource(R.string.okay))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
