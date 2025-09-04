/*
 * Copyright (c) 2022 Jannis Scheibe <jannis@tadris.de>
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

package de.tadris.fitness.recording.component

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import de.tadris.fitness.Instance
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.indoor.exercise.ExerciseRecognizer
import de.tadris.fitness.util.WorkoutLogger
import org.greenrobot.eventbus.EventBus

/**
 * Activity recognition for indoor workouts
 */
class ExerciseRecognitionComponent : RecorderServiceComponent, SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var exerciseRecognizer: ExerciseRecognizer? = null

    override fun register(service: RecorderService) {
        init(service)
        if (exerciseRecognizer != null) {
            WorkoutLogger.log(
                "RecoderService",
                "Using ${exerciseRecognizer!!.javaClass.simpleName} recognizer"
            )
            exerciseRecognizer!!.start()
            exerciseRecognizer!!.getActivatedSensors().forEach {
                activateSensor(it)
            }
        } else {
            WorkoutLogger.log("RecoderService", "No recognizer found")
        }
    }

    fun init(context: RecorderService) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        exerciseRecognizer = ExerciseRecognizer.findByType(context.stateHolder.workoutType.id)
    }

    override fun unregister() {
        sensorManager?.unregisterListener(this)
        exerciseRecognizer?.stop()
    }

    fun activateSensor(sensorOption: FitoTrackSensorOption) {
        val sensor = sensorManager?.getDefaultSensor(sensorOption.sensorType)
        if (sensor != null) {
            WorkoutLogger.log("RecoderService", "Activating sensor ${sensorOption.name}")
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        EventBus.getDefault().post(event)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}