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

package de.tadris.fitness.recording.autostart;

import androidx.annotation.NonNull;

import de.tadris.fitness.Instance;
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent;

/**
 * This class provides short countdown time announcements, i.e. only the amount of seconds left will
 * be spoken.
 *
 * @apiNote Use this announcement type when the time between announcements is short (e.g. <2 seconds).<p>
 *     Only the plain number will be spoken, nothing else.
 */
public class ShortSecondCountdownTimeAnnouncement extends CountdownTimeAnnouncement {

    public ShortSecondCountdownTimeAnnouncement(Instance instance, int countdownS) {
        super(instance, countdownS);
    }

    @Override
    public String getSpokenText(@NonNull AggregatedRecordingValuesEvent values) {
        return String.valueOf(getCountdownS());
    }
}
