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

import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import de.tadris.fitness.R;
import de.tadris.fitness.data.BaseSample;
import de.tadris.fitness.data.RouteSample;
import de.tadris.fitness.data.Workout;

public class HeightConverter extends AbstractSampleConverter {

    public HeightConverter(Context context) {
        super(context);
    }

    @Override
    public void onCreate(BaseWorkoutData data) {
    }

    @Override
    public float getValue(BaseSample sample) {
        return (float) distanceUnitUtils.getDistanceUnitSystem().getElevationFromMeters(((RouteSample) sample).elevationMSL);
    }

    @Override
    public String getName() {
        return getString(R.string.height);
    }

    @Override
    public ValueFormatter getYValueFormatter() {
            return new DefaultValueFormatter(0);
    }

    @Override
    public String getUnit() {
        return distanceUnitUtils.getDistanceUnitSystem().getElevationUnit();
    }

    @Override
    public int getColor() {
        return R.color.diagramHeight;
    }

    @Override
    public float getMinValue(Workout workout) {
        return (float) distanceUnitUtils.getDistanceUnitSystem().getElevationFromMeters(((GpsWorkout) workout).minElevationMSL);
    }

    @Override
    public float getMaxValue(Workout workout) {
        return (float) distanceUnitUtils.getDistanceUnitSystem().getElevationFromMeters(((GpsWorkout) workout).maxElevationMSL);
    }
}
