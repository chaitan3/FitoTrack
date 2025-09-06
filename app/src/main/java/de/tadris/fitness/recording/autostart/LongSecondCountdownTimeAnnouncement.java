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
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent;

/**
 * This class provides long countdown time announcements, i.e. the amount of seconds left plus some
 * more words will be spoken.
 *
 * @apiNote Use this announcement type when there are a few seconds between announcements (e.g. ~5s).<p>
 *     Do NOT use it for one second, however, as the plural form of seconds is used and the
 *     announcement might thus be grammatically incorrect depending on the language.
 */
public class LongSecondCountdownTimeAnnouncement extends CountdownTimeAnnouncement {
    private final Context context;

    public LongSecondCountdownTimeAnnouncement(Context context, Instance instance, int countdownS) {
        super(instance, countdownS);
        this.context = context;
    }

    @Override
    public String getSpokenText(@NonNull AggregatedRecordingValuesEvent values) {
        int seconds = getCountdownS();
        String secs = context.getResources().getQuantityString(R.plurals.seconds, seconds, seconds);
        return context.getString(R.string.ttsLongSecondCountdownAnnouncement, secs);
    }
}
