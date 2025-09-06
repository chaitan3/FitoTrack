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

import de.tadris.fitness.R;
import de.tadris.fitness.data.BaseSample;
import de.tadris.fitness.data.Workout;
import de.tadris.fitness.util.unit.UnitUtils;

public class FrequencyConverter extends AbstractSampleConverter {

    public FrequencyConverter(Context context) {
        super(context);
    }

    @Override
    public void onCreate(BaseWorkoutData data) {
    }

    @Override
    public float getMinValue(Workout workout) {
        return 0;
    }

    @Override
    public float getMaxValue(Workout workout) {
        return (float) ((IndoorWorkout) workout).maxFrequency;
    }

    @Override
    public float getValue(BaseSample sample) {
        return (float) ((IndoorSample) sample).frequency;
    }

    @Override
    public String getName() {
        return getString(R.string.workoutFrequency);
    }

    @Override
    public int getColor() {
        return R.color.diagramFrequency;
    }

    @Override
    public String getUnit() {
        return UnitUtils.unitHertzShort;
    }
}
