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
package de.tadris.fitness.recording.autostart

import org.greenrobot.eventbus.EventBus

enum class AutoStartMode { INSTANT, WAIT_FOR_GPS, ON_MOVE }

/** Posted by UI to request auto-start. */
data class AutoStartRequestEvent(val mode: AutoStartMode, val delayMs: Long = 0L)

/** Posted by UI to abort a pending auto-start. */
object AutoStartCancelEvent

/** Posted by the component (sticky) to expose current auto-start state to the UI. */
data class AutoStartStateEvent(val state: State) {

    sealed interface State {
        data object Idle : State
        data class Countdown(val remainingMs: Long, val remainingS: Int) : State
        data object WaitingForGps : State
        data object WaitingForMove : State
    }

    companion object {
        val IDLE = AutoStartStateEvent(State.Idle)

        fun getCurrent(): AutoStartStateEvent =
            EventBus.getDefault().getStickyEvent(AutoStartStateEvent::class.java) ?: IDLE
    }
}

/** Posted by the component when the workout is auto-started, so the UI can show a toast. */
object WorkoutAutoStartedEvent
