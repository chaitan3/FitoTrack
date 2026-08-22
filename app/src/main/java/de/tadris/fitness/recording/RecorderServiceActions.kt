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

package de.tadris.fitness.recording

import android.util.Log
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
    
    /**
     * Master state of the workout recording.
     */
    var state
        get() = service.stateHolder.state
        set(value) {
            val oldState = service.stateHolder.state
            service.stateHolder.state = value
            val event =
                RecordingStateChangeEvent(
                    service.stateHolder.workoutId,
                    oldState,
                    value
                )
            Log.d(TAG, ": $event")
            EventBus.getDefault().postSticky(event)
        }

    companion object {
        private const val TAG = "RecorderServiceActions"
    }

    fun register() {
        EventBus.getDefault().register(this)
        EventBus.getDefault().postSticky(
            RecordingStateChangeEvent(service.stateHolder.workoutId, state, state)
        )
    }

    fun unregister() {
        EventBus.getDefault().unregister(this)
    }

    @Subscribe
    fun receiveStart(e: WorkoutActionStart) {
        Log.d(TAG, "receiveStart $e")
        start(e.reason)
    }

    @Subscribe
    fun receiveResume(e: WorkoutActionResume) {
        Log.d(TAG, "receiveResume $e")
        resume()
    }

    @Subscribe
    fun receiveDiscard(e: WorkoutActionDiscard) {
        Log.d(TAG, "receiveDiscard $e")
        discard()
    }

    @Subscribe
    fun receivePause(e: WorkoutActionPause) {
        Log.d(TAG, "receivePause $e")
        pause(e.isAutomatic)
    }

    @Subscribe
    fun receiveStop(e: WorkoutActionStop) {
        Log.d(TAG, "receiveStop $e")
        stop(e.reason)
    }

    @Synchronized
    fun start(reason: StartReason) {
        log(TAG, "Called start, reason: ${reason.logMessage} state: $state")
        if (state == RecordingState.IDLE) {
            log(TAG, "Start")
            state = RecordingState.RUNNING
            service.instance.db.workoutDao().editWorkout(service.stateHolder.workoutId) {
                it.copy(start = System.currentTimeMillis())
            }
        } else if (state.isPause()) {
            resume()
        }
        // The only state left is STOPPED or RUNNING
        check(state == RecordingState.RUNNING) { "Cannot start or resume recording. state: $state" }
    }

    private fun insertPause() {
        Log.d(TAG, "insertPause() from state $state")
        service.instance.db.workoutDao().insertPause(
            WorkoutPause(
                service.stateHolder.workoutId,
                0,
                service.stateHolder.lastStateChange,
                System.currentTimeMillis(),
                state == RecordingState.MANUAL_PAUSE || state == RecordingState.STOPPED
            )
        )
    }
    
    /**
     * Resume a started recording. Will not resume a [RecordingState.STOPPED] recording.
     */
    @Synchronized
    fun resume() {
        log(TAG, "Called resume")
        if (state.isPause()) {
            log(TAG, "resuming from $state")
            insertPause()
            state = RecordingState.RUNNING
        } else
            assert(false, { "Called resume() before pause() in ReorderServiceActions" })
    }
    
    /**
     * Pause a running recording.
     */
    @Synchronized
    fun pause(isAutomatic: Boolean) {
        if (state == RecordingState.RUNNING || (!isAutomatic && state == RecordingState.AUTO_PAUSE)) {
            log(TAG, "Pause, isAutomatic=$isAutomatic")
            state = if (isAutomatic) {
                RecordingState.AUTO_PAUSE
            } else {
                RecordingState.MANUAL_PAUSE
            }
        }
    }
    
    /**
     * Stop recording, don't stop the service.
     */
    @Synchronized
    fun stop(reason: StopReason) {
        log(TAG, "Called stop, reason: ${reason.logMessage} state: $state")
        
        // Should these two special cases be errors? Stopping when already stopped or stopping before starting.
        if (state == RecordingState.STOPPED) {
            log(TAG, "Already stopped")
            return
        }
        if (state == RecordingState.IDLE) {
            log(TAG, "Was in IDLE, nothing to stop")
            return
        }

        log(TAG, "Stopping and finalizing workout...")
        if (state.isPause()) {
            insertPause() // Append the pause that was in progress when stopping
        }
        state = RecordingState.STOPPED

        service.instance.db.workoutDao().editWorkout(service.stateHolder.workoutId) {
            it.copy(end = System.currentTimeMillis())
        }
        WorkoutFinalizer(service, service.instance.db.workoutDao(), service.stateHolder.workoutId).finalizeWorkout()
    }
    
    /**
     * Remove the current recording from the database, regardless of its current state.
     * Does not stop the service.
     */
    fun discard(){
        log(TAG, "Discard called")
        val workout = service.instance.db.workoutDao().getWorkoutById(service.stateHolder.workoutId)
        if (workout != null) {
            log(TAG, "Deleting workout from DB")
            service.instance.db.workoutDao().deleteWorkout(workout)
        } else {
            log(TAG, "Couldn't delete, workout not found ID: ${service.stateHolder.workoutId}")
        }
    }

}