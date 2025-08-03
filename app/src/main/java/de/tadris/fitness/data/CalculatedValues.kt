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

import kotlin.math.max
import kotlin.math.min

class CalculatedValues(private val data: WorkoutData) {

    companion object {
        const val ASCENT_FLOATING_CONSTANT = 1.0 // meter
        const val ROUNDED_ELEVATION_WINDOW = 7
    }

    fun getAscentAndDescent(): Pair<Double, Double> {
        val elevationValues = getRoundedElevationsMSL()
        var ascent = 0.0
        var descent = 0.0
        if (elevationValues.isNotEmpty()) {
            var floatingThreshold = elevationValues.first()
            elevationValues.forEach { elevation ->
                val upperLimit = floatingThreshold + ASCENT_FLOATING_CONSTANT
                val lowerLimit = floatingThreshold - ASCENT_FLOATING_CONSTANT

                if(elevation > upperLimit){
                    ascent += elevation - upperLimit
                    floatingThreshold = upperLimit
                }else if(elevation < lowerLimit){
                    descent += lowerLimit - elevation
                    floatingThreshold = lowerLimit
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