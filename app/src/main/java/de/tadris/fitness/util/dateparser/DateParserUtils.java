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

// Copied from https://github.com/sisyphsu/dateparser (MIT License)
package de.tadris.fitness.util.dateparser;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.Date;

/**
 * Convenience utils for {@link DateParser}.
 *
 * @author sulin
 * @since 2019-09-15 11:19:31
 */
public final class DateParserUtils {

    private DateParserUtils() {
    }

    private static final DateParserBuilder builder = DateParser.newBuilder();
    private static DateParser dateParser = builder.build();

    public static synchronized Date parseDate(String str) {
        return dateParser.parseDate(str);
    }

    public static synchronized Calendar parseCalendar(String str) {
        return dateParser.parseCalendar(str);
    }

    public static synchronized LocalDateTime parseDateTime(String str) {
        return dateParser.parseDateTime(str);
    }

    public static synchronized OffsetDateTime parseOffsetDateTime(String str) {
        return dateParser.parseOffsetDateTime(str);
    }

    public static synchronized void preferMonthFirst(boolean preferMonthFirst) {
        dateParser.setPreferMonthFirst(preferMonthFirst);
    }

    public static synchronized void registerStandardRule(String re) {
        builder.addRule(re);
        dateParser = builder.build();
    }

    public static synchronized void registerCustomizedRule(String re, RuleHandler handler) {
        builder.addRule(re, handler);
        dateParser = builder.build();
    }

}
