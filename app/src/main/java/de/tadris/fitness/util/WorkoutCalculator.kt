/*
 * Copyright (c) 2023 Jannis Scheibe <jannis@tadris.de>
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
package de.tadris.fitness.util

import de.tadris.fitness.data.BaseSample
import de.tadris.fitness.data.RouteSample
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.WorkoutPause
import org.mapsforge.core.model.LatLong

object WorkoutCalculator {
    fun getSetsFromWorkout(data: IndoorWorkoutData): MutableList<Set?> {
        val workout: IndoorWorkout = data.getWorkout()
        val samples: MutableList<IndoorSample?> = ArrayList<Any?>(data.getSamples())
        val result: MutableList<Set?> = ArrayList<Set?>()

        var lastAbsoluteTimeEnd: Long = workout.start
        var lastRelativeTimeEnd: Long = 0
        val pauses = getPausesFromWorkout(data.castToBaseWorkoutData())
        pauses.add(
            PauseLocation(
                workout.end,
                workout.duration,
                0,
                null
            )
        ) // Append a last pause so every set ends with a pause

        for (pause in pauses) {
            val set = Set(
                lastAbsoluteTimeEnd,
                lastRelativeTimeEnd,
                pause.relativeTimeStart - lastRelativeTimeEnd,
                0
            )

            while (samples.size > 0 && samples.get(0).relativeTime < set.relativeTimeStart + set.duration) {
                set.repetitions += samples.removeAt(0).repetitions
            }

            if (set.repetitions > 0) {
                result.add(set)
            }
            lastAbsoluteTimeEnd = pause.absoluteTimeStart + pause.duration
            lastRelativeTimeEnd = pause.relativeTimeStart + pause.duration
        }

        return result
    }

    fun calculatePauseDuration(data: BaseWorkoutData): Long {
        var pauseDuration: Long = 0
        for (pause in getPausesFromWorkout(data)) {
            pauseDuration += pause.duration
        }

        return pauseDuration
    }

    @JvmStatic
    fun getPausesFromWorkout(data: WorkoutData) = data.pauses.mapNotNull { pause ->
        data.samples
            .find { it.timestamp < pause.startDate }
            ?.toLatLong()
            ?.let { PauseLocation(pause, it) }
    }

    /**
     * Returns a list of relative times when intervals were triggered
     */
    fun getIntervalSetTimesFromWorkout(data: BaseWorkoutData): MutableList<Long?> {
        val result: MutableList<Long?> = ArrayList<Long?>()
        val samples: MutableList<BaseSample> = data.getSamples()

        for (sample in samples) {
            if (sample.intervalTriggered > 0) {
                result.add(sample.relativeTime)
            }
        }
        return result
    }

    class Set(
        val absoluteTimeStart: Long,
        val relativeTimeStart: Long,
        var duration: Long,
        var repetitions: Int
    )

    class PauseLocation(val pause: WorkoutPause, val location: LatLong)
}