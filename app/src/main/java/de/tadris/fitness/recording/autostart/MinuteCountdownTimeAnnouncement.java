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

import android.content.Context;

import androidx.annotation.NonNull;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.recording.WorkoutRecorder;

/**
 * This class provides countdown time announcements for even minutes, i.e. the amount of minutes
 * left plus some more words will be spoken.
 *
 * @apiNote Use this announcement type for even minutes, e.g. 10m0s or 8m0s.<p>
 *     Do NOT use it for 1m0s, however, as the plural form of minutes is used and the announcement
 *     might thus be grammatically incorrect depending on the language.
 * @see MinuteSecondCountdownTimeAnnouncement
 */
public class MinuteCountdownTimeAnnouncement extends CountdownTimeAnnouncement {
    private final Context context;

    public MinuteCountdownTimeAnnouncement(Context context, Instance instance, int countdownS) {
        super(instance, countdownS);
        this.context = context;
    }

    @Override
    public String getSpokenText(@NonNull WorkoutRecorder recorder) {
        int minutes = getCountdownS() / 60;
        String mins = context.getResources().getQuantityString(R.plurals.minutes, minutes, minutes);
        return context.getString(R.string.ttsMinuteCountdownAnnouncement, mins);
    }
}
