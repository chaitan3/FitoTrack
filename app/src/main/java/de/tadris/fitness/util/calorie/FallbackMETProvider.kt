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

import de.tadris.fitness.data.preferences.UserMeasurements
import kotlin.math.max
import kotlin.math.pow

/**
 * calorie calculation based on @link { https://sites.google.com/site/compendiumofphysicalactivities/Activity-Categories }
 *
 *
 * How do we get to this calculation from the values of the compendium of physical activities?
 * Easy: we have given some points like
 * - 5kmh -> 4 MET or
 * - 8kmh -> 6 MET.
 * Using regression the function "MET = f(speedInKmh)" can be created.
 * That's what you can see below for different activities.
 */
object FallbackMETProvider : METProvider {

    override fun calculateMET(measurements: UserMeasurements, typeId: String, speedInKmh: Double) =
        when (typeId) {
            "running", "walking", "hiking", "treadmill" -> max(3.0, speedInKmh * 0.97)
            "cycling" -> max(
                3.5,
                0.00818 * speedInKmh.pow(2.0) + 0.1925 * speedInKmh + 1.13
            )
            "inline_skating" -> max(3.0, 0.6747 * speedInKmh - 2.1893)
            "skateboarding" -> max(4.0, 0.43 * speedInKmh + 0.89)
            "rowing" -> max(
                2.5,
                0.18 * speedInKmh.pow(2.0) - 1.375 * speedInKmh + 5.2
            )
            else -> null // no result
        }
}