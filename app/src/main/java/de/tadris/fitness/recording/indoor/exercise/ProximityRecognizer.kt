/*
 * Copyright (c) 2021 Jannis Scheibe <jannis@tadris.de>
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
import de.tadris.fitness.data.RepetitionSample
import de.tadris.fitness.recording.component.FitoTrackSensorOption
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class ProximityRecognizer : ExerciseRecognizer() {

    override fun getActivatedSensors() = listOf(FitoTrackSensorOption.PROXIMITY)

    private var lastState = false // true if last sensor value was below the threshold

    @Subscribe
    fun onSensorEvent(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] < 2) {
                if (!lastState) {
                    EventBus.getDefault()
                        .post(RepetitionRecognizedEvent(RepetitionSample.REPETITION_TYPE_PUSH_UP, System.currentTimeMillis()))
                    lastState = true
                }
            } else {
                lastState = false
            }
        }
    }

}