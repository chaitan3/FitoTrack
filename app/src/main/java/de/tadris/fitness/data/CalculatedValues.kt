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

package de.tadris.fitness.data

import android.content.Context
import kotlin.math.max
import kotlin.math.min

class CalculatedValues(private val data: WorkoutData) {

    constructor(context: Context, workout: Workout) : this(
        WorkoutData.fromWorkout(
            context,
            workout
        )
    )

    companion object {
        const val ASCENT_FLOATING_CONSTANT = 2.0 // meter
        const val ROUNDED_ELEVATION_WINDOW = 5
    }

    val topSpeed by lazy { data.samples.maxOf { it.speed } }
    val minElevationMSL by lazy { data.samples.minOf { it.elevationMSL } }
    val maxElevationMSL by lazy { data.samples.maxOf { it.elevationMSL } }
    val maxFrequency by lazy { data.repetitionSamples.maxOf { it.frequency } }
    val ascentDescent by lazy { getAscentAndDescent() }
    val ascent by lazy { ascentDescent.first }
    val descent by lazy { ascentDescent.second }

    val maxHeartRate by lazy { data.heartRateSamples.maxOfOrNull { it.heartRate } }
    val averageHeartRate by lazy { if(data.hasHRData) data.heartRateSamples.averageBy { it.heartRate.toDouble() } else null }

    val averageIntensity by lazy { if(data.hasRepetitions) data.repetitionSamples.averageBy { it.intensity } else 0.0 }
    val maxIntensity by lazy { if(data.hasRepetitions) data.repetitionSamples.maxOf { it.intensity } else 0.0 }

    fun getAscentAndDescent(): Pair<Double, Double> {
        val elevationValues = getRoundedElevationsMSL()
        var ascent = 0.0
        var descent = 0.0

        if (elevationValues.isNotEmpty()) {
            var floatingThreshold = elevationValues.first()

            for (elevation in elevationValues) {
                val upperLimit = floatingThreshold + ASCENT_FLOATING_CONSTANT
                val lowerLimit = floatingThreshold - ASCENT_FLOATING_CONSTANT

                when {
                    elevation > upperLimit -> {
                        // add the amount beyond the band and slide the band up
                        ascent += elevation - upperLimit
                        floatingThreshold = elevation - ASCENT_FLOATING_CONSTANT
                    }
                    elevation < lowerLimit -> {
                        // add the amount beyond the band and slide the band down
                        descent += lowerLimit - elevation
                        floatingThreshold = elevation + ASCENT_FLOATING_CONSTANT
                    }
                }
            }
        }
        return ascent to descent
    }


    fun getRoundedElevationsMSL(): List<Double> {
        return List(data.samples.size) { index ->
            val minIndex = max(index - ROUNDED_ELEVATION_WINDOW, 0)
            val maxIndex = min(index + ROUNDED_ELEVATION_WINDOW, data.samples.size - 1)
            data.samples.subList(minIndex, maxIndex).averageBy { it.elevationMSL }
        }
    }

    private inline fun <T> List<T>.averageBy(selector: (T) -> Double) = sumOf(selector) / size

}