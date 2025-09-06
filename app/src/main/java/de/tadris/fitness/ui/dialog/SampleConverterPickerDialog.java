/*
 * Copyright (c) 2020 Jannis Scheibe <jannis@tadris.de>
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
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;

import de.tadris.fitness.R;
import de.tadris.fitness.ui.workout.diagram.ConverterManager;
import de.tadris.fitness.ui.workout.diagram.SampleConverter;

public class SampleConverterPickerDialog {

    private static final int MAX_SELECTED = 2;
    private static final int MIN_SELECTED = 1;

    private final Activity context;
    private final SampleConverterSelectListener listener;
    private final ConverterManager manager;
    private Dialog dialog;

    public SampleConverterPickerDialog(Activity context, SampleConverterSelectListener listener, ConverterManager manager) {
        this.context = context;
        this.listener = listener;
        this.manager = manager;
    }

    public void show() {
        final AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        for (final SampleConverter converter : manager.availableConverters) {
            CheckBox checkBox = getCheckBox(converter);
            linearLayout.addView(checkBox);
        }

        ScrollView scrollView = new ScrollView(context);

        scrollView.addView(linearLayout);
        builderSingle.setView(scrollView);

        builderSingle.setCancelable(true);

        builderSingle.setPositiveButton(R.string.okay, null);
        builderSingle.setOnDismissListener(dialog -> listener.onDialogClose());

        dialog = builderSingle.create();

        dialog.show();
    }

    @NonNull
    private CheckBox getCheckBox(SampleConverter converter) {
        CheckBox checkBox = new CheckBox(context);
        checkBox.setText(converter.getName());
        checkBox.setChecked(manager.selectedConverters.contains(converter));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (manager.selectedConverters.size() >= MAX_SELECTED) {
                    checkBox.setChecked(false);
                } else {
                    manager.selectedConverters.add(converter);
                }
            } else {
                if (manager.selectedConverters.size() <= MIN_SELECTED) {
                    checkBox.setChecked(true);
                } else {
                    manager.selectedConverters.remove(converter);
                }
            }
        });
        return checkBox;
    }

    public interface SampleConverterSelectListener {

        void onDialogClose();

    }

}
