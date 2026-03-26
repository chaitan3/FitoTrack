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

class WorkoutCutter(
    context: Context,
    data: WorkoutData,
    private val startSample: RouteSample?,
    private val endSample: RouteSample?,
) : AbstractWorkoutSaver(context, data) {

    override fun save() {
        if (startSample != null) {
            cutStart(startSample)
        }
        if (endSample != null) {
            cutEnd(endSample)
        }

        calculatePauses() // Update pauses
        calculateRelativeTimes() // Update relative time as start time and pauses might have changed
        calculateAggregatedValues() // Recalculate data

        updateWorkoutAndSamples()
    }

    private fun cutStart(startSample: RouteSample) {
        val removedSamples = mutableListOf<RouteSample>()

        for(sample in routeSamples){
            if(sample.id == startSample.id) break
            removedSamples += sample
        }

        deleteSamples(removedSamples)
        workout = workout.copy(start = startSample.timestamp)
    }

    private fun cutEnd(endSample: RouteSample) {
        val removedSamples = mutableListOf<RouteSample>()

        var found = false
        for(sample in routeSamples){
            if(found){
                removedSamples += sample
            }else if(sample.id == endSample.id){
                found = true
            }
        }

        deleteSamples(removedSamples)
        workout = workout.copy(end = endSample.timestamp)
    }

    private fun deleteSamples(samples: List<RouteSample>) {
        for(sample in samples){
            Instance.getInstance(context).db.workoutDao().deleteSample(sample)
        }
        routeSamples = routeSamples.filter { it !in samples }
    }

}