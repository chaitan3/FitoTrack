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

package de.tadris.fitness.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.data.WorkoutTypeManager
import de.tadris.fitness.map.MapManager
import de.tadris.fitness.osm.OAuthAuthentication
import de.tadris.fitness.ui.dialog.WorkoutTypePickerDialog
import de.tadris.fitness.ui.navigation.AppNavigation
import de.tadris.fitness.ui.navigation.Route
import de.tadris.fitness.ui.record.RecordingConstants
import de.tadris.fitness.ui.theme.FitoTrackTheme
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {

    private val initializer: AppInitializer by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MapManager.initMapProvider(this)

        splashScreen.setKeepOnScreenCondition { initializer.state.value is InitState.Loading }
        initializer.start()

        setContent {
            FitoTrackTheme {
                val state by initializer.state.collectAsStateWithLifecycle()
                var showWorkoutTypeDialog by remember { mutableStateOf(false) }

                when (val s = state) {
                    is InitState.Ready -> {
                        val runningWorkout = s.runningWorkout
                        if (runningWorkout != null) {
                            LaunchedEffect(Unit) {
                                val workoutType = runningWorkout.getWorkoutType(this@MainActivity)
                                startActivity(
                                    Intent(this@MainActivity, workoutType.recordingTypeDecoded.recorderActivityClass)
                                        .putExtra(RecordingConstants.WORKOUT_TYPE_EXTRA, workoutType.id)
                                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        .setAction(RecordingConstants.RESUME_ACTION)
                                )
                                finish()
                            }
                        } else {
                            val navController = rememberNavController()
                            AppNavigation(
                                navController = navController,
                                onRecord = { showWorkoutTypeDialog = true },
                                onRecordLast = { workoutType -> startRecording(workoutType) },
                            )

                            if (showWorkoutTypeDialog) {
                                val types = remember { WorkoutTypeManager.getInstance().getAllTypesSorted(this) }
                                WorkoutTypePickerDialog(
                                    types = types,
                                    onDismiss = { showWorkoutTypeDialog = false },
                                    onSelect = { workoutType ->
                                        showWorkoutTypeDialog = false
                                        startRecording(workoutType)
                                    },
                                    onAddType = {
                                        showWorkoutTypeDialog = false
                                        navController.navigate(Route.EditWorkoutType(""))
                                    },
                                )
                            }
                        }
                    }
                    else -> MigrationScreen(
                        state = s,
                        onErrorDismiss = { finish() },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        OAuthAuthentication.resetCustomTabState(this)
    }

    private fun startRecording(workoutType: WorkoutType) {
        val intent = Intent(this, workoutType.getRecordingTypeDecoded().recorderActivityClass)
        intent.action = RecordingConstants.LAUNCH_ACTION
        intent.putExtra(RecordingConstants.WORKOUT_TYPE_EXTRA, workoutType)
        startActivity(intent)
    }
}
