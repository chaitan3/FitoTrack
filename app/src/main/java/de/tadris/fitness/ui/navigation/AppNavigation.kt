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

package de.tadris.fitness.ui.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import de.tadris.fitness.Instance
import de.tadris.fitness.data.WorkoutPause
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.RecordingStateHolder
import de.tadris.fitness.ui.ImportHandler
import de.tadris.fitness.ui.ImportViewModel
import de.tadris.fitness.ui.dashboard.DashboardScreen
import de.tadris.fitness.ui.record.RecordWorkoutActivity
import de.tadris.fitness.ui.settings.FitoTrackSettingsActivity
import de.tadris.fitness.ui.statistics.StatisticsActivity
import de.tadris.fitness.ui.workout.EnterWorkoutScreen
import de.tadris.fitness.ui.workout.ShareWorkoutScreen
import de.tadris.fitness.ui.workout.show.DiagramType
import de.tadris.fitness.ui.workout.show.EditWorkoutStartEndScreen
import de.tadris.fitness.ui.workout.show.ShowWorkoutFullscreenMapScreen
import de.tadris.fitness.ui.workout.show.ShowWorkoutMapDiagramScreen
import de.tadris.fitness.ui.workout.show.ShowWorkoutScreen
import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Dashboard : Route

    @Serializable
    data class ShowWorkout(val id: Long) : Route

    @Serializable
    data class ShowWorkoutFullscreenMap(val id: Long) : Route

    @Serializable
    data class ShowWorkoutMapDiagram(val id: Long, val diagramType: DiagramType) : Route

    @Serializable
    data class EditWorkoutStartEnd(val id: Long) : Route

    /** id == 0L means create new workout, non-zero means edit existing */
    @Serializable
    data class EnterWorkout(val id: Long = 0L) : Route

    @Serializable
    data class ShareWorkout(val id: Long) : Route
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    onRecord: () -> Unit,
    onRecordLast: (WorkoutType) -> Unit,
) {
    val context = LocalContext.current
    val importVm: ImportViewModel = viewModel()

    ImportHandler(vm = importVm)

    NavHost(navController = navController, startDestination = Route.Dashboard) {
        composable<Route.Dashboard> {
            DashboardScreen(
                workoutDao = Instance.getInstance(context).db.workoutDao(),
                onWorkoutClick = { workout ->
                    navController.navigate(Route.ShowWorkout(workout.id))
                },
                onDeleteWorkout = { workout ->
                    Thread {
                        Instance.getInstance(context).db.workoutDao().deleteWorkout(workout)
                    }.start()
                },
                onRecord = onRecord,
                onRecordLast = onRecordLast,
                onEnterWorkout = { navController.navigate(Route.EnterWorkout()) },
                onImport = { importVm.requestImport() },
                onSettings = {
                    context.startActivity(Intent(context, FitoTrackSettingsActivity::class.java))
                },
                onStatistics = {
                    context.startActivity(Intent(context, StatisticsActivity::class.java))
                },
            )
        }
        composable<Route.ShowWorkoutFullscreenMap> { backStackEntry ->
            ShowWorkoutFullscreenMapScreen(
                id = backStackEntry.toRoute<Route.ShowWorkoutFullscreenMap>().id,
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.ShowWorkoutMapDiagram> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.ShowWorkoutMapDiagram>()
            ShowWorkoutMapDiagramScreen(
                id = route.id,
                diagramType = route.diagramType,
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.EditWorkoutStartEnd> { backStackEntry ->
            EditWorkoutStartEndScreen(
                id = backStackEntry.toRoute<Route.EditWorkoutStartEnd>().id,
                onBack = { navController.popBackStack() },
                onWorkoutModified = { navController.popBackStack() },
            )
        }
        composable<Route.ShareWorkout> { backStackEntry ->
            ShareWorkoutScreen(
                id = backStackEntry.toRoute<Route.ShareWorkout>().id,
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.EnterWorkout> { backStackEntry ->
            EnterWorkoutScreen(
                id = backStackEntry.toRoute<Route.EnterWorkout>().id,
                onBack = { navController.popBackStack() },
                onSaved = { workoutId ->
                    navController.navigate(Route.ShowWorkout(workoutId)) {
                        popUpTo(Route.Dashboard)
                    }
                },
            )
        }
        composable<Route.ShowWorkout> { backStackEntry ->
            ShowWorkoutScreen(
                id = backStackEntry.toRoute<Route.ShowWorkout>().id,
                onBack = { navController.popBackStack() },
                onShowFullscreenMap = { workoutId ->
                    navController.navigate(Route.ShowWorkoutFullscreenMap(workoutId))
                },
                onShowDiagram = { workoutId, diagramType ->
                    navController.navigate(Route.ShowWorkoutMapDiagram(workoutId, diagramType))
                },
                onEditStartEnd = { workoutId ->
                    navController.navigate(Route.EditWorkoutStartEnd(workoutId))
                },
                onEditWorkout = { workoutId ->
                    navController.navigate(Route.EnterWorkout(workoutId))
                },
                onShareWorkout = { workoutId ->
                    navController.navigate(Route.ShareWorkout(workoutId))
                },
                onResumeWorkout = { workout ->
                    val dao = Instance.getInstance(context).db.workoutDao()
                    dao.insertPause(
                        WorkoutPause(
                            workout.id,
                            System.nanoTime(),
                            workout.end,
                            System.currentTimeMillis(),
                            true
                        )
                    )
                    dao.updateWorkout(workout.copy(end = RecordingStateHolder.RUNNING_WORKOUT_END_TIME))
                    context.startForegroundService(Intent(context, RecorderService::class.java))
                    val workoutType = workout.getWorkoutType(context)
                    context.startActivity(
                        Intent(context, workoutType.recordingTypeDecoded.recorderActivityClass)
                            .putExtra(RecordWorkoutActivity.WORKOUT_TYPE_EXTRA, workoutType.id)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            .setAction(RecordWorkoutActivity.RESUME_ACTION)
                    )
                },
            )
        }
    }
}
