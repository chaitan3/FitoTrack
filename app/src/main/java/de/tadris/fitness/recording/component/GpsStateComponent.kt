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

import android.location.Location
import android.os.SystemClock
import de.tadris.fitness.recording.GpsState
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.event.LocationChangeEvent
import de.tadris.fitness.recording.event.WorkoutGPSStateChanged
import de.tadris.fitness.util.WorkoutLogger.Companion.log
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class GpsStateComponent : RecorderServiceComponent {

    companion object {
        private const val SIGNAL_BAD_THRESHOLD: Double = 30.0 // In meters
        private const val SIGNAL_LOST_THRESHOLD: Int = 10000 // 10 Seconds In milliseconds
    }

    private var gpsState = GpsState.SIGNAL_LOST
    private var lastFix: Location? = null

    override fun register(service: RecorderService) {
        EventBus.getDefault().register(this)
    }

    override fun unregister() {
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    fun onLocationChange(e: LocationChangeEvent) {
        lastFix = e.location
    }

    override fun checkSlow() {
        val lastFix = lastFix ?: return
        val state =
            if ((SystemClock.elapsedRealtimeNanos() - lastFix.elapsedRealtimeNanos) / 1000000L > SIGNAL_LOST_THRESHOLD) {
                GpsState.SIGNAL_LOST
            } else if (lastFix.accuracy > SIGNAL_BAD_THRESHOLD) {
                GpsState.SIGNAL_BAD
            } else {
                GpsState.SIGNAL_GOOD
            }

        if (state != gpsState) {
            log("Recorder", "GPS State: " + this.gpsState.name + " -> " + state.name)
            EventBus.getDefault().post(WorkoutGPSStateChanged(this.gpsState, state))
            gpsState = state
        }
    }
}