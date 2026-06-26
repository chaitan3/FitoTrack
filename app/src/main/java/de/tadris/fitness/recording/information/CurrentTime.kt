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
package de.tadris.fitness.recording.information

import android.content.Context
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CurrentTime(context: Context) : RecordingInformation(context) {

    override val id = "currentTime"
    override val isEnabledByDefault = false
    override fun canBeDisplayed() = true

    override fun getTitle(): String {
        return getString(R.string.currentTime)
    }

    override fun getDisplayedText(values: AggregatedRecordingValuesEvent): String {
        return Instance.getInstance(context).userDateTimeUtils.formatTime(Date())
    }

    override fun getSpokenText(values: AggregatedRecordingValuesEvent): String {
        return getString(R.string.currentTime) + ": " + getSpokenTime(Calendar.getInstance(Locale.getDefault())) + "."
    }

    private fun getSpokenTime(currentTime: Calendar): String {
        val hours = currentTime[Calendar.HOUR_OF_DAY].toLong()
        val minutes = currentTime[Calendar.MINUTE].toLong()
        return context.getString(
            R.string.currentTimeStructure,
            hours.toString(),
            minutes.toString()
        )
    }
}