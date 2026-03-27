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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.osm.OAuthAuthentication
import de.tadris.fitness.ui.dialog.SelectWorkoutTypeDialog
import de.tadris.fitness.ui.navigation.AppNavigation
import de.tadris.fitness.ui.record.RecordWorkoutActivity
import de.tadris.fitness.ui.theme.FitoTrackTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FitoTrackTheme {
                AppNavigation(
                    onRecord = { showWorkoutSelection() },
                    onRecordLast = { workoutType -> startRecording(workoutType) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        OAuthAuthentication.resetCustomTabState(this)
    }

    private fun showWorkoutSelection() {
        SelectWorkoutTypeDialog(this) { workoutType: WorkoutType -> startRecording(workoutType) }.show()
    }

    private fun startRecording(workoutType: WorkoutType) {
        val intent = Intent(this, workoutType.getRecordingTypeDecoded().recorderActivityClass)
        intent.action = RecordWorkoutActivity.LAUNCH_ACTION
        intent.putExtra(RecordWorkoutActivity.WORKOUT_TYPE_EXTRA, workoutType)
        startActivity(intent)
    }
}
