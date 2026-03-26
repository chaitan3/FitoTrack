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

package de.tadris.fitness.recording.indoor.exercise

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.util.Log
import de.tadris.fitness.data.RepetitionSample
import de.tadris.fitness.recording.component.FitoTrackSensorOption
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class JumpRecognizer(private val maxJumpDuration: Long) : ExerciseRecognizer() {

    companion object {
        /**
         * If absolute acceleration is smaller than this threshold, it means that the user is currently falling
         * in m/s²
         */
        const val THRESHOLD_FALLING = 2.5

        const val THRESHOLD_JUMPING = 20.0 // m/s²
    }

    override fun getActivatedSensors() = listOf(FitoTrackSensorOption.ACCELERATION)

    private var lastJumpDetected: Long = 0

    private var thisJumpMaxAcceleration = 0.0
    private var lastJumpMaxAcceleration = 0.0

    private var motionState = MotionState.RELAXING
    //set(value) { field = value; Log.d("JumpRecognizer", "State detected: " + value.name) }

    @Subscribe
    fun onSensorEvent(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val absoluteAcceleration =
                sqrt(event.values[0].pow(2) + event.values[1].pow(2) + event.values[2].pow(2)).toDouble() // in m/s²

            if (motionState == MotionState.RELAXING && absoluteAcceleration < THRESHOLD_FALLING) {
                motionState = MotionState.PREPARE
                lastJumpDetected = System.currentTimeMillis()
            } else if ((motionState == MotionState.PREPARE || motionState == MotionState.FALLING) && absoluteAcceleration > THRESHOLD_JUMPING && absoluteAcceleration > lastJumpMaxAcceleration * 0.6) {
                motionState = MotionState.JUMPING
                lastJumpDetected = System.currentTimeMillis()
            } else if (motionState == MotionState.JUMPING && absoluteAcceleration < THRESHOLD_FALLING) {
                motionState = MotionState.FALLING

                val intensity = thisJumpMaxAcceleration
                EventBus.getDefault().post(RepetitionRecognizedEvent(RepetitionSample.REPETITION_TYPE_JUMP, lastJumpDetected, intensity))

                lastJumpMaxAcceleration = thisJumpMaxAcceleration
                Log.d("JumpRecognizer", "max acceleration: $lastJumpMaxAcceleration")
                thisJumpMaxAcceleration = 0.0
            } else if (motionState != MotionState.RELAXING && System.currentTimeMillis() - lastJumpDetected > maxJumpDuration) {
                motionState = MotionState.RELAXING
                lastJumpMaxAcceleration = 0.0
            }

            if (motionState == MotionState.JUMPING) {
                thisJumpMaxAcceleration = max(thisJumpMaxAcceleration, absoluteAcceleration)
            }
        }
    }

    enum class MotionState {
        RELAXING,
        PREPARE,
        JUMPING,
        FALLING,
    }

}