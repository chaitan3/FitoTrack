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

package de.tadris.fitness.ui.settings.export;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.ArrayAdapter;

import de.tadris.fitness.R;
import de.tadris.fitness.util.autoexport.target.ExportTarget;

public class SelectExportTargetTypeDialog {

    private final Activity context;
    private final ExportTargetTypeSelectListener listener;
    private final ExportTarget[] targetTypes;

    public SelectExportTargetTypeDialog(Activity context, ExportTargetTypeSelectListener listener) {
        this.context = context;
        this.listener = listener;
        this.targetTypes = ExportTarget.Companion.getExportTargetTypes();
    }

    public void show() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, R.layout.select_dialog_singlechoice_material);
        for (ExportTarget targetType : targetTypes) {
            arrayAdapter.add(context.getString(targetType.getTitleRes()));
        }

        builderSingle.setTitle(R.string.selectExportTarget);
        builderSingle.setAdapter(arrayAdapter, (dialog, which) -> listener.onTargetTypeSelect(targetTypes[which]));
        builderSingle.show();
    }

    public interface ExportTargetTypeSelectListener {
        void onTargetTypeSelect(ExportTarget target);
    }

}
