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

import de.tadris.fitness.data.WorkoutTypeManager
import de.tadris.fitness.data.preferences.UserMeasurements

/**
 * Data is from here: https://sites.google.com/site/compendiumofphysicalactivities/Activity-Categories
 * Also find more workout types on this site :)
 */
object METFunctionsProvider : METProvider {

    private val metFunctions: MutableMap<String, METFunction> = HashMap()

    init {
        METFunction(
            arrayOf(
                SpeedToMET(2.0, 2.8),
                SpeedToMET(2.5, 3.0),
                SpeedToMET(3.0, 3.5),
                SpeedToMET(3.5, 4.3),
                SpeedToMET(4.0, 5.0),
                SpeedToMET(5.0, 8.3)
            )
        ).let {
            metFunctions[WorkoutTypeManager.WORKOUT_TYPE_ID_HIKING] = it
            metFunctions[WorkoutTypeManager.WORKOUT_TYPE_ID_WALKING] = it
        }
        metFunctions[WorkoutTypeManager.WORKOUT_TYPE_ID_RUNNING] = METFunction(
            arrayOf(
                SpeedToMET(4.0, 6.0),
                SpeedToMET(5.0, 8.3),
                SpeedToMET(5.2, 9.0),
                SpeedToMET(6.0, 9.8),
                SpeedToMET(6.7, 10.5),
                SpeedToMET(7.0, 11.0),
                SpeedToMET(7.5, 11.8),
                SpeedToMET(8.0, 11.8),
                SpeedToMET(8.6, 12.3),
                SpeedToMET(9.0, 12.8),
                SpeedToMET(10.0, 14.5),
                SpeedToMET(11.0, 16.0),
                SpeedToMET(12.0, 19.0),
                SpeedToMET(13.0, 19.8),
                SpeedToMET(14.0, 23.0)
            )
        )
        metFunctions[WorkoutTypeManager.WORKOUT_TYPE_ID_CYCLING] = METFunction(
            arrayOf(
                SpeedToMET(5.5, 3.5),
                SpeedToMET(9.4, 5.8),
                SpeedToMET(11.0, 6.8),
                SpeedToMET(13.0, 8.0),
                SpeedToMET(15.0, 10.0),
                SpeedToMET(17.5, 12.0)
            )
        )
    }

    override fun calculateMET(measurements: UserMeasurements, typeId: String, speedInKmh: Double) =
        metFunctions[typeId]?.getMET(speedInKmh)

    override fun toString() =
        metFunctions.entries.joinToString(separator = "\n") { "- ${it.key}: ${it.value}" }
}