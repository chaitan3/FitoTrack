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

package de.tadris.fitness.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import de.tadris.fitness.data.preferences.UserPreferences;

public class UserDateTimeUtils {

    private final UserPreferences preferences;

    public UserDateTimeUtils(UserPreferences preferences) {
        this.preferences = preferences;
    }

    public String formatDateTime(Date date) {
        return formatDate(date) + " " + formatTime(date);
    }

    public String formatDate(Date date) {
        String pattern = preferences.getDateFormatSetting();
        if (pattern.equals("system")) {
            return SimpleDateFormat.getDateInstance().format(date);
        } else {
            return format(date, pattern);
        }
    }

    public String formatTime(Date date) {
        String mode = preferences.getTimeFormatSetting();
        switch (mode) {
            default:
            case "system":
                return SimpleDateFormat.getTimeInstance().format(date);
            case "12":
                return new SimpleDateFormat("h:mm a").format(date).toUpperCase();
            case "24":
                return new SimpleDateFormat("HH:mm").format(date);
        }
    }

    public String format(Date date, String pattern) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(date);
    }

    /**
     * Get a calendar instance possibly overriding the default java locales
     */
    public Calendar getCalendarInstance(){
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(preferences.getFirstDayOfWeek());
        return calendar;
    }
}
