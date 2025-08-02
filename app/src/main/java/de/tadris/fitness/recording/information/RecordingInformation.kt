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
import android.preference.PreferenceManager
import androidx.annotation.StringRes
import de.tadris.fitness.Instance
import de.tadris.fitness.recording.announcement.Announcement
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent

abstract class RecordingInformation(protected val context: Context) : Announcement {

    override fun isAnnouncementEnabled() = PreferenceManager.getDefaultSharedPreferences(context)
        .getBoolean("announcement_$id", isEnabledByDefault)

    protected fun getString(@StringRes resId: Int) = context.getString(resId)

    protected val distanceUnitUtils = Instance.getInstance(context).distanceUnitUtils!!
    protected val energyUnitUtils = Instance.getInstance(context).energyUnitUtils!!

    abstract val id: String

    abstract val isEnabledByDefault: Boolean

    abstract fun canBeDisplayed(): Boolean

    abstract fun getTitle(): String

    abstract fun getDisplayedText(values: AggregatedRecordingValuesEvent): String
}