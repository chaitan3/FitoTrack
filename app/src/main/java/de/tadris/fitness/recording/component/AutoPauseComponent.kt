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

import de.tadris.fitness.data.RecordingType
import de.tadris.fitness.data.preferences.UserPreferences
import de.tadris.fitness.recording.GpsState
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.RecordingState
import de.tadris.fitness.recording.event.WorkoutGPSStateChanged
import org.greenrobot.eventbus.Subscribe

class AutoPauseComponent : LastSampleTimeComponent() {

    companion object {
        const val PAUSE_TIME: Int = 10000 // 10 Seconds
    }

    private var gpsState = GpsState.SIGNAL_LOST
    private var autoPauseEnabled = false

    override fun register(service: RecorderService) {
        super.register(service)
        autoPauseEnabled = UserPreferences(service).useAutoPause
    }

    @Subscribe
    fun onGpsStateChange(e: WorkoutGPSStateChanged) {
        gpsState = e.newState
    }

    override fun checkFast() {
        if (lastSampleTime == 0L) return

        val timeDiff = System.currentTimeMillis() - lastSampleTime
        // check pause
        if (timeDiff > PAUSE_TIME) {
            if (service.stateHolder.state == RecordingState.RUNNING && autoPausePossible()) {
                service.actions.pause(isAutomatic = true)
            }
        } else {
            if (service.stateHolder.state == RecordingState.AUTO_PAUSE) {
                service.actions.resume()
            }
        }
    }

    private fun autoPausePossible(): Boolean {
        return (gpsState != GpsState.SIGNAL_LOST || service.stateHolder.recordingType == RecordingType.INDOOR) && autoPauseEnabled
    }

}