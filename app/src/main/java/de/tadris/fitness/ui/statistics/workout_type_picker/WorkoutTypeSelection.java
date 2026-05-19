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

package de.tadris.fitness.ui.statistics.workout_type_picker;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import de.tadris.fitness.R;
import de.tadris.fitness.data.RecordingType;
import de.tadris.fitness.data.WorkoutType;
import de.tadris.fitness.data.WorkoutTypeManager;
import de.tadris.fitness.stats.aggregation.WorkoutTypeFilter;
import de.tadris.fitness.ui.FitoTrackActivity;
import de.tadris.fitness.util.Icon;

public class WorkoutTypeSelection extends LinearLayout {

    private List<WorkoutType> selectedWorkoutTypes;
    private ArrayList<SelectWorkoutTypeDialog.WorkoutTypeSelectListener> listeners;

    private WorkoutType typeAll;

    public WorkoutTypeSelection(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.view_workout_type_selection, this);

        listeners = new ArrayList<>();

        // selectListener forwards the selected entry to setSelectedWorkoutType
        SelectWorkoutTypeDialog.WorkoutTypeSelectListener selectListener = workoutType -> setSelectedWorkoutTypes(createWorkoutTypeList(context, workoutType));
        // Setup onClickListener
        OnClickListener clickListener = view -> new SelectWorkoutTypeDialogAll((FitoTrackActivity) getContext(), selectListener).show();
        this.setOnClickListener(clickListener);

        // The init
        typeAll = new WorkoutType(WorkoutTypeFilter.ID_ALL,
                getContext().getString(R.string.workoutTypeAll), 0,
                Color.WHITE, "list", 0, RecordingType.GPS.id);
        setSelectedWorkoutTypes(createWorkoutTypeList(context,typeAll));
    }

    /**
     * Returns the selected workout type as a list. Normally the list contains only one item.
     * In case "all" is selected, the list contains every workout type.
     * @return selected workout type as a list.
     */
    public List<WorkoutType> getSelectedWorkoutTypes() {
        return selectedWorkoutTypes;
    }

    public void setSelectedWorkoutTypes(List<WorkoutType> types) {
        this.selectedWorkoutTypes = types;
        ImageView imageView = findViewById(R.id.view_workout_type_selection_image);
        TextView textView = findViewById(R.id.view_workout_type_selection_text);

        WorkoutType type;
        if(types != null && types.size()==1) {
            type = types.get(0);
        }
        else
        {
            type = typeAll;
        }
        imageView.setImageDrawable(ContextCompat.getDrawable(getContext(),
                Icon.getIcon(type.icon)));
        textView.setText(type.title);
        for (SelectWorkoutTypeDialog.WorkoutTypeSelectListener listener : listeners) {
            listener.onSelectWorkoutType(type);
        }
    }

    public void addOnWorkoutTypeSelectListener(SelectWorkoutTypeDialog.WorkoutTypeSelectListener listener) {
        this.listeners.add(listener);
    }

    public void removeOnWorkoutTypeSelectListener(SelectWorkoutTypeDialog.WorkoutTypeSelectListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Convert workout type to list (_all) type should be converted to a list with all Workout types
     * @param workoutType
     * @return list of workout types
     */
    public static ArrayList<WorkoutType> createWorkoutTypeList(Context ctx, WorkoutType workoutType) {
        ArrayList<WorkoutType> workoutTypes = new ArrayList<>();

        if (workoutType.id.equals(WorkoutTypeFilter.ID_ALL)) {
            workoutTypes = (ArrayList<WorkoutType>) WorkoutTypeManager.getInstance().getAllTypes(ctx);
        } else {
            workoutTypes.add(workoutType);
        }
        return workoutTypes;
    }
}
