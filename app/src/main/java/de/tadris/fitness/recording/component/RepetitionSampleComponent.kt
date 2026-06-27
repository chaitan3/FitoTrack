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

import de.tadris.fitness.data.RepetitionSample
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.RecordingState
import de.tadris.fitness.recording.event.RecordingStateChangeEvent
import de.tadris.fitness.recording.event.RepetitionSampleEvent
import de.tadris.fitness.recording.indoor.exercise.RepetitionRecognizedEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class RepetitionSampleComponent : RecorderServiceComponent {

    private lateinit var service: RecorderService
    val minRepetitionCount get() = service.stateHolder.workoutType.minDistance
    var lastPush = 0L
    val repetitionAggregation = mutableListOf<RepetitionRecognizedEvent>()

    override fun register(service: RecorderService) {
        this.service = service
        EventBus.getDefault().register(this)
    }

    override fun unregister() {
        EventBus.getDefault().unregister(this)
    }

    @Subscribe
    fun onRepetitionDetected(e: RepetitionRecognizedEvent){
        val state = service.stateHolder.state
        if (state != RecordingState.RUNNING && state != RecordingState.AUTO_PAUSE) return
        repetitionAggregation.add(e)
        if(repetitionAggregation.size >= minRepetitionCount){
            pushSample()
        }
    }

    @Subscribe
    fun onRecordingStateChanged(e: RecordingStateChangeEvent){
        pushSample()
    }

    private fun pushSample(){
        if(repetitionAggregation.isEmpty()) return

        val startTime = repetitionAggregation.minOf { it.timestamp }
        val endTime = repetitionAggregation.maxOf { it.timestamp }
        val repetitions = repetitionAggregation.size
        val frequency =
            if(lastPush != 0L) repetitions / ((endTime - lastPush) / 1000.0)
            else if(endTime - startTime > 500) repetitions / ((endTime - startTime) / 1000.0)
            else 0.0

        val sample = RepetitionSample(
            service.stateHolder.workoutId,
            0,
            startTime,
            endTime,
            service.stateHolder.relativeTime,
            repetitionAggregation.first().type,
            repetitions,
            repetitionAggregation.sumOf { it.intensity } / repetitionAggregation.size,
            frequency
        )

        lastPush = endTime
        repetitionAggregation.clear()
        service.instance.db.workoutDao().insertSample(sample)
        EventBus.getDefault().post(RepetitionSampleEvent(sample))
    }

}