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
import android.widget.ArrayAdapter;

import java.util.List;

import de.tadris.fitness.R;
import de.tadris.fitness.data.RecordingType;
import de.tadris.fitness.recording.information.InformationManager;
import de.tadris.fitness.recording.information.RecordingInformation;

/**
 * Creates a Dialog which lets the user set a new metric (like Distance, Avg Speed, etc) for a slot.
 */
public class SelectWorkoutInformationDialog {

    private final Activity context;
    private final WorkoutInformationSelectListener listener;
    private final RecordingType mode;
    private final int slot;
    private final List<RecordingInformation> informationList;

    public SelectWorkoutInformationDialog(Activity context, RecordingType mode, int slot, WorkoutInformationSelectListener listener) {
        this.context = context;
        this.listener = listener;
        this.mode = mode;
        this.slot = slot;
        this.informationList = new InformationManager(mode, context).getDisplayableInformation();
    }

    public void show() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, R.layout.select_dialog_singlechoice_material);
        for (RecordingInformation information : informationList) {
            arrayAdapter.add(information.getTitle());
        }

        builderSingle.setAdapter(arrayAdapter, (dialog, which) -> onSelect(which));
        builderSingle.show();
    }

    private void onSelect(int which) {
        RecordingInformation information = informationList.get(which);
        listener.onSelectWorkoutInformation(slot, information);
    }

    public interface WorkoutInformationSelectListener {
        void onSelectWorkoutInformation(int slot, RecordingInformation information);
    }

}
