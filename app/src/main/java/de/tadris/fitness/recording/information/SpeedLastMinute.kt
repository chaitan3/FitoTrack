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

class SpeedLastMinute(context: Context) : GpsRecordingInformation(context) {

    override val id = "speed_last_minute"
    override val isEnabledByDefault = false
    override fun canBeDisplayed() = true

    override fun getTitle(): String {
        return getString(R.string.speedLastMinute)
    }

    override fun getDisplayedText(recorder: GpsWorkoutRecorder): String {
        return distanceUnitUtils.getSpeed(recorder.getCurrentSpeed(TIME))
    }

    override fun getSpokenText(recorder: GpsWorkoutRecorder): String {
        return getString(R.string.speedLastMinuteSpoken) + ": " + distanceUnitUtils.getSpeed(
            recorder.getCurrentSpeed(
                TIME
            ), true
        ) + "."
    }

    companion object {
        private const val TIME = 1000 * 60 // One minute
    }
}