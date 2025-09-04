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

package de.tadris.fitness.recording.event

import de.tadris.fitness.data.RecordingType
import de.tadris.fitness.data.WorkoutType
import de.tadris.fitness.recording.RecordingState
import org.greenrobot.eventbus.EventBus

data class AggregatedRecordingValuesEvent(
    val recordingType: RecordingType,
    val workoutType: WorkoutType,
    val currentState: RecordingState,
    val activeDuration: Long, // in ms
    val pauseDuration: Long, // in ms
    val distance: Double, // in m
    val ascent: Double, // in m
    val descent: Double, // in m
    val energy: Double, // in joule
    val currentSpeed: Double, // in m/s
    val currentHeartRate: Int,
    val currentHeartRateBattery: Int,
    val currentFrequency: Double,
    val currentIntensity: Double,
    val repetitions: Int,
    val sampleCount: Int,
) {

    companion object {

        private val EMPTY = AggregatedRecordingValuesEvent(
            RecordingType.GPS,
            WorkoutType(),
            RecordingState.IDLE,
            0L, 0L, 0.0, 0.0, 0.0,
            0.0, 0.0, 0, -1,
            0.0, 0.0, 0, 0
        )

        fun getCurrent() = getCurrentOptional() ?: EMPTY

        fun getCurrentOptional(): AggregatedRecordingValuesEvent? =
            EventBus.getDefault().getStickyEvent(AggregatedRecordingValuesEvent::class.java)

    }

    val totalDuration get() = activeDuration + pauseDuration

    val averageFrequency get() = repetitions / (activeDuration / 1000.0) // 1/s

    val averageSpeedMotion get() = distance / (activeDuration / 1000.0) // m/s

    val averageSpeedTotal get() = distance / (totalDuration / 1000.0) // m/s

    // min/km
    val averagePaceMotion
        get() =
            if (averageSpeedMotion < 0.001) 0.0
            else (1 / averageSpeedMotion) * 1000 / 60

    val recordedSomething get() = sampleCount >= 2

}