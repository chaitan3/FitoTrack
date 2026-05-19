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

package de.tadris.fitness.ui.charts.formatter;

import android.content.Context;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.tadris.fitness.stats.aggregation.AggregationSpan;

public class FractionedDateFormatter extends ValueFormatter {
    Context ctx;
    SimpleDateFormat format;
    AggregationSpan span;

    public FractionedDateFormatter(Context ctx, AggregationSpan span)
    {
        setAggregationSpan(span);
        this.span = span;
        this.ctx = ctx;
    }

    public void setAggregationSpan(AggregationSpan span)
    {
        format = span.dateFormat;
    }
    public AggregationSpan getSpan() {return span;}

    @Override
    public String getFormattedValue(float value) {
        return format.format(new Date((long) (value + span.spanInterval/2)));
    }
}
