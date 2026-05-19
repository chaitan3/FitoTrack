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

package de.tadris.fitness.stats.aggregation.information;

import android.content.Context;

import de.tadris.fitness.Instance;
import de.tadris.fitness.data.Workout;
import de.tadris.fitness.stats.aggregation.WorkoutInformation;
import de.tadris.fitness.util.unit.DistanceUnitUtils;
import de.tadris.fitness.util.unit.EnergyUnitUtils;

public abstract class AbstractWorkoutInformation implements WorkoutInformation {

    protected Context context;
    protected DistanceUnitUtils distanceUnitUtils;
    protected EnergyUnitUtils energyUnitUtils;

    AbstractWorkoutInformation(Context context) {
        this.context = context;
        this.distanceUnitUtils = Instance.getInstance(context).distanceUnitUtils;
        this.energyUnitUtils = Instance.getInstance(context).energyUnitUtils;
    }

    @Override
    public boolean isInformationAvailableFor(Workout workout) {
        return true;
    }

}
