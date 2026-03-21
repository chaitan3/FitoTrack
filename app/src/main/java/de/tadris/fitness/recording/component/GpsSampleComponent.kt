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

import android.location.Location
import de.tadris.fitness.data.RouteSample
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.RecordingState
import de.tadris.fitness.recording.component.GpsComponent.Companion.locationToLatLong
import de.tadris.fitness.recording.event.LocationChangeEvent
import de.tadris.fitness.recording.event.PressureChangeEvent
import de.tadris.fitness.recording.event.RouteSampleEvent
import de.tadris.fitness.util.LocationUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import kotlin.math.abs

class GpsSampleComponent : RecorderServiceComponent {

    private var minDistance = 10

    private lateinit var service: RecorderService
    private var lastSample: RouteSample? = null
    private var lastPressure: Float = -1f

    override fun register(service: RecorderService) {
        this.service = service
        minDistance = service.stateHolder.workoutType.minDistance
        EventBus.getDefault().register(this)
    }

    override fun unregister() {
        EventBus.getDefault().unregister(this)
    }

    @Subscribe
    fun onPressureChanged(e: PressureChangeEvent) {
        this.lastPressure = e.pressure
    }

    @Subscribe
    fun onLocationChanged(e: LocationChangeEvent) {
        val location = e.location
        val lastSample = this.lastSample
        if (lastSample != null) {
            val distance =
                abs(locationToLatLong(location).sphericalDistance(lastSample.toLatLong()))
            val timediff = abs(lastSample.timestamp - LocationUtils.getTimeFor(location))
            if (distance < minDistance || timediff < RecorderService.MIN_DURATION_DIFF) {
                return
            }
        }

        if (service.stateHolder.state == RecordingState.RUNNING ||
            service.stateHolder.state == RecordingState.AUTO_PAUSE
        ) {
            val sample = createSampleFromLocation(location)
            saveSample(sample)
            EventBus.getDefault().post(RouteSampleEvent(sample))
            this.lastSample = sample
        }
    }

    private fun createSampleFromLocation(location: Location) = RouteSample(
        service.stateHolder.workoutId,
        LocationUtils.getTimeFor(location),
        LocationUtils.getTimeFor(location),
        service.stateHolder.relativeTime,
        location.latitude,
        location.longitude,
        location.altitude,
        0.0,
        location.speed.toDouble(),
        lastPressure
    )

    private fun saveSample(sample: RouteSample) {
        service.instance.db.workoutDao().insertSample(sample)
    }

}