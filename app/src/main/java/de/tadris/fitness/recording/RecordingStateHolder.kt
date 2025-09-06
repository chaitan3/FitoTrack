/*
 * Copyright (c) 2025 Jannis Scheibe <jannis@tadris.de>
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

package de.tadris.fitness.recording

import android.content.Context
import de.tadris.fitness.Instance
import de.tadris.fitness.data.RecordingType
import de.tadris.fitness.data.Workout
import de.tadris.fitness.data.WorkoutType
import java.util.UUID

class RecordingStateHolder {

    lateinit var workoutType: WorkoutType
    var recordingType: RecordingType = RecordingType.GPS
    var workoutId: Long = -1
    var state = RecordingState.IDLE
    var pausedSince = 0L

    fun init(context: Context){
        recordingType = RecordingType.findById(workoutType.recordingType)
        createFreshWorkout().also {
            workoutId = it.id
            Instance.getInstance(context).db.workoutDao().insertWorkout(it)
        }
    }

    private fun createFreshWorkout() = Workout(
        id = System.nanoTime(),
        uuid = UUID.randomUUID().toString(),
        start = -1,
        end = -1,
        activeDuration = 0,
        comment = "",
        distance = 0.0,
        repetitions = 0,
        energy = 0.0,
        workoutTypeId = workoutType.id,
        intervalSetUsedId = -1
    )

}