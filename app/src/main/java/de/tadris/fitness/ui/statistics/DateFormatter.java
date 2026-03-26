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

package de.tadris.fitness.ui.statistics;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.tadris.fitness.aggregation.AggregationSpan;

/**
 * Converts an aggregation span to a string.
 */
public class DateFormatter {

    private AggregationSpan aggregationSpan;

    public DateFormatter(@NotNull AggregationSpan aggregationSpan) {
        setAggregationSpan(aggregationSpan);
    }

    public void setAggregationSpan(AggregationSpan aggregationSpan) {
        this.aggregationSpan = aggregationSpan;
    }

    public String format(Calendar calendar) {
        String formatString;
        SimpleDateFormat format;
        switch (aggregationSpan) {
            case DAY:
                format = new SimpleDateFormat("dd. MMM yyyy");
                formatString = format.format(calendar.getTime());
                break;
            case WEEK:
                format = new SimpleDateFormat("yyyy 'W'w");
                formatString = format.format(calendar.getTime());
                break;
            case MONTH:
                format = new SimpleDateFormat("MMM yyyy");
                formatString = format.format(calendar.getTime());
                break;
            case YEAR:
                format = new SimpleDateFormat("yyyy");
                formatString = format.format(calendar.getTime());
                break;
            default:
                formatString = "-";
        }
        return formatString;
    }
}
