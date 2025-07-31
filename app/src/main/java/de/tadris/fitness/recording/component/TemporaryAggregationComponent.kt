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

package de.tadris.fitness.recording.component

import android.hardware.SensorManager
import de.tadris.fitness.data.RouteSample
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.RecordingState
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent
import de.tadris.fitness.recording.event.RouteSampleEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class TemporaryAggregationComponent : RecorderServiceComponent {

    companion object {
        private const val PRESSURE_ELEVATION_AVERAGE = 0.1
    }

    private lateinit var service: RecorderService

    private var lastSample: RouteSample? = null

    private var activeDuration = 0L
    private var pauseDuration = 0L
    private var lastStateChange = 0L // TODO is not updated yet

    private var distance = 0.0
    private var elevation = -1.0
    private var ascent = 0.0
    private var descent = 0.0

    private val timeSinceLastStateChange get() = System.currentTimeMillis() - lastStateChange

    private val currentActiveDuration
        get() = activeDuration +
                if (service.stateHolder.state == RecordingState.RUNNING) timeSinceLastStateChange else 0

    private val currentPauseDuration
        get() = pauseDuration +
                if (service.stateHolder.state.isPause()) timeSinceLastStateChange else 0

    private val calories get() = 0.0 // TODO

    override fun register(service: RecorderService) {
        this.service = service
        EventBus.getDefault().register(this)
    }

    override fun unregister() {
        EventBus.getDefault().unregister(this)
    }

    override fun checkFast() {
        publishAggregatedValues()
    }

    private fun publishAggregatedValues() {
        EventBus.getDefault().post(
            AggregatedRecordingValuesEvent(
                currentPauseDuration,
                currentPauseDuration,
                distance,
                ascent,
                descent,
                calories
            )
        )
    }

    @Subscribe
    fun onNewRouteSample(e: RouteSampleEvent) {
        lastSample?.let { addDifference(it, e.sample) }
        lastSample = e.sample
        addElevation(e.sample)
    }

    private fun addDifference(first: RouteSample, second: RouteSample) {
        distance += first.toLatLong().vincentyDistance(second.toLatLong())
    }

    private fun addElevation(sample: RouteSample) {
        val sampleElevation = getElevationForPressure(sample.pressure)
        if (elevation != -1.0) {
            val newElevation =
                sampleElevation * PRESSURE_ELEVATION_AVERAGE + elevation * (1 - PRESSURE_ELEVATION_AVERAGE) // floating avg
            if (newElevation > elevation) {
                ascent += newElevation - elevation
            } else {
                descent += elevation - newElevation
            }
            elevation = newElevation
        } else {
            elevation = sampleElevation
        }
    }

    private fun getElevationForPressure(pressure: Float): Double {
        return SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure)
            .toDouble()
    }

}