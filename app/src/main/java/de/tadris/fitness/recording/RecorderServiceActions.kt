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

package de.tadris.fitness.recording

import de.tadris.fitness.data.WorkoutPause
import de.tadris.fitness.recording.actions.WorkoutActionDiscard
import de.tadris.fitness.recording.actions.WorkoutActionPause
import de.tadris.fitness.recording.actions.WorkoutActionResume
import de.tadris.fitness.recording.actions.WorkoutActionStart
import de.tadris.fitness.recording.actions.WorkoutActionStop
import de.tadris.fitness.recording.event.RecordingStateChangeEvent
import de.tadris.fitness.util.WorkoutLogger.Companion.log
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class RecorderServiceActions(private val service: RecorderService) {

    var state
        get() = service.stateHolder.state
        set(value) {
            val oldState = service.stateHolder.state
            service.stateHolder.state = value
            EventBus.getDefault().postSticky(
                RecordingStateChangeEvent(
                    service.stateHolder.workoutId,
                    oldState,
                    value
                )
            )
        }

    companion object {
        private const val TAG = "RecorderServiceActions"
    }

    fun register() {
        EventBus.getDefault().register(this)
    }

    fun unregister() {
        EventBus.getDefault().unregister(this)
    }

    @Subscribe
    fun receiveStart(e: WorkoutActionStart) {
        start(e.reason)
    }

    @Subscribe
    fun receiveResume(e: WorkoutActionResume) {
        resume()
    }

    @Subscribe
    fun receiveDiscard(e: WorkoutActionDiscard) {
        discard()
    }

    @Subscribe
    fun receivePause(e: WorkoutActionPause) {
        pause(e.isAutomatic)
    }

    @Subscribe
    fun receiveStop(e: WorkoutActionStop) {
        stop(e.reason)
    }

    fun start(reason: StartReason) {
        log(TAG, "Called start, reason: ${reason.logMessage}")
        if (state == RecordingState.IDLE) {
            log(TAG, "Start")
            resume()
            service.instance.db.workoutDao().editWorkout(service.stateHolder.workoutId) {
                it.copy(start = System.currentTimeMillis())
            }
        } else if (state.isPause()) {
            resume()
        } else check(state == RecordingState.RUNNING) { "Cannot start or resume recording. state = $state" }
    }

    fun resume() {
        log(TAG, "Called resume")
        if (state.isPause()) {
            service.instance.db.workoutDao().insertPause(
                WorkoutPause(
                    service.stateHolder.workoutId,
                    System.nanoTime(),
                    service.stateHolder.lastStateChange,
                    System.currentTimeMillis(),
                    state == RecordingState.MANUAL_PAUSE
                )
            )
        }
        state = RecordingState.RUNNING
    }

    fun pause(isAutomatic: Boolean) {
        if (state == RecordingState.RUNNING) {
            log("Recorder", "Pause, isAutomatic=$isAutomatic")
            state = if (isAutomatic) {
                RecordingState.AUTO_PAUSE
            } else {
                RecordingState.MANUAL_PAUSE
            }
        }
    }

    fun stop(reason: StopReason) {
        log(TAG, "Called stop, reason: ${reason.logMessage}")
        pause(isAutomatic = true)
        state = RecordingState.STOPPED
        service.instance.db.workoutDao().editWorkout(service.stateHolder.workoutId) {
            it.copy(end = System.currentTimeMillis())
        }
        WorkoutFinalizer(service, service.instance.db.workoutDao(), service.stateHolder.workoutId).finalize()
        service.stopSelf()
    }

    fun discard(){
        log(TAG, "Discard called")
        service.instance.db.workoutDao().getWorkoutById(service.stateHolder.workoutId)?.let {
            service.instance.db.workoutDao().deleteWorkout(it)
        }
        service.stopSelf()
    }

}