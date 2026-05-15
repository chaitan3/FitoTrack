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

import de.tadris.fitness.data.HeartRateSample
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.RecordingState
import de.tadris.fitness.recording.event.HeartRateChangeEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class HeartRateSampleComponent : RecorderServiceComponent {

    private lateinit var service: RecorderService

    override fun register(service: RecorderService) {
        this.service = service
        EventBus.getDefault().register(this)
    }

    override fun unregister() {
        EventBus.getDefault().unregister(this)
    }

    @Subscribe
    fun onHeartRateChange(e: HeartRateChangeEvent) {
        val state = service.stateHolder.state
        if (state != RecordingState.RUNNING && state != RecordingState.AUTO_PAUSE) return

        val now = System.currentTimeMillis()
        val sample = HeartRateSample(
            workoutId = service.stateHolder.workoutId,
            id = System.nanoTime(),
            timestamp = now,
            relativeTime = service.stateHolder.relativeTime,
            heartRate = e.heartRate,
        )
        service.instance.db.workoutDao().insertSample(sample)
    }
}
