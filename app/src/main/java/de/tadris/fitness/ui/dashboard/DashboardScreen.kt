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
package de.tadris.fitness.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.Workout
import de.tadris.fitness.data.WorkoutDao
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.ui.theme.FitoTrackTheme
import de.tadris.fitness.util.Icon
import java.util.Date

@Composable
fun DashboardScreen(
    workoutDao: WorkoutDao,
    onWorkoutClick: (Workout) -> Unit = {},
    onDeleteWorkout: (Workout) -> Unit = {},
    onRecord: () -> Unit = {},
    onRecordLast: (WorkoutType) -> Unit = {},
    onEnterWorkout: () -> Unit = {},
    onImport: () -> Unit = {},
    onSettings: () -> Unit = {},
    onStatistics: () -> Unit = {},
) {
    val workouts by workoutDao.getWorkoutsFlow().collectAsState(initial = emptyList())
    DashboardScreen(
        workouts = workouts,
        onWorkoutClick = onWorkoutClick,
        onDeleteWorkout = onDeleteWorkout,
        onRecord = onRecord,
        onRecordLast = onRecordLast,
        onEnterWorkout = onEnterWorkout,
        onImport = onImport,
        onSettings = onSettings,
        onStatistics = onStatistics,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    workouts: List<Workout>,
    onWorkoutClick: (Workout) -> Unit = {},
    onDeleteWorkout: (Workout) -> Unit = {},
    onRecord: () -> Unit = {},
    onRecordLast: (WorkoutType) -> Unit = {},
    onEnterWorkout: () -> Unit = {},
    onImport: () -> Unit = {},
    onSettings: () -> Unit = {},
    onStatistics: () -> Unit = {},
) {
    val context = LocalContext.current
    var fabExpanded by rememberSaveable { mutableStateOf(false) }
    var workoutToDelete by remember { mutableStateOf<Workout?>(null) }

    val lastWorkoutType = remember(workouts) {
        workouts.firstOrNull()?.getWorkoutType(context)
    }

    if (workoutToDelete != null) {
        AlertDialog(
            onDismissRequest = { workoutToDelete = null },
            title = { Text(stringResource(R.string.deleteWorkout)) },
            text = { Text(stringResource(R.string.deleteWorkoutMessage)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteWorkout(workoutToDelete!!)
                    workoutToDelete = null
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { workoutToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.workouts)) },
                actions = {
                    IconButton(onClick = onStatistics) {
                        Icon(
                            painter = painterResource(R.drawable.outline_bar_chart_24),
                            contentDescription = stringResource(R.string.workout_statistics),
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            DashboardFab(
                expanded = fabExpanded,
                lastWorkoutType = lastWorkoutType,
                onToggle = { fabExpanded = !fabExpanded },
                onRecord = { fabExpanded = false; onRecord() },
                onRecordLast = { type -> fabExpanded = false; onRecordLast(type) },
                onEnterWorkout = { fabExpanded = false; onEnterWorkout() },
                onImport = { fabExpanded = false; onImport() },
            )
        },
    ) { innerPadding ->
        if (workouts.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.hintAddWorkout),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp),
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(workouts, key = { it.id }) { workout ->
                    WorkoutListItem(
                        workout = workout,
                        onClick = { onWorkoutClick(workout) },
                        onLongClick = { workoutToDelete = workout },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutListItem(
    workout: Workout,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val instance = Instance.getInstance(context)
    val workoutType = workout.getWorkoutType(context)

    val dateText = instance.userDateTimeUtils.formatDateTime(Date(workout.start))
    val distanceText = if (workout.distance == 0.0 && workout.repetitions > 0) {
        val reps = workout.repetitions
        "$reps ${context.resources.getQuantityString(workoutType.repeatingExerciseName, reps)}"
    } else {
        instance.distanceUnitUtils.getDistance(workout.distance.toInt())
    }
    val durationText = instance.distanceUnitUtils.getHourMinuteTime(workout.activeDuration)
    val comment = workout.comment?.let {
        if (it.length > 33) it.take(30) + "..." else it
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color(workoutType.color),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(Icon.getIcon(workoutType.icon)),
                        contentDescription = workoutType.title,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = workoutType.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (comment != null) {
                        Text(
                            text = comment,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = distanceText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardFab(
    expanded: Boolean,
    lastWorkoutType: WorkoutType?,
    onToggle: () -> Unit,
    onRecord: () -> Unit,
    onRecordLast: (WorkoutType) -> Unit,
    onEnterWorkout: () -> Unit,
    onImport: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                if (lastWorkoutType != null) {
                    FabAction(
                        label = lastWorkoutType.title,
                        iconRes = Icon.getIcon(lastWorkoutType.icon),
                        onClick = { onRecordLast(lastWorkoutType) },
                    )
                }
                FabAction(
                    label = stringResource(R.string.recordWorkout),
                    iconRes = R.drawable.ic_record_24dp,
                    onClick = onRecord,
                )
                FabAction(
                    label = stringResource(R.string.enterWorkout),
                    iconRes = R.drawable.ic_edit,
                    onClick = onEnterWorkout,
                )
                FabAction(
                    label = stringResource(R.string.importWorkout),
                    iconRes = R.drawable.ic_storage,
                    onClick = onImport,
                )
            }
        }

        FloatingActionButton(onClick = onToggle) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun FabAction(
    label: String,
    iconRes: Int,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 2.dp,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        SmallFloatingActionButton(onClick = onClick) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    val workout = Workout(
        id = 0,
        uuid = "",
        start = System.currentTimeMillis() - 3_600_000,
        end = System.currentTimeMillis(),
        activeDuration = 3_600_000,
        comment = "Morning run",
        distance = 10_000.0,
        repetitions = 0,
        energy = 1_500_000.0,
        workoutTypeId = "running",
        intervalSetUsedId = -1,
    )
    FitoTrackTheme {
        DashboardScreen(workouts = listOf(workout, workout, workout))
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardEmptyPreview() {
    FitoTrackTheme {
        DashboardScreen(workouts = emptyList())
    }
}
