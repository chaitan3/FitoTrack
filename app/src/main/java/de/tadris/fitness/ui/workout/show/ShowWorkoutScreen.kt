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

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.Workout
import de.tadris.fitness.data.WorkoutTypeManager
import de.tadris.fitness.data.preferences.UserMeasurements
import de.tadris.fitness.osm.OAuthAuthentication
import de.tadris.fitness.ui.theme.spacing
import de.tadris.fitness.ui.workout.EditWorkoutStartEndActivity
import de.tadris.fitness.ui.workout.EnterWorkoutActivity
import de.tadris.fitness.ui.workout.ShareWorkoutActivity
import de.tadris.fitness.ui.workout.ShowWorkoutMapDiagramActivity
import de.tadris.fitness.ui.workout.WorkoutActivity
import de.tadris.fitness.ui.workout.diagram.FrequencyConverter
import de.tadris.fitness.ui.workout.diagram.HeartRateConverter
import de.tadris.fitness.ui.workout.diagram.HeightConverter
import de.tadris.fitness.ui.workout.diagram.IntensityConverter
import de.tadris.fitness.ui.workout.diagram.SpeedConverter
import de.tadris.fitness.util.sections.SectionListModel
import de.tadris.fitness.util.sections.SectionListPresenter
import de.tadris.fitness.util.sections.SectionListView
import de.tadris.fitness.util.unit.UnitUtils
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun ShowWorkoutScreen(
    id: Long,
    onBack: () -> Unit,
    onResumeWorkout: (Workout) -> Unit,
) {
    val context = LocalContext.current
    val vm: ShowWorkoutViewModel = viewModel(
        key = "workout_$id",
        factory = viewModelFactory {
            initializer {
                ShowWorkoutViewModel(
                    context.applicationContext as android.app.Application,
                    id,
                )
            }
        }
    )
    val state by vm.uiState.collectAsState()

    when (val s = state) {
        is WorkoutUiState.Loading -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        is WorkoutUiState.Error -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(s.message)
        }

        is WorkoutUiState.Success -> ShowWorkoutContent(
            state = s,
            onBack = onBack,
            onUpdateComment = { vm.updateComment(it) },
            onDelete = { vm.deleteWorkout(onBack) },
            onResumeWorkout = { onResumeWorkout(s.workout) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowWorkoutContent(
    state: WorkoutUiState.Success,
    onBack: () -> Unit,
    onUpdateComment: (String) -> Unit,
    onDelete: () -> Unit,
    onResumeWorkout: () -> Unit,
) {
    val context = LocalContext.current
    val workout = state.workout
    val data = state.data
    val calculatedValues = state.calculatedValues
    val sets = state.sets

    val instance = remember { Instance.getInstance(context) }
    val distanceUnitUtils = remember { instance.distanceUnitUtils }
    val energyUnitUtils = remember { instance.energyUnitUtils }
    val dateTimeUtils = remember { instance.userDateTimeUtils }

    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showCommentDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showResumeDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout.getWorkoutType(context).title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.actionBack)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more)
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = { showMenu = false; showDeleteDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.editWorkout)) },
                            onClick = {
                                showMenu = false
                                context.startActivity(
                                    Intent(context, EnterWorkoutActivity::class.java)
                                        .putExtra(EnterWorkoutActivity.WORKOUT_ID_EXTRA, workout.id)
                                )
                            }
                        )
                        if (data.hasRouteData) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.exportAsGpxFile)) },
                                onClick = {
                                    showMenu = false
                                    // TODO
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.actionUploadToOSM)) },
                                onClick = {
                                    showMenu = false
                                    context.startActivity(
                                        Intent(
                                            context,
                                            OAuthAuthentication::class.java
                                        )
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.editWorkoutStartEnd)) },
                                onClick = {
                                    showMenu = false
                                    context.startActivity(
                                        Intent(context, EditWorkoutStartEndActivity::class.java)
                                            .putExtra(WorkoutActivity.WORKOUT_ID_EXTRA, workout.id)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.share)) },
                                onClick = {
                                    showMenu = false
                                    context.startActivity(
                                        Intent(context, ShareWorkoutActivity::class.java)
                                            .putExtra(WorkoutActivity.WORKOUT_ID_EXTRA, workout.id)
                                    )
                                }
                            )
                        }
                        if (state.isLastWorkout) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.resumeWorkout)) },
                                onClick = { showMenu = false; showResumeDialog = true }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = MaterialTheme.spacing.defaultFull),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.defaultHalf),
        ) {
            // Comment
            val commentText = if (!workout.comment.isNullOrEmpty())
                stringResource(R.string.comment) + ": " + workout.comment
            else
                stringResource(R.string.noComment)
            Text(
                text = commentText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCommentDialog = true }
                    .padding(vertical = 12.dp)
            )

            // Time
            SectionHeader(stringResource(R.string.workoutTime))
            KeyValueRow(
                stringResource(R.string.workoutDate),
                dateTimeUtils.formatDate(Date(workout.start))
            )
            KeyValueRow(
                stringResource(R.string.workoutDuration),
                distanceUnitUtils.getHourMinuteSecondTime(workout.activeDuration),
                stringResource(R.string.workoutPauseDuration),
                distanceUnitUtils.getHourMinuteSecondTime(workout.pauseDuration)
            )
            KeyValueRow(
                stringResource(R.string.workoutStartTime),
                dateTimeUtils.formatTime(Date(workout.start)),
                stringResource(R.string.workoutEndTime),
                dateTimeUtils.formatTime(Date(workout.end))
            )

            // Distance / Repetitions
            if (data.repetitionSamples.isEmpty()) {
                KeyValueRow(
                    stringResource(R.string.workoutDistance),
                    distanceUnitUtils.getDistance(workout.distance.toInt()),
                    stringResource(R.string.workoutPace),
                    distanceUnitUtils.getPace(workout.averagePaceActive)
                )
            } else {
                KeyValueRow(
                    stringResource(R.string.workoutRepetitions),
                    workout.repetitions.toString(),
                    stringResource(R.string.workoutSets),
                    sets.size.toString()
                )
                if (workout.workoutTypeId == WorkoutTypeManager.WORKOUT_TYPE_ID_TREADMILL) {
                    val measurements = UserMeasurements.from(context)
                    val estimatedDistance =
                        (measurements.stepLength * workout.repetitions).roundToInt()
                    val estimatedSpeed = estimatedDistance / (workout.activeDuration / 1000.0)
                    KeyValueRow(
                        stringResource(R.string.approxSymbol) + stringResource(R.string.workoutDistance),
                        distanceUnitUtils.getDistance(estimatedDistance),
                        stringResource(R.string.approxSymbol) + stringResource(R.string.workoutSpeed),
                        distanceUnitUtils.getSpeed(estimatedSpeed)
                    )
                }
            }

            // Route map
            if (data.hasRouteData) {
                SectionHeader(stringResource(R.string.workoutRoute))
                WorkoutMap(
                    data = data,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mapHeightDp().dp)
                )
            }

            // Speed
            if (data.hasRouteData) {
                SectionHeader(stringResource(R.string.workoutSpeed))
                KeyValueRow(
                    stringResource(R.string.avgSpeedInMotion),
                    distanceUnitUtils.getSpeed(workout.averageSpeedActive),
                    stringResource(R.string.avgSpeedTotalShort),
                    distanceUnitUtils.getSpeed(workout.averageSpeedTotal)
                )
                KeyValueRow(
                    stringResource(R.string.workoutTopSpeed),
                    distanceUnitUtils.getSpeed(calculatedValues.topSpeed)
                )
                WorkoutDiagram(
                    converter = SpeedConverter(context),
                    data = data,
                    workout = workout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((mapHeightDp() / 2).dp),
                    onClick = {
                        context.startActivity(
                            Intent(context, ShowWorkoutMapDiagramActivity::class.java)
                                .putExtra(WorkoutActivity.WORKOUT_ID_EXTRA, workout.id)
                                .putExtra(
                                    ShowWorkoutMapDiagramActivity.DIAGRAM_TYPE_EXTRA,
                                    ShowWorkoutMapDiagramActivity.DIAGRAM_TYPE_SPEED
                                )
                        )
                    }
                )
                SectionHeader(stringResource(R.string.histogram))
                SpeedHistogram(
                    data = data,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((mapHeightDp() / 2).dp)
                )
            }

            // Frequency
            if (data.hasRepetitions) {
                SectionHeader(stringResource(R.string.workoutFrequency))
                if (data.repetitionSamples.isNotEmpty()) {
                    KeyValueRow(
                        stringResource(R.string.workoutAvgFrequencyShort),
                        UnitUtils.round(
                            workout.averageFrequency,
                            2
                        ) + " " + UnitUtils.unitHertzShort,
                        stringResource(R.string.workoutMaxFrequency),
                        UnitUtils.round(
                            calculatedValues.maxFrequency,
                            2
                        ) + " " + UnitUtils.unitHertzShort
                    )
                    WorkoutDiagram(
                        converter = FrequencyConverter(context),
                        data = data,
                        workout = workout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((mapHeightDp() / 2).dp)
                    )
                } else {
                    KeyValueRow(
                        stringResource(R.string.workoutAvgFrequencyShort),
                        UnitUtils.round(
                            workout.averageFrequency,
                            2
                        ) + " " + UnitUtils.unitHertzShort
                    )
                }
            }

            // Heart rate
            if (data.hasHRData) {
                SectionHeader(stringResource(R.string.workoutHeartRate))
                KeyValueRow(
                    stringResource(R.string.workoutAvgHeartRate),
                    calculatedValues.averageHeartRate?.roundToInt()
                        .toString() + " " + stringResource(R.string.unitHeartBeatsPerMinute),
                    stringResource(R.string.workoutMaxHeartRate),
                    (calculatedValues.maxHeartRate
                        ?: 0).toString() + " " + stringResource(R.string.unitHeartBeatsPerMinute)
                )
                WorkoutDiagram(
                    converter = HeartRateConverter(context),
                    data = data,
                    workout = workout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((mapHeightDp() / 2).dp),
                    onClick = {
                        context.startActivity(
                            Intent(context, ShowWorkoutMapDiagramActivity::class.java)
                                .putExtra(WorkoutActivity.WORKOUT_ID_EXTRA, workout.id)
                                .putExtra(
                                    ShowWorkoutMapDiagramActivity.DIAGRAM_TYPE_EXTRA,
                                    ShowWorkoutMapDiagramActivity.DIAGRAM_TYPE_HEART_RATE
                                )
                        )
                    }
                )
            }

            // Energy
            SectionHeader(stringResource(R.string.workoutBurnedEnergy))
            KeyValueRow(
                stringResource(R.string.workoutTotalEnergy),
                energyUnitUtils.getEnergy(workout.energy),
                stringResource(R.string.workoutEnergyConsumption),
                energyUnitUtils.getRelativeEnergy(workout.energy / (workout.activeDuration.toDouble() / 1000 / 60))
            )

            // Height
            if (data.hasRouteData) {
                SectionHeader(stringResource(R.string.height))
                KeyValueRow(
                    stringResource(R.string.workoutMinHeight),
                    distanceUnitUtils.getElevation(calculatedValues.minElevationMSL.roundToInt()),
                    stringResource(R.string.workoutMaxHeight),
                    distanceUnitUtils.getElevation(calculatedValues.maxElevationMSL.roundToInt())
                )
                KeyValueRow(
                    stringResource(R.string.workoutAscent),
                    distanceUnitUtils.getElevation(calculatedValues.ascent.roundToInt()),
                    stringResource(R.string.workoutDescent),
                    distanceUnitUtils.getElevation(calculatedValues.descent.roundToInt())
                )
                WorkoutDiagram(
                    converter = HeightConverter(context),
                    data = data,
                    workout = workout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((mapHeightDp() / 2).dp),
                    onClick = {
                        context.startActivity(
                            Intent(context, ShowWorkoutMapDiagramActivity::class.java)
                                .putExtra(WorkoutActivity.WORKOUT_ID_EXTRA, workout.id)
                                .putExtra(
                                    ShowWorkoutMapDiagramActivity.DIAGRAM_TYPE_EXTRA,
                                    ShowWorkoutMapDiagramActivity.DIAGRAM_TYPE_HEIGHT
                                )
                        )
                    }
                )
                SectionHeader(stringResource(R.string.sections))
                AndroidView(
                    factory = { ctx ->
                        val listView = SectionListView(ctx)
                        val listModel = SectionListModel(data)
                        SectionListPresenter(listView, listModel)
                        listView
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Intensity
            if (data.hasIntensityData && data.repetitionSamples.isNotEmpty()) {
                SectionHeader(stringResource(R.string.workoutIntensity))
                KeyValueRow(
                    stringResource(R.string.workoutAvgIntensityShort),
                    UnitUtils.round(calculatedValues.averageIntensity, 2),
                    stringResource(R.string.workoutMaxIntensity),
                    UnitUtils.round(calculatedValues.maxIntensity, 2)
                )
                WorkoutDiagram(
                    converter = IntensityConverter(context),
                    data = data,
                    workout = workout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((mapHeightDp() / 2).dp)
                )
            }

            Spacer(Modifier.height(MaterialTheme.spacing.defaultFull))
        }
    }

    // Comment dialog
    if (showCommentDialog) {
        var commentText by rememberSaveable { mutableStateOf(workout.comment ?: "") }
        AlertDialog(
            onDismissRequest = { showCommentDialog = false },
            title = { Text(stringResource(R.string.enterComment)) },
            text = {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateComment(commentText)
                    showCommentDialog = false
                }) { Text(stringResource(R.string.okay)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCommentDialog = false
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Delete dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.deleteWorkout)) },
            text = { Text(stringResource(R.string.deleteWorkoutMessage)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Resume dialog
    if (showResumeDialog) {
        AlertDialog(
            onDismissRequest = { showResumeDialog = false },
            title = { Text(stringResource(R.string.resumeWorkout)) },
            text = { Text(stringResource(R.string.resumeWorkoutConfirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    showResumeDialog = false
                    onResumeWorkout()
                }) { Text(stringResource(R.string.actionResume)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showResumeDialog = false
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

// ─── Section helpers ────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider(Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun KeyValueRow(
    label1: String,
    value1: String,
    label2: String? = null,
    value2: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label1,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value1, style = MaterialTheme.typography.bodyLarge)
        }
        if (label2 != null && value2 != null) {
            Column(Modifier.weight(1f)) {
                Text(
                    label2,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(value2, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

// ─── Map ────────────────────────────────────────────────────────────────────


private fun mapHeightDp(): Int {
    return 300
}
