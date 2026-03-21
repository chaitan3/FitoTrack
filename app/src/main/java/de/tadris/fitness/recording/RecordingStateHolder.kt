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
import android.util.Log
import de.tadris.fitness.Instance
import de.tadris.fitness.data.RecordingType
import de.tadris.fitness.data.Workout
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.data.WorkoutTypeManager
import java.util.UUID

class RecordingStateHolder {

    lateinit var workoutType: WorkoutType
    var recordingType: RecordingType = RecordingType.GPS
    var workoutId: Long = -1
    var state = RecordingState.IDLE
        set(value) {
            val oldValue = field
            field = value
            if (oldValue.isPause() && !value.isPause()) {
                pauseDuration += timeSinceLastStateChange
            }
            if (!oldValue.isPause() && value.isPause()) {
                activeDuration += timeSinceLastStateChange
            }

            lastStateChange = System.currentTimeMillis()
        }

    private var activeDuration = 0L
    private var pauseDuration = 0L
    var lastStateChange = 0L
        private set
    private val timeSinceLastStateChange get() = System.currentTimeMillis() - lastStateChange

    val relativeTime get() = currentActiveDuration
    val currentActiveDuration
        get() = activeDuration +
                if (state == RecordingState.RUNNING) timeSinceLastStateChange else 0

    val currentPauseDuration
        get() = pauseDuration +
                if (state.isPause()) timeSinceLastStateChange else 0

    fun initFresh(context: Context, workoutType: WorkoutType) {
        this.workoutType = workoutType
        recordingType = RecordingType.findById(workoutType.recordingType)
        Log.d("RecordingStateHolder", "Init with type=$workoutType recordingType=$recordingType")
        createFreshWorkout(workoutType).also {
            workoutId = it.id
            Instance.getInstance(context).db.workoutDao().insertWorkout(it)
        }
    }

    fun initExisting(context: Context) {
        val existingWorkout = Instance.getInstance(context).db.workoutDao().findRunningWorkout()
            ?: createFreshWorkout(
                WorkoutTypeManager.getInstance()
                    .getWorkoutTypeById(context, WorkoutTypeManager.WORKOUT_TYPE_ID_OTHER)
            )

        workoutType = existingWorkout.getWorkoutType(context)
        recordingType = RecordingType.findById(workoutType.recordingType)
        workoutId = existingWorkout.id
        Log.d("RecordingStateHolder", "Init with type=$workoutType recordingType=$recordingType")

        recoverStateVariables(context, existingWorkout)
    }

    private fun recoverStateVariables(context: Context, workout: Workout) {
        state = RecordingState.RUNNING

        // Estimate pause and active time from recorded samples
        val data = WorkoutData.fromWorkout(context, workout)
        val allSamples = data.samples + data.repetitionSamples + data.events + data.heartRateSamples
        val lastSample = allSamples.maxByOrNull { it.relativeTime }

        activeDuration = lastSample?.relativeTime ?: 0
        pauseDuration = data.pauses.sumOf { it.duration }
        lastStateChange = lastSample?.timestamp ?: System.currentTimeMillis()
    }

    private fun createFreshWorkout(workoutType: WorkoutType) = Workout(
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