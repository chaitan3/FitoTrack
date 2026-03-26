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

import de.tadris.fitness.R;


/**
 * This is the imperial system with meters as the elevation unit
 */
public class ImperialWithMeters extends Imperial {

    @Override
    public double getElevationFromMeters(double meters) {
        return meters;
    }

    @Override
    public String getElevationUnit() {
        return "m";
    }

    @Override
    public int getElevationUnitTitle(boolean isPlural) {
        return isPlural ? R.string.unitMetersPlural : R.string.unitMetersSingular;
    }
}
