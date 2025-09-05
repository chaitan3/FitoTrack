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

class PullupRecognizer : ExerciseRecognizer() {

    companion object {

        private const val TAG = "PullupRecognizer"

        private const val SMOOTHING = 0.02

        private const val PULL_THRESHOLD = 10.2
        private const val RELAX_THRESHOLD = 9.65

        val TIME1_MIN = 500 // ms - minimum duration to the next pull-up
        val TIME2_RANGE = 400..2000 // ms - duration of a pull-up

    }

    override fun getActivatedSensors() = listOf(FitoTrackSensorOption.ACCELERATION)

    private var lastPullupDetected = 0L
    private var lastRelaxDetected = 0L

    private var smoothedAcceleration = 0.0
    private var maxAcceleration = 0.0
    private var state = MotionState.RELAXING
        set(value) {
            field = value
            Log.d(TAG, "State: $state")
        }

    private val maxIntensity get() = (maxAcceleration - 9.81) * 10

    @Subscribe
    fun onSensorEvent(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val absoluteAcceleration =
                sqrt(event.values[0].pow(2) + event.values[1].pow(2) + event.values[2].pow(2)).toDouble() // in m/s²

            onAccelerationDetected(absoluteAcceleration)
        }
    }

    private fun onAccelerationDetected(absoluteAcceleration: Double) {
        smoothedAcceleration =
            (1 - SMOOTHING) * smoothedAcceleration + SMOOTHING * absoluteAcceleration
        maxAcceleration = max(maxAcceleration, smoothedAcceleration)

        when {
            state == MotionState.RELAXING && smoothedAcceleration > PULL_THRESHOLD -> {
                state = MotionState.PULLING
                lastPullupDetected = System.currentTimeMillis()
            }

            state == MotionState.PULLING && smoothedAcceleration < RELAX_THRESHOLD -> {
                state = MotionState.RELAXING
                detectPullup()
                lastRelaxDetected = System.currentTimeMillis()
            }
        }
    }

    private fun detectPullup() {
        val time1 = lastPullupDetected - lastRelaxDetected
        val time2 = System.currentTimeMillis() - lastPullupDetected

        val valid = time1 > TIME1_MIN && time2 in TIME2_RANGE
        Log.d(
            TAG,
            "Detected pullup with t1=$time1 t2=$time2 intensity=$maxIntensity -> valid=$valid"
        )

        if (valid) {
            EventBus.getDefault().post(
                RepetitionRecognizedEvent(
                    RepetitionSample.REPETITION_TYPE_PULL_UP,
                    lastPullupDetected,
                    maxIntensity
                )
            )
        }

        maxAcceleration = 0.0
    }

    enum class MotionState {
        PULLING,
        RELAXING
    }

}