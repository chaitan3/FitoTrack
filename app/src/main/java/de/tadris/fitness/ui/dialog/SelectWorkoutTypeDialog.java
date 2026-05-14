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

package de.tadris.fitness.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.tadris.fitness.data.WorkoutType;
import de.tadris.fitness.data.WorkoutTypeManager;
import de.tadris.fitness.ui.adapter.WorkoutTypeAdapter;

public class SelectWorkoutTypeDialog implements WorkoutTypeAdapter.WorkoutTypeAdapterListener {

    private Activity context;
    private WorkoutTypeSelectListener listener;
    protected List<WorkoutType> options;
    private Dialog dialog;

    public SelectWorkoutTypeDialog(Activity context, WorkoutTypeSelectListener listener) {
        this.context = context;
        this.listener = listener;
        this.options = WorkoutTypeManager.getInstance().getAllTypesSorted(context);
    }

    public void show() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);

        RecyclerView recyclerView = new RecyclerView(context);
        WorkoutTypeAdapter adapter = new WorkoutTypeAdapter(options, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);

        builderSingle.setView(recyclerView);
        dialog = builderSingle.create();
        dialog.show();
    }

    @Override
    public void onItemSelect(int pos, WorkoutType type) {
        dialog.dismiss();
        listener.onSelectWorkoutType(type);
    }

    public interface WorkoutTypeSelectListener {
        void onSelectWorkoutType(WorkoutType workoutType);
    }
}
