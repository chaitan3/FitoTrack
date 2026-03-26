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
 * Preferences within this class concern the Metrics (Information) shown on the Sharing
 * Activities
 */
class SharingScreenInformationPreferences(private val prefs: SharedPreferences) {

    @Throws(UnknownModeException::class)
    fun getIdOfDisplayedInformation(mode: String, slot: Int): String {
        val defaultValueEnum = when (mode) {
            RecordingType.INDOOR.id -> this.getDefaultIndoorActivityId(slot)
            RecordingType.GPS.id -> this.getDefaultGpsActivityId(slot)
            else -> throw UnknownModeException()
        }

        val preferenceKey = "information_display_share_${mode}_${slot}"
        return this.prefs.getString(preferenceKey, defaultValueEnum)!!
    }

    fun setIdOfDisplayedInformation(mode: String, slot: Int, valueType: String) {
        val preferenceKey = "information_display_share_${mode}_${slot}"
        this.prefs.edit().putString(preferenceKey, valueType).apply()
    }

    private fun getDefaultIndoorActivityId(slot: Int) = when (slot) {
        0 -> "repetitions"
        1 -> "duration"
        2 -> "burned-energy"
        else -> "burned-energy"
    }

    private fun getDefaultGpsActivityId(slot: Int) = when (slot) {
        0 -> "distance"
        1 -> "average-pace"
        2 -> "duration"
        else -> "duration"
    }

    class UnknownModeException : Exception()

}