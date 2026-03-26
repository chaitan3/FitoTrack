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

package de.tadris.fitness.data.preferences

import android.content.SharedPreferences
import de.tadris.fitness.data.RecordingType

/**
 * Preferences within this class concern the Metrics (Information) shown on the Recording
 * Activities
 */
class RecordingScreenInformationPreferences(private val prefs: SharedPreferences) {

    @Throws(UnknownModeException::class)
    fun getIdOfDisplayedInformation(mode: String, slot: Int) : String {
        val defaultValue = when(mode) {
            RecordingType.INDOOR.id -> this.getDefaultIndoorActivityId(slot)
            RecordingType.GPS.id -> this.getDefaultGpsActivityId(slot)
            else -> throw UnknownModeException()
        }

        val preferenceKey = "information_display_${mode}_${slot}"
        return this.prefs.getString(preferenceKey, defaultValue)!!
    }

    fun setIdOfDisplayedInformation(mode: String, slot: Int, value: String) {
        val preferenceKey = "information_display_${mode}_${slot}"
        this.prefs.edit().putString(preferenceKey, value).apply()
    }

    // TODO: At some point, replace strings with an Enum
    private fun getDefaultIndoorActivityId(slot: Int) : String {
        return when(slot) {
            0 -> "avg_frequency"
            1 -> "energy_burned"
            2 -> "current_intensity"
            3 -> "pause_duration"
            // Should never happen, but even if it would, there is no point in throwing an exception
            // over this
            else -> "pause_duration"
        }
    }

    // TODO: At some point, replace strings with an Enum
    private fun getDefaultGpsActivityId(slot: Int) : String {
        return when(slot) {
            0 -> "distance"
            1 -> "energy_burned"
            2 -> "avgSpeedMotion"
            3 -> "pause_duration"
            // Should never happen, but even if it would, there is no point in throwing an exception
            // over this
            else -> "pause_duration"
        }
    }

    class UnknownModeException : Exception()

}