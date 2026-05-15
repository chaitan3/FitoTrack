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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * Predefine some date parser's rules, and support to customize new rules.
 *
 * @author sulin
 * @since 2019-09-12 14:34:10
 */
public final class DateParserBuilder {

    static final String[] months = {
            "january",
            "february",
            "march",
            "april",
            "may",
            "june",
            "july",
            "august",
            "september",
            "october",
            "november",
            "december",
            "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec",
    };
    static final String[] weeks = {
            "monday",
            "tuesday",
            "wednesday",
            "thursday",
            "friday",
            "saturday",
            "sunday",
            "mon", "tue", "wed", "thu", "fri", "sat", "sun",
    };

    static final List<String> STANDARD_RULES = new ArrayList<>();

    static final List<String> CUSTOMIZED_RULES = new ArrayList<>();
    static final Map<String, RuleHandler> CUSTOMIZED_RULE_MAP = new HashMap<>();

    static {
        for (String week : weeks) {
            register(String.format("(?<week>%s)\\W*", week));
        }

        for (String month : months) {
            register(String.format("(?<month>%s)\\W+(?<day>\\d{1,2})(?:th)?\\W+(?<year>\\d{4})\\b", month));
            register(String.format("(?<month>%s)\\W+(?<day>\\d{1,2})(?:th)?\\W+(?<year>\\d{2})$", month));
            register(String.format("(?<month>%s)\\W+(?<day>\\d{1,2})(?:th)?\\W+(?<year>\\d{2})[^:\\d]", month));
            register(String.format("(?<month>%s)\\W+(?<day>\\d{1,2})(?:th)?\\b", month));

            register(String.format("(?<day>\\d{1,2})(?:th)?\\W+(?<month>%s)\\W+(?<year>\\d{4})\\b", month));
            register(String.format("(?<day>\\d{1,2})(?:th)?\\W+(?<month>%s)\\W+(?<year>\\d{2})$", month));
            register(String.format("(?<day>\\d{1,2})(?:th)?\\W+(?<month>%s)\\W+(?<year>\\d{2})[^:\\d]", month));
            register(String.format("(?<day>\\d{1,2})(?:th)?\\W+(?<month>%s)\\W*", month));

            register(String.format("(?<year>\\d{4})\\W+(?<month>%s)\\W+(?<day>\\d{1,2})(?:th)?\\W*", month));
        }

        // yyyy-MM-dd, yyyy/MM/dd...
        register("(?<year>\\d{4})\\W{1}(?<month>\\d{1,2})\\W{1}(?<day>\\d{1,2})[^\\d]?");

        // yyyy-MM, yyyy/MM...
        register("^(?<year>\\d{4})\\W{1}(?<month>\\d{1,2})$");

        // MM/dd/yyyy, dd/MM/yyyy
        register("(?<dayOrMonth>\\d{1,2}\\W{1}\\d{1,2})\\W{1}(?<year>\\d{4})[^\\d]?");

        // dd/MM/yy, MM/dd/yy
        register("(?<dayOrMonth>\\d{1,2}[./]\\d{1,2})[./](?<year>\\d{2})$");
        register("(?<dayOrMonth>\\d{1,2}[./]\\d{1,2})[./](?<year>\\d{2})[^:\\d]");

        // yyyy
        register(" ?(?<year>\\d{4})$");
        // yyyyMM
        register("^(?<year>\\d{4})(?<month>\\d{2})$");
        // yyyyMMdd
        register("^(?<year>\\d{4})(?<month>\\d{2})(?<day>\\d{2})$");
        // yyyyMMddhhmmss
        register("^(?<year>\\d{4})(?<month>\\d{2})(?<day>\\d{2})(?<hour>\\d{2})(?<minute>\\d{2})(?<second>\\d{2})$");

        // unixtime(10)
        register("^(?<unixsecond>\\d{10})$");

        // millisecond(13)
        register("^(?<millisecond>\\d{11,13})$");

        // microsecond(16)
        register("^(?<microsecond>\\d{16})$");

        // nanosecond(19)
        register("^(?<nanosecond>\\d{19})$");

        // at hh:mm:ss.SSSSZ
        register("\\W*(?:at )?(?<hour>\\d{1,2}):(?<minute>\\d{1,2})(?::(?<second>\\d{1,2}))?(?:[.,](?<ns>\\d{1,9}))?(?<zero>z)?");

        // +08:00
        register(" ?(?<zoneOffset>[-+]\\d{1,2}:?(?:\\d{2})?)");

        // 12 o'clock
        register(" ?(?<hour>\\d{1,2}) o'clock\\W*");

        // am, pm
        register(" ?(?<m>am|pm)\\W*");

        // (CEST) (GMT Daylight Time)
        register(" [(](?<zoneName>.+)[)]");

        // support all languages' default TimeZone
        for (String zoneId : TimeZone.getAvailableIDs()) {
            final TimeZone zone = TimeZone.getTimeZone(zoneId);
            final RuleHandler handler = (cs, matcher, dt) -> dt.zone = zone;

            String zoneIdStr = zone.getID().toLowerCase();
            register(String.format(" ?\\Q%s\\E", zoneIdStr), handler);
            register(String.format(" ?\\Q[%s]\\E", zoneIdStr), handler);
        }

        // support others non-standard 'timezone'
        register(" ?pdt", (cs, matcher, dt) -> dt.zone = TimeZone.getTimeZone("PST"));
        register(" ?cest", (cs, matcher, dt) -> dt.zone = TimeZone.getTimeZone("CET"));

        // MSK m=+0.000000001
        register(" msk m=[+-]\\d\\.\\d+");
    }

    static synchronized void register(String re) {
        if (!STANDARD_RULES.contains(re)) {
            STANDARD_RULES.add(re);
        }
    }

    static synchronized void register(String re, RuleHandler handler) {
        if (!CUSTOMIZED_RULE_MAP.containsKey(re)) {
            CUSTOMIZED_RULES.add(re);
        }
        CUSTOMIZED_RULE_MAP.put(re, handler);
    }

    private boolean preferMonthFirst = false;
    private final List<String> rules = new ArrayList<>();
    private final Set<String> standardRules = new HashSet<>();
    private final Map<String, RuleHandler> customizedRuleMap = new HashMap<>();

    DateParserBuilder() {
        this.rules.addAll(DateParserBuilder.STANDARD_RULES);
        this.standardRules.addAll(DateParserBuilder.STANDARD_RULES);
        this.rules.addAll(DateParserBuilder.CUSTOMIZED_RULES);
        this.customizedRuleMap.putAll(DateParserBuilder.CUSTOMIZED_RULE_MAP);
    }

    public DateParserBuilder preferMonthFirst(boolean preferMonthFirst) {
        this.preferMonthFirst = preferMonthFirst;
        return this;
    }

    public DateParserBuilder addRule(String rule) {
        if (!standardRules.contains(rule)) {
            rules.add(rule);
            standardRules.add(rule);
        }
        return this;
    }

    public DateParserBuilder addRule(String rule, RuleHandler handler) {
        if (!customizedRuleMap.containsKey(rule)) {
            rules.add(rule);
        }
        customizedRuleMap.put(rule, handler);
        return this;
    }

    public DateParser build() {
        return new DateParser(rules, standardRules, customizedRuleMap, preferMonthFirst);
    }

}
