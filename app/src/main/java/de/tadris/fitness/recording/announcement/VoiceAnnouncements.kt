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
package de.tadris.fitness.recording.announcement

import android.content.Context
import de.tadris.fitness.data.Interval
import de.tadris.fitness.recording.WorkoutRecorder
import de.tadris.fitness.recording.announcement.interval.IntervalAnnouncements

class VoiceAnnouncements(
    context: Context,
    recorder: WorkoutRecorder,
    ttsController: TTSController,
    intervals: List<Interval>
) {

    private val informationAnnouncements =
        InformationAnnouncements(context, recorder, ttsController)
    private val intervalAnnouncements =
        IntervalAnnouncements(context, recorder, ttsController, intervals)

    fun check() {
        intervalAnnouncements.check()
        informationAnnouncements.check()
    }

    fun applyIntervals(intervals: List<Interval?>?) {
        intervalAnnouncements.setIntervals(intervals)
    }

}