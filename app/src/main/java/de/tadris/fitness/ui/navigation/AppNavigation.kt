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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.tadris.fitness.Instance
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.ui.dashboard.DashboardScreen
import de.tadris.fitness.ui.settings.FitoTrackSettingsActivity
import de.tadris.fitness.ui.statistics.StatisticsActivity
import de.tadris.fitness.ui.workout.ShowWorkoutActivity
import de.tadris.fitness.ui.workout.WorkoutActivity
import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Dashboard : Route
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    onRecord: () -> Unit,
    onRecordLast: (WorkoutType) -> Unit,
    onEnterWorkout: () -> Unit,
    onImport: () -> Unit,
) {
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = Route.Dashboard) {
        composable<Route.Dashboard> {
            DashboardScreen(
                workoutDao = Instance.getInstance(context).db.workoutDao(),
                onWorkoutClick = { workout ->
                    val intent = Intent(context, ShowWorkoutActivity::class.java)
                    intent.putExtra(WorkoutActivity.WORKOUT_ID_EXTRA, workout.id)
                    context.startActivity(intent)
                },
                onDeleteWorkout = { workout ->
                    Thread {
                        Instance.getInstance(context).db.workoutDao().deleteWorkout(workout)
                    }.start()
                },
                onRecord = onRecord,
                onRecordLast = onRecordLast,
                onEnterWorkout = onEnterWorkout,
                onImport = onImport,
                onSettings = {
                    context.startActivity(Intent(context, FitoTrackSettingsActivity::class.java))
                },
                onStatistics = {
                    context.startActivity(Intent(context, StatisticsActivity::class.java))
                },
            )
        }
    }
}
