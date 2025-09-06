/*
 * Copyright (c) 2020 Jannis Scheibe <jannis@tadris.de>
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

data class WorkoutData(
    val workout: Workout,
    val samples: List<RouteSample>,
    val pauses: List<WorkoutPause>,
    val events: List<WorkoutEvent>,
    val repetitionSamples: List<RepetitionSample>,
    val heartRateSamples: List<HeartRateSample>,
) {

    val hasRepetitions get() = repetitionSamples.isNotEmpty()

    val hasIntensityData get() = hasRepetitions && repetitionSamples.last().intensity > 0.0

    val hasRouteData get() = samples.isNotEmpty()

    val hasHRData get() = heartRateSamples.isNotEmpty()

    companion object {
        @JvmStatic
        fun fromWorkout(context: Context?, workout: Workout): WorkoutData {
            val database = Instance.getInstance(context).db
            return fromWorkout(database.workoutDao(), workout)
        }

        fun fromWorkout(workoutDao: WorkoutDao, workout: Workout): WorkoutData {
            return WorkoutData(
                workout,
                listOf(*workoutDao.getAllSamplesOfWorkout(workout.id)),
                listOf(*workoutDao.getAllPausesOfWorkout(workout.id)),
                listOf(*workoutDao.getEventsOfWorkout(workout.id)),
                listOf(*workoutDao.getAllRepetitionSamplesOfWorkout(workout.id)),
                listOf(*workoutDao.getAllHeartRateSamplesOfWorkout(workout.id)),
            )
        }
    }
}
