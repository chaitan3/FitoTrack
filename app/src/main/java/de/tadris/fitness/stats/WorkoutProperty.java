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

package de.tadris.fitness.stats;

import android.content.Context;

import androidx.annotation.StringRes;

import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.ui.charts.ChartStyles;
import de.tadris.fitness.ui.charts.formatter.DistanceFormatter;
import de.tadris.fitness.ui.charts.formatter.FractionedDateFormatter;
import de.tadris.fitness.ui.charts.formatter.TimeFormatter;

public enum WorkoutProperty {
    NUMBER(0, WorkoutPropertyType.BASE, R.string.workoutCount),
    START(1, WorkoutPropertyType.BASE, R.string.workoutStartTime),
    END(2, WorkoutPropertyType.BASE, R.string.workoutEndTime),
    DURATION(3, WorkoutPropertyType.BASE, R.string.workoutDuration),
    PAUSE_DURATION(4, WorkoutPropertyType.BASE, R.string.workoutPauseDuration),
    CALORIE(5, WorkoutPropertyType.BASE, R.string.workoutBurnedEnergy),

    // GPS Workout
    LENGTH(6, WorkoutPropertyType.GPS, R.string.workoutDistance),
    AVG_SPEED(7, WorkoutPropertyType.GPS, R.string.workoutAvgSpeedShort),
    AVG_PACE(8, WorkoutPropertyType.GPS, R.string.workoutPace),

    // Indoor Workout
    AVG_FREQUENCY(9, WorkoutPropertyType.INDOOR, R.string.workoutFrequency),
    REPETITIONS(10, WorkoutPropertyType.INDOOR, R.string.workoutRepetitions);

    private final int id;

    private final WorkoutPropertyType type;

    @StringRes
    private final int titleRes;

    WorkoutProperty(int id, WorkoutPropertyType type, int titleRes) {
        this.id = id;
        this.type = type;
        this.titleRes = titleRes;
    }

    public int getId() {
        return id;
    }

    public WorkoutPropertyType getType() {
        return type;
    }

    public int getTitleRes() {
        return titleRes;
    }

    public static WorkoutProperty getById(int id) {
        for (WorkoutProperty property : values()) {
            if (property.id == id) return property;
        }
        throw new IllegalArgumentException("Cannot find WorkoutProperty with id " + id);
    }

    private static final List<WorkoutProperty> summable = Arrays.asList(NUMBER, DURATION, PAUSE_DURATION, CALORIE, LENGTH, REPETITIONS);

    public boolean summable() {
        return summable.contains(this);
    }

    public static List<String> getStringRepresentations(Context context) {
        List<String> criteria = new ArrayList<>();
        for (WorkoutProperty property : values()) {
            criteria.add(context.getString(property.getTitleRes()));
        }
        return criteria;
    }


    public String getFormattedValue(Context ctx, float value) {
        return getFormattedValue(ctx, value, true);
    }

    public String getFormattedValue(Context ctx, float value, boolean unit) {
        String formatted = getValueFormatter(ctx, value).getFormattedValue(value);
        if (unit)
            formatted += " " + getUnit(ctx, value);
        return formatted;
    }

    public ValueFormatter getValueFormatter(Context ctx, float value) {
        switch (this) {
            case NUMBER:
                return new DefaultValueFormatter(0);
            case START:
            case END:
                return new FractionedDateFormatter(ctx, ChartStyles.statsAggregationSpan((long) value));
            case DURATION:
            case PAUSE_DURATION:
                return StatsProvider.getCorrectTimeFormatter(TimeUnit.MILLISECONDS, (long) value);
            case AVG_PACE:
                return StatsProvider.getCorrectTimeFormatter(TimeUnit.MINUTES, (long) value);

            case AVG_SPEED:
            case LENGTH:
                return new DistanceFormatter(Instance.getInstance(ctx).distanceUnitUtils, (int)value);

            case CALORIE:
            case AVG_FREQUENCY:
            case REPETITIONS:
            default:
                return new DefaultValueFormatter(2);
        }
    }

    public String getUnit(Context ctx, float value) {
        switch (this) {
            case START:
            case END:
                return ctx.getString(((FractionedDateFormatter) getValueFormatter(ctx, value)).getSpan().axisLabel);
            case DURATION:
            case PAUSE_DURATION:
                return ((TimeFormatter) getValueFormatter(ctx, value)).getUnit(ctx);
            case AVG_PACE:
                return Instance.getInstance(ctx).distanceUnitUtils.getPaceUnit();

            case AVG_SPEED:
            case LENGTH:
                if(value / Instance.getInstance(ctx).distanceUnitUtils.getDistanceUnitSystem().getMetersFromLongDistance(1) > 1)
                    return Instance.getInstance(ctx).distanceUnitUtils.getDistanceUnitSystem().getLongDistanceUnit();
                else
                    return Instance.getInstance(ctx).distanceUnitUtils.getDistanceUnitSystem().getShortDistanceUnit();

            case NUMBER:
            case CALORIE:
            case AVG_FREQUENCY:
            case REPETITIONS:
            default:
                return "";
        }
    }
}
