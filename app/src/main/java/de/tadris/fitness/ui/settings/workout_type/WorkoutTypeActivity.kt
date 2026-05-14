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

package de.tadris.fitness.ui.settings.workout_type

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import de.tadris.fitness.ui.navigation.Route
import de.tadris.fitness.ui.theme.FitoTrackTheme

class WorkoutTypeActivity : ComponentActivity() {

    companion object {
        /** Pass true to start directly on the create-new screen instead of the list. */
        const val EXTRA_START_CREATE = "start_create"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startCreate = intent.getBooleanExtra(EXTRA_START_CREATE, false)
        val startDestination: Route =
            if (startCreate) Route.EditWorkoutType("") else Route.ManageWorkoutTypes

        setContent {
            FitoTrackTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = startDestination) {
                    composable<Route.ManageWorkoutTypes> {
                        ManageWorkoutTypesScreen(
                            onBack = { finish() },
                            onEditType = { typeId ->
                                navController.navigate(Route.EditWorkoutType(typeId ?: ""))
                            },
                        )
                    }
                    composable<Route.EditWorkoutType> { backStackEntry ->
                        EditWorkoutTypeScreen(
                            typeId = backStackEntry.toRoute<Route.EditWorkoutType>().typeId,
                            onBack = {
                                if (!navController.popBackStack()) finish()
                            },
                        )
                    }
                }
            }
        }
    }
}
