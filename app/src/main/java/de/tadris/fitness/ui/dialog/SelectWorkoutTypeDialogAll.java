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

package de.tadris.fitness.ui.dialog;

import java.util.Arrays;

import de.tadris.fitness.R;
import de.tadris.fitness.aggregation.WorkoutTypeFilter;
import de.tadris.fitness.data.RecordingType;
import de.tadris.fitness.data.WorkoutType;
import de.tadris.fitness.stats.StatsDataProvider;
import de.tadris.fitness.stats.WorkoutProperty;
import de.tadris.fitness.ui.FitoTrackActivity;

public class SelectWorkoutTypeDialogAll extends SelectWorkoutTypeDialog {

    StatsDataProvider statsDataProvider;

    public SelectWorkoutTypeDialogAll(FitoTrackActivity context, WorkoutTypeSelectListener listener) {
        super(context, listener);
        // this.options.removeIf(type -> type.id==ID_ADD);
        statsDataProvider = new StatsDataProvider(context);
        this.options.removeIf(type -> statsDataProvider.getData(WorkoutProperty.START, Arrays.asList(type)).size() == 0);

        this.options.add(0, new WorkoutType(WorkoutTypeFilter.ID_ALL,
                context.getString(R.string.workoutTypeAll), 0,
                context.getThemePrimaryColor(), "list", 0, RecordingType.GPS.id));
    }

}
