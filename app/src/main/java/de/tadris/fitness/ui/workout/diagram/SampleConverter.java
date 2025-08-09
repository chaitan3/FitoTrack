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

import androidx.annotation.ColorRes;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.List;

import de.tadris.fitness.data.BaseSample;
import de.tadris.fitness.data.Workout;
import de.tadris.fitness.data.WorkoutData;
import kotlin.Pair;

public interface SampleConverter {

    float getMinValue(WorkoutData data);

    float getMaxValue(WorkoutData data);

    List<Pair<BaseSample, Float>> getEntries(WorkoutData data);

    float getValue(BaseSample sample);

    String getName();

    String getXAxisLabel();

    ValueFormatter getXValueFormatter();

    String getYAxisLabel();

    ValueFormatter getYValueFormatter();

    boolean isIntervalSetVisible();

    void afterAdd(CombinedChart chart);

    @ColorRes
    int getColor();

    String getUnit();

}
