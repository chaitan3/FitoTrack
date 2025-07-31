/*
 * Copyright (c) 2022 Jannis Scheibe <jannis@tadris.de>
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

package de.tadris.fitness.ui.workout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.tadris.fitness.Instance;
import de.tadris.fitness.data.BaseSample;
import de.tadris.fitness.data.StatsDataTypes;
import de.tadris.fitness.data.Workout;

public abstract class IndoorWorkoutActivity extends WorkoutActivity {

    protected IndoorWorkout workout;
    protected List<IndoorSample> samples;

    @Override
    void initBeforeContent() {
        super.initBeforeContent();
        workout = (IndoorWorkout) getWorkout();
        samples = getBaseWorkoutData().castToIndoorData().getSamples();
    }

    @Override
    Workout findWorkout(long id) {
        return Instance.getInstance(this).db.indoorWorkoutDao().getWorkoutById(id);
    }

    @Override
    List<BaseSample> findSamples(long workoutId) {
        return Arrays.asList(Instance.getInstance(this).db.indoorWorkoutDao().getAllSamplesOfWorkout(workoutId));
    }

    protected IndoorWorkoutData getIndoorWorkoutData() {
        return new IndoorWorkoutData(workout, samples);
    }

    @Override
    protected List<BaseSample> aggregatedSamples(int aggregationLength, StatsDataTypes.TimeSpan viewFieldSpan) {
        return new ArrayList<>(samples);
    }

}
