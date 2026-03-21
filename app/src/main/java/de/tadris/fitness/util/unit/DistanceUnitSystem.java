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

package de.tadris.fitness.util.unit;

import androidx.annotation.StringRes;

public interface DistanceUnitSystem {

    double getMetersFromShortDistance(double shortdistance);

    default double getMetersFromLongDistance(double longdistance) {
        return getMetersFromShortDistance(getShortDistanceFromLong(longdistance));
    }

    default double getElevationFromMeters(double meters) {
        return getDistanceFromMeters(meters);
    }

    double getShortDistanceFromLong(double longdistance);

    double getDistanceFromMeters(double meters);

    double getDistanceFromKilometers(double kilometers);

    double getWeightFromKilogram(double kilogram);

    double getKilogramFromUnit(double unit);

    double getSpeedFromMeterPerSecond(double meterPerSecond);

    double getMeterPerSecondFromSpeed(double speed);

    String getLongDistanceUnit();

    @StringRes
    int getLongDistanceUnitTitle(boolean isPlural);

    String getShortDistanceUnit();

    @StringRes
    int getShortDistanceUnitTitle(boolean isPlural);

    String getReallyShortDistanceUnit();

    double getDistanceFromCentimeters(double centimeters);

    double getCentimetersFromReallyShortDistance(double reallyShortDistance);

    default String getElevationUnit() {
        return getShortDistanceUnit();
    }

    @StringRes
    default int getElevationUnitTitle(boolean isPlural) {
        return getShortDistanceUnitTitle(isPlural);
    }

    String getWeightUnit();

    String getSpeedUnit();

    @StringRes
    int getSpeedUnitTitle();

}
