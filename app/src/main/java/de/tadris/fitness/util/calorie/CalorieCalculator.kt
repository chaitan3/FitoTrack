/*
 * Copyright (c) 2022 Jannis Scheibe <jannis@tadris.de>
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
package de.tadris.fitness.util.calorie

import android.content.Context
import de.tadris.fitness.data.Workout
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.preferences.UserMeasurements

class CalorieCalculator(val provider: List<METProvider>) {

    constructor(context: Context) : this(
        listOf(
            METFunctionsProvider,
            FallbackMETProvider,
            WorkoutTypeMETProvider(context)
        )
    )

    /**
     * workoutType, duration, ascent and avgSpeed of workout have to be set
     *
     * @param measurements user measurements which affect the calorie calculation
     * @param workout the workout
     * @return calories burned
     */
    fun calculateCalories(measurements: UserMeasurements, duration: Long, ascent: Int): Int {
        val weight = measurements.weight.toDouble()
        val mins = (duration / 1000).toDouble() / 60
        return (mins * (getMET(measurements) * 3.5 * weight) / 200).toInt() + ascent
    }

    private fun getMET(measurements: UserMeasurements, workout: Workout): Double {
        val speedInKmh = getSpeedInKmh(measurements, workout)

        provider.forEach { provider ->
            val result = provider.calculateMET(measurements, workout.workoutTypeId, speedInKmh)
            result?.let { return it } // return if not null
        }

        return 0.0 // nothing found
    }

    private fun getSpeedInKmh(measurements: UserMeasurements, workout: WorkoutData): Double {


        when (workout) {
            is GpsWorkout -> workout.avgSpeed * 3.6
            is IndoorWorkout ->
                if (workout.hasEstimatedDistance()) workout.estimateSpeed(measurements) * 3.6
                else 0.0
            else -> 0.0
        }
    }

}