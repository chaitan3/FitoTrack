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
package de.tadris.fitness.util.calorie

/**
 * Class which represents a function to get MET based on speed. This class will use linear
 * regression to get a suitable function. This might not be helpful for all sports.
 *
 * hint: upon construction it'll calculate the function. Make sure to avoid instantiating.
 */
class METFunction internal constructor(lookup: Array<SpeedToMET>) {

    private val slope: Double
    private val yOffset: Double

    init {
        if (lookup.isEmpty()) {
            slope = 0.0
            yOffset = 0.0
        } else {
            var sumX = 0.0
            var sumY = 0.0
            var sumProductXY = 0.0
            var sumSquareX = 0.0
            for (speedToMET in lookup) {
                sumX += speedToMET.avgSpeedMph
                sumY += speedToMET.avgMET
                sumProductXY += speedToMET.avgSpeedMph * speedToMET.avgMET
                sumSquareX += speedToMET.avgSpeedMph * speedToMET.avgSpeedMph
            }
            val arithmeticAvgX = sumX / lookup.size
            val arithmeticAvgY = sumY / lookup.size
            val covarianceXY = 1.0 / lookup.size * sumProductXY - arithmeticAvgX * arithmeticAvgY
            val varianceX = 1.0 / lookup.size * sumSquareX - arithmeticAvgX * arithmeticAvgX
            slope = covarianceXY / varianceX
            yOffset = arithmeticAvgY - slope * arithmeticAvgX
        }
    }

    fun getMET(speedInKmh: Double): Double {
        if (slope == 0.0 || yOffset == 0.0) {
            assert(false)
            return 0.0
        }
        val speedInMph = speedInKmh * 0.621371
        return speedInMph * slope + yOffset
    }

    override fun toString() = "${slope}x + $yOffset"
}