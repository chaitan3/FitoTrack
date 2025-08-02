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
package de.tadris.fitness.recording.information

import android.content.Context
import de.tadris.fitness.R

class SystemActions(context: Context) : RecordingInformation(context) {

    private var lastState: WorkoutRecorder.RecordingState =
        WorkoutRecorder.RecordingState.IDLE

    override val id = "system_actions"
    override val isEnabledByDefault = true
    override fun canBeDisplayed() = false
    override fun getSpokenText(recorder: WorkoutRecorder): String {
        var text = ""
        if (lastState != recorder.state) {
            text = speakState(recorder.state)
        }
        lastState = recorder.state
        return text
    }

    private fun speakState(newState: WorkoutRecorder.RecordingState): String {
        if (lastState == WorkoutRecorder.RecordingState.IDLE) {
            return getString(R.string.workoutStarted)
        }
        return when (newState) {
            WorkoutRecorder.RecordingState.RUNNING -> getString(R.string.workoutResumed)
            WorkoutRecorder.RecordingState.PAUSED -> getString(R.string.workoutPaused)
            WorkoutRecorder.RecordingState.STOPPED -> getString(R.string.workoutStopped)
            else -> ""
        }
    }

    override fun getDisplayedText(recorder: WorkoutRecorder): String {
        return ""
    }

    override fun isPlayedAlways(): Boolean {
        return true
    }

    override fun getTitle(): String {
        return ""
    }
}