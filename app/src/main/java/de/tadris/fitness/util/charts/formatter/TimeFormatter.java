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

package de.tadris.fitness.util.charts.formatter;

import android.content.Context;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.concurrent.TimeUnit;

import de.tadris.fitness.R;

public class TimeFormatter extends ValueFormatter {
    TimeUnit input;
    boolean dispSecs, dispMins, dispHours;

    public TimeFormatter(TimeUnit input) {
        this(input, true, true, true);
    }

    public TimeFormatter(TimeUnit input, boolean dispSecs, boolean dispMins, boolean dispHours) {
        this.input = input;
        this.dispSecs = dispSecs;
        this.dispMins = dispMins;
        this.dispHours = dispHours;
    }

    /**
     * @param value time in the unit defined by TimeInput of the constructor
     * @return
     */
    @Override
    public String getFormattedValue(float value) {
        long multip = 1000000;
        long s = input.toSeconds((long) (value*multip))/multip; // "Trick" to avoid precision loss
        if(dispSecs)
            return de.tadris.fitness.util.unit.TimeFormatter.formatDuration(s);
        else if (dispHours)
            return de.tadris.fitness.util.unit.TimeFormatter.formatHoursMinutes(s);
        else
            return de.tadris.fitness.util.unit.TimeFormatter.formatMinutesOnly(s);
        // TODO: Implement rest
    }

    public String getUnit(Context ctx)
    {
        if(dispHours)
            return ctx.getString(R.string.timeHourShort);
        else
            return ctx.getString(R.string.timeMinuteShort);
    }
}
