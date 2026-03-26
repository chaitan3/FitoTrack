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

package de.tadris.fitness.aggregation;

import android.annotation.SuppressLint;

import androidx.annotation.StringRes;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import de.tadris.fitness.R;

public enum AggregationSpan {

    SINGLE(R.string.singleWorkout, R.string.dayInMonth, TimeUnit.MINUTES.toMillis(1), Calendar.MILLISECOND, "dd/MMM/yy") {
        @Override
        public Calendar setCalendarToAggregationStart(Calendar calendar, int firstDayOfWeek) {
            return calendar;
        }
    },
    DAY(R.string.day, R.string.dayInMonth, TimeUnit.DAYS.toMillis(1), Calendar.DAY_OF_MONTH, "dd/mm/yy"),
    WEEK(R.string.week, R.string.calendarWeekYear, TimeUnit.DAYS.toMillis(7), Calendar.WEEK_OF_YEAR, "ww/yy") {
        @Override
        public Calendar setCalendarToAggregationStart(Calendar calendar, int firstDayOfWeek) {
            super.setCalendarToAggregationStart(calendar, firstDayOfWeek);
            calendar.set(Calendar.DAY_OF_WEEK, firstDayOfWeek);
            return calendar;
        }
    },
    MONTH(R.string.month, R.string.monthYear, TimeUnit.DAYS.toMillis(30), Calendar.MONTH, "MMM/yy") {
        @Override
        public Calendar setCalendarToAggregationStart(Calendar calendar, int firstDayOfWeek) {
            super.setCalendarToAggregationStart(calendar, firstDayOfWeek);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            return calendar;
        }
    },
    YEAR(R.string.year, R.string.year, TimeUnit.DAYS.toMillis(365), Calendar.YEAR, "yyyy") {
        @Override
        public Calendar setCalendarToAggregationStart(Calendar calendar, int firstDayOfWeek) {
            super.setCalendarToAggregationStart(calendar, firstDayOfWeek);
            calendar.set(Calendar.DAY_OF_YEAR, 1);
            return calendar;
        }
    },
    ALL(R.string.workoutTypeAll, R.string.workoutTypeAll, Long.MAX_VALUE, Integer.MAX_VALUE, "yyyy") {
        @Override
        public Calendar setCalendarToAggregationStart(Calendar calendar, int firstDayOfWeek) {
            super.setCalendarToAggregationStart(calendar, firstDayOfWeek);
            calendar.set(Calendar.YEAR, 1);
            calendar.set(Calendar.DAY_OF_YEAR, 1);
            return calendar;
        }

        public Calendar getAggregationEnd(Calendar calendar) {
            Calendar clone = (Calendar) calendar.clone();
            clone.setTimeInMillis(Long.MAX_VALUE);
            return clone;
        }

        public long getAggregationEnd(long start) {
            Calendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(Long.MAX_VALUE);
            return calendar.getTimeInMillis();
        }
    };

    @StringRes
    public final int title;
    @StringRes
    public final int axisLabel;
    public final SimpleDateFormat dateFormat;
    public final long spanInterval;
    public final int calendarField;

    @SuppressLint("SimpleDateFormat")
    AggregationSpan(int title, int axisLabel, long spanInterval, int calendarField, String formatString) {
        this.title = title;
        this.axisLabel = axisLabel;
        this.spanInterval = spanInterval;
        this.calendarField = calendarField;
        this.dateFormat = new SimpleDateFormat(formatString);
    }

    public Calendar setCalendarToAggregationStart(Calendar calendar, int firstDayOfWeek) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public Calendar getAggregationEnd(Calendar calendar) {
        Calendar clone = (Calendar) calendar.clone();
        clone.add(calendarField, 1);
        return clone;
    }

    public long getAggregationEnd(long start) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(start);
        calendar.add(calendarField, 1);
        return calendar.getTimeInMillis();
    }

    public int toInt() {
        switch (this) {
            case SINGLE:
                return 0;
            case DAY:
                return 1;
            case WEEK:
                return 2;
            case MONTH:
                return 3;
            case YEAR:
                return 4;
            case ALL:
            default:
                return 5;

        }
    }

    public static AggregationSpan fromInt(int statsAggregation) {
        switch (statsAggregation) {
            case 0:
                return SINGLE;
            case 1:
                return DAY;
            case 2:
                return WEEK;
            case 3:
                return MONTH;
            case 4:
                return YEAR;
            case 5:
            default:
                return ALL;
        }

    }
}
