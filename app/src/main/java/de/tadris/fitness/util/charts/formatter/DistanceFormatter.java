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

import com.github.mikephil.charting.formatter.ValueFormatter;

import de.tadris.fitness.util.unit.DistanceUnitUtils;

public class DistanceFormatter extends ValueFormatter {
    DistanceUnitUtils distanceUnitUtils;
    boolean useLongUnit = false;

    public DistanceFormatter(DistanceUnitUtils distanceUnitUtils, int longestDistanceInMeters) {
        this.distanceUnitUtils = distanceUnitUtils;
        useLongUnit = longestDistanceInMeters/distanceUnitUtils.getDistanceUnitSystem().getMetersFromLongDistance(1) > 1 ? true : false;
    }

    @Override
    public String getFormattedValue(float value) {
        return distanceUnitUtils.getDistanceWithoutUnit((int)value, useLongUnit, 1);
    }
}