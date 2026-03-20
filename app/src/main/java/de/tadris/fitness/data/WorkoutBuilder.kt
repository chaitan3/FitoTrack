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
package de.tadris.fitness.data

import android.content.Context
import de.tadris.fitness.Instance
import de.tadris.fitness.data.preferences.UserMeasurements.Companion.from
import de.tadris.fitness.util.calorie.CalorieCalculator
import java.util.Calendar
import java.util.UUID

class WorkoutBuilder(context: Context?) {
    var workoutType: WorkoutType
    var start: Calendar
    var duration: Long
    var distance = 500.0 // in meters
    var comment: String? = ""
    private var existingWorkout: Workout? = null
    var isFromExistingWorkout: Boolean = false
        private set

    init {
        workoutType = WorkoutTypeManager.getInstance()
            .getWorkoutTypeById(context, WorkoutTypeManager.WORKOUT_TYPE_ID_RUNNING)
        start = Calendar.getInstance()
        duration = 1000L * 60 * 10
    }

    fun create(context: Context): Workout {
        val pauseDuration = if (this.isFromExistingWorkout) existingWorkout!!.pauseDuration else 0L
        val activeDuration = duration - pauseDuration
        val energy = CalorieCalculator(context).calculateEnergy(
            from(context),
            activeDuration,
            0,  // TODO
            workoutType.id,
            distance / (activeDuration / 1000.0)
        )
        return Workout(
            existingWorkout?.id ?: System.nanoTime(),
            existingWorkout?.uuid ?: UUID.randomUUID().toString(),
            start.getTimeInMillis(),
            start.getTimeInMillis() + duration,
            activeDuration,
            comment,
            distance,
            existingWorkout?.repetitions ?: 0,
            energy,
            workoutType.id,
            -1
        )
    }

    fun saveWorkout(context: Context): Workout {
        val workout = create(context)
        if (this.isFromExistingWorkout) {
            updateWorkout(context, workout)
        } else {
            insertWorkout(context, workout)
        }
        return workout
    }

    private fun updateWorkout(context: Context, workout: Workout) {
        Instance.getInstance(context).db.workoutDao().updateWorkout(workout)
    }

    private fun insertWorkout(context: Context, workout: Workout) {
        Instance.getInstance(context).db.workoutDao().insertWorkout(workout)
    }

    companion object {
        @JvmStatic
        fun fromWorkout(context: Context, workout: Workout): WorkoutBuilder {
            return WorkoutBuilder(context).apply {
                isFromExistingWorkout = true
                existingWorkout = workout
                workoutType = workout.getWorkoutType(context)
                start.setTimeInMillis(workout.start)
                duration = workout.activeDuration
                distance = workout.distance
                comment = workout.comment

            }
        }
    }
}
