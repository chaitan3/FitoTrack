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

import androidx.annotation.StringRes;

import de.tadris.fitness.R;
import de.tadris.fitness.data.Workout;
import de.tadris.fitness.stats.aggregation.AggregationType;

public class Distance extends AbstractWorkoutInformation {
    public Distance(Context context) {
        super(context);
    }

    @Override
    @StringRes
    public int getTitleRes() {
        return R.string.workoutDistance;
    }

    @Override
    public String getUnit() {
        return distanceUnitUtils.getDistanceUnitSystem().getLongDistanceUnit();
    }

    @Override
    public String getId() {
        return "distance";
    }

    @Override
    public double getValueFromWorkout(Workout workout) {
        return distanceUnitUtils.getDistanceUnitSystem().getDistanceFromKilometers(workout.getDistance() / 1000d);
    }

    @Override
    public AggregationType getAggregationType() {
        return AggregationType.SUM;
    }
}
