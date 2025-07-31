/*
 * Copyright (c) 2021 Jannis Scheibe <jannis@tadris.de>
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

package de.tadris.fitness.ui.workout.diagram;

import android.content.Context;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.data.BaseSample;
import de.tadris.fitness.data.RouteSample;
import de.tadris.fitness.data.Workout;

public class SpeedConverter extends AbstractSampleConverter {

    public SpeedConverter(Context context) {
        super(context);
    }

    @Override
    public void onCreate(BaseWorkoutData data) {

    }

    @Override
    public float getValue(BaseSample sample) {
        return (float) distanceUnitUtils.getDistanceUnitSystem().getSpeedFromMeterPerSecond(((RouteSample) sample).speed);
    }

    @Override
    public String getName() {
        return getString(R.string.workoutSpeed);
    }


    @Override
    public String getUnit() {
        return distanceUnitUtils.getDistanceUnitSystem().getSpeedUnit();
    }

    @Override
    public boolean isIntervalSetVisible() {
        return true;
    }

    @Override
    public int getColor() {
        return R.color.diagramSpeed;
    }

    @Override
    public float getMinValue(Workout workout) {
        Stream<RouteSample> samples = Arrays.stream(Instance.getInstance(context).db.workoutDao().getAllSamplesOfWorkout(workout.id));
        return (float) distanceUnitUtils.getDistanceUnitSystem().getSpeedFromMeterPerSecond(samples.min(speedComparator).get().speed);
    }

    @Override
    public float getMaxValue(Workout workout) {
        List<RouteSample> samples = Arrays.stream(Instance.getInstance(context).db.workoutDao().getAllSamplesOfWorkout(workout.id)).collect(Collectors.toList());
        return (float) distanceUnitUtils.getDistanceUnitSystem().getSpeedFromMeterPerSecond(samples.stream().max(speedComparator).get().speed);
    }

    public static Comparator<RouteSample> speedComparator = (first, second) -> {
        if (first.speed > second.speed) {
            return 1;
        } else if (first.speed < second.speed) {
            return -1;
        } else {
            return 0;
        }
    };
}
