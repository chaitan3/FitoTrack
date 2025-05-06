/*
 * Copyright (c) 2025 Bart Vullings <dev@windkracht8.com>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU General Public License as published by
 * 	the Free Software Foundation, either version 3 of the License, or
 * 	(at your option) any later version.
 *
 * 	FitoTrack is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 *
 * 	You should have received a copy of the GNU General Public License
 * 	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.tadris.fitness.ui.workouts;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.tadris.fitness.R;
import de.tadris.fitness.data.WorkoutData;

public class WorkoutsItem extends LinearLayout{
    WorkoutsItem(Context context, WorkoutData workoutData){
        super(context);
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.workouts_item, this, true);
        ((ImageView)findViewById(R.id.icon)).setImageResource(workoutData.getWorkoutTypeIcon());
        ((TextView)findViewById(R.id.name)).setText(workoutData.name);
        ((TextView)findViewById(R.id.description)).setText(workoutData.description);
    }
}
