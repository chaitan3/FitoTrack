/*
 * Copyright (c) 2025 Bart Vullings <dev@windkracht8.com>
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

package de.tadris.fitness.util;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Util{
    private static final SimpleDateFormat sdfDuration = new SimpleDateFormat("mm:ss", Locale.GERMANY);
    private static final SimpleDateFormat sdfDurationShort = new SimpleDateFormat("m:ss", Locale.GERMANY);
    private static final SimpleDateFormat sdfDurationHour = new SimpleDateFormat("H:mm:ss", Locale.GERMANY);

    public static String prettyDuration(long value){
        if(value >= 3600000){
            return sdfDurationHour.format(value-3600000);
        }else{
            return sdfDuration.format(value);
        }
    }

    public static String prettyDurationShort(long value){
        if(value >= 3600000){
            return sdfDurationHour.format(value-3600000);
        }else{
            return sdfDurationShort.format(value);
        }
    }

    public static String prettySize(long bytes){
        long GB = 1073741824;
        long MB = 1048576;
        long KB = 1024;
        if (bytes > GB * 3) {
            double gbs = (double) bytes/GB;
            return String.format(Locale.getDefault(), "%.1f GB", gbs);
        }else if(bytes > GB){
            double gbs = (double) bytes/GB;
            return String.format(Locale.getDefault(), "%.2f GB", gbs);
        }else if(bytes > MB){
            double mbs = (double) bytes/MB;
            return String.format(Locale.getDefault(), "%.0f MB", mbs);
        }else if(bytes > KB){
            double kbs = (double) bytes/KB;
            return String.format(Locale.getDefault(), "%.0f KB", kbs);
        }
        return bytes + " B";
    }

    public static String distanceDoubleToString(double value){
        return doubleToString(value, value >= 10 ? 1 : 2);
    }

    public static String speedDoubleToString(double value){
        return doubleToString(value, value >= 20 ? 0 : 1);
    }

    private static String doubleToString(Double doubleValue, int scale){
        Double roundedValue = BigDecimal.valueOf(doubleValue)
                .setScale(scale, RoundingMode.DOWN)
                .doubleValue();
        if(scale == 0) return String.valueOf(roundedValue.intValue());
        String format = "%." + scale + "f";
        return String.format(Locale.ENGLISH, format, roundedValue);
    }

    public static final ValueFormatter timeAxisValueFormatter = new ValueFormatter(){
        @Override
        public String getFormattedValue(float value){return prettyDurationShort((long)value);}
    };
}
