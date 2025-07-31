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

import de.tadris.fitness.data.preferences.UserPreferences
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.RecordingState
import de.tadris.fitness.recording.event.WorkoutAutoStopEvent
import de.tadris.fitness.util.WorkoutLogger.Companion.log
import org.greenrobot.eventbus.EventBus

class AutoStopComponent : LastSampleTimeComponent() {

    companion object {
        private const val AUTO_TIMEOUT_MULTIPLIER = 1000 * 60L // minutes to ms
    }

    private var autoTimeoutMs = 0L

    override fun register(service: RecorderService) {
        autoTimeoutMs = UserPreferences(service).autoTimeout * AUTO_TIMEOUT_MULTIPLIER
    }

    override fun checkSlow() {
        if (lastSampleTime == 0L) return
        val timeDiff = System.currentTimeMillis() - lastSampleTime

        if (autoTimeoutMs > 0 && timeDiff > autoTimeoutMs && service.stateHolder.state == RecordingState.AUTO_PAUSE) {
            log("WorkoutRecorder", "Auto timeout was set to: $autoTimeoutMs")
            service.actions.stop("Auto timeout, timediff: $timeDiff")
            EventBus.getDefault().post(WorkoutAutoStopEvent())
        }
    }

}