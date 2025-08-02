/*
 * Copyright (c) 2022 Jannis Scheibe <jannis@tadris.de>
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
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent

class CurrentHRBatteryLevel(context: Context) : RecordingInformation(context) {

    override val id = "current_hr_battery_level"
    override val isEnabledByDefault = false
    override fun canBeDisplayed() = true

    override fun getTitle(): String {
        return getString(R.string.heartRateSensorBattery)
    }

    override fun getDisplayedText(values: AggregatedRecordingValuesEvent): String {
        return if (isHRBatteryAvailable(recorder)) {
            recorder.currentHRBatteryLevel.toString() + getString(R.string.unitHeartRateSensorBattery)
        } else {
            "-" // No heart rate data available
        }
    }

    override fun getSpokenText(values: AggregatedRecordingValuesEvent): String {
        return if (isHRBatteryAvailable(recorder)) {
            getTitle() + ": " + getDisplayedText(recorder)
        } else {
            ""
        }
    }

    private fun isHRBatteryAvailable(values: AggregatedRecordingValuesEvent): Boolean {
        return recorder.currentHRBatteryLevel >= 0
    }
}