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

import java.util.ArrayList;
import java.util.List;

import de.tadris.fitness.data.WorkoutData;

public class ConverterManager {

    public List<SampleConverter> availableConverters = new ArrayList<>();
    public List<SampleConverter> selectedConverters = new ArrayList<>();

    public ConverterManager(Context context, WorkoutData data) {
        if (data.getHasRouteData()) {
            availableConverters.add(new SpeedConverter(context));
            availableConverters.add(new HeightConverter(context));
        }
        if (data.getHasHRData()) {
            availableConverters.add(new HeartRateConverter(context));
        }
        if (data.getHasRepetitions()) {
            availableConverters.add(new FrequencyConverter(context));
        }
        if (data.getHasIntensityData()) {
            availableConverters.add(new IntensityConverter(context));
        }
    }


}
