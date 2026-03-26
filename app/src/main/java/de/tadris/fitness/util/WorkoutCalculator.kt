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
package de.tadris.fitness.util

import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.WorkoutEvent
import de.tadris.fitness.data.WorkoutPause
import org.mapsforge.core.model.LatLong

object WorkoutCalculator {

    fun getIntervalSetTimesFromWorkout(data: WorkoutData): List<Long> {
        return data.events
            .asSequence()
            .filter { it.typeId == WorkoutEvent.TYPE_ID_INTERVAL }
            .map { it.relativeTime }
            .toList()
    }

    fun getSetsFromWorkout(data: WorkoutData): List<Set> {
        val workout = data.workout
        val samples = data.repetitionSamples.toMutableList()

        val result = mutableListOf<Set>()

        var lastAbsoluteTimeEnd = workout.start
        val pauses = getPausesFromWorkout(data) + listOf(
            PauseLocation(
                WorkoutPause(-1, -1, workout.end, workout.end, false),
                LatLong(0.0, 0.0),
            )  // Append a last pause so every set ends with a pause
        )

        for (pause in pauses) {
            val set = Set(
                lastAbsoluteTimeEnd,
                pause.pause.startDate - lastAbsoluteTimeEnd,
                0
            )

            while (samples.isNotEmpty() && samples[0].timestamp < set.absoluteTimeStart + set.duration) {
                set.repetitions += samples.removeAt(0).repetitions
            }

            if (set.repetitions > 0) {
                result.add(set)
            }
            lastAbsoluteTimeEnd = pause.pause.endDate
        }

        return result
    }

    @JvmStatic
    fun getPausesFromWorkout(data: WorkoutData) = data.pauses.mapNotNull { pause ->
        data.samples
            .find { it.timestamp > pause.startDate }
            ?.toLatLong()
            ?.let { PauseLocation(pause, it) }
    }

    class Set(
        val absoluteTimeStart: Long,
        var duration: Long,
        var repetitions: Int
    )

    class PauseLocation(val pause: WorkoutPause, val location: LatLong)
}