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
package de.tadris.fitness.data.calorie

import android.content.Context
import de.tadris.fitness.data.CalculatedValues
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.preferences.UserMeasurements
import de.tadris.fitness.util.unit.Kcal
import kotlin.math.roundToInt

class CalorieCalculator(val provider: List<METProvider>) {

    constructor(context: Context) : this(
        listOf(
            METFunctionsProvider,
            FallbackMETProvider,
            WorkoutTypeMETProvider(context)
        )
    )

    fun calculateEnergy(context: Context, data: WorkoutData): Double =
        calculateEnergy(
            UserMeasurements.from(context),
            data.workout.activeDuration,
            CalculatedValues(data).getAscentAndDescent().first.roundToInt(),
            data.workout.workoutTypeId,
            data.workout.averageSpeedActive
        )

    fun calculateEnergy(
        measurements: UserMeasurements,
        duration: Long,
        ascent: Int,
        workoutTypeId: String,
        speed: Double // m/s
    ) =
        Kcal().toJoules(calculateCalories(measurements, duration, ascent, workoutTypeId, speed))

    fun calculateCalories(
        measurements: UserMeasurements,
        duration: Long,
        ascent: Int,
        workoutTypeId: String,
        speed: Double // m/s
    ): Double {
        val weight = measurements.weight.toDouble()
        val mins = (duration / 1000).toDouble() / 60
        return (mins * (getMET(measurements, workoutTypeId, speed) * 3.5 * weight) / 200) + ascent
    }

    private fun getMET(
        measurements: UserMeasurements,
        workoutTypeId: String,
        speed: Double // m/s
    ): Double {
        provider.forEach { provider ->
            val result = provider.calculateMET(measurements, workoutTypeId, speed * 3.6)
            result?.let { return it } // return if not null
        }

        return 0.0 // nothing found
    }

}