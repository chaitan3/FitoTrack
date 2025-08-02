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
import androidx.preference.PreferenceManager
import de.tadris.fitness.R
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent

class Duration(context: Context) : RecordingInformation(context) {

    override val id = "duration"
    override val isEnabledByDefault = true
    override fun canBeDisplayed() = false

    override fun getSpokenText(values: AggregatedRecordingValuesEvent): String {
        return getString(R.string.workoutDuration) + ": " + getSpokenTime(values.activeDuration) + "."
    }

    private fun getSpokenTime(duration: Long): String {
        var duration = duration
        val minute = 1000L * 60
        val hour = minute * 60
        val spokenTime = StringBuilder()
        if (duration > hour) {
            val hours = duration / hour
            duration %= hour // Set duration to the rest
            spokenTime.append(hours).append(" ")
            spokenTime.append(getString(if (hours == 1L) R.string.timeHourSingular else R.string.timeHourPlural))
                .append(" ")
                .append(getString(R.string.and)).append(" ")
        }
        val minutes = duration / minute
        spokenTime.append(minutes).append(" ")
        spokenTime.append(getString(if (minutes == 1L) R.string.timeMinuteSingular else R.string.timeMinutePlural))

        if(durationWithSeconds()) {
            val seconds = (duration % minute) / 1000L
            spokenTime.append(seconds).append(" ")
            spokenTime.append(getString(if (seconds == 1L) R.string.timeSecondsSingular else R.string.timeSecondsPlural))
        }
        return spokenTime.toString()
    }

    override fun getTitle(): String {
        return ""
    }

    override fun getDisplayedText(values: AggregatedRecordingValuesEvent): String {
        return ""
    }

    private fun durationWithSeconds() = PreferenceManager.getDefaultSharedPreferences(context)
        .getBoolean("announcement_duration_with_seconds", false)
}