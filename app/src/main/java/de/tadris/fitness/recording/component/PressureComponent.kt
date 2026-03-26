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

package de.tadris.fitness.recording.component

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.event.PressureChangeEvent
import de.tadris.fitness.util.WorkoutLogger
import org.greenrobot.eventbus.EventBus

class PressureComponent : RecorderServiceComponent, SensorEventListener {

    companion object {
        const val TAG = "PressureComponent"
    }

    private var sensorManager: SensorManager? = null

    private var pressureSensor: Sensor? = null

    override fun register(service: RecorderService) {
        register(service as Context)
    }

    fun register(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        pressureSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)
        if (pressureSensor != null) {
            WorkoutLogger.log(TAG, "started Pressure Sensor")
            sensorManager?.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            WorkoutLogger.log(TAG, "no Pressure Sensor Available")
        }
    }

    override fun unregister() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        EventBus.getDefault().post(PressureChangeEvent(event.values[0]))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
}