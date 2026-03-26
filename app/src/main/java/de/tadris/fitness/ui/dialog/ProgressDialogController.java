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
import android.widget.ProgressBar;
import android.widget.TextView;

import de.tadris.fitness.R;

public class ProgressDialogController {

    private final AlertDialog dialog;
    private TextView infoView;
    private ProgressBar progressBar;

    public ProgressDialogController(Activity context, String title) {
        this(context);
        setTitle(title);
    }

    private ProgressDialogController(Activity context) {
        this.dialog = new AlertDialog.Builder(context)
                .setView(R.layout.dialog_progress)
                .setCancelable(false)
                .create();
    }

    private void setTitle(String title) {
        dialog.setTitle(title);
    }

    public void setIndeterminate(boolean indeterminate){
        progressBar.setIndeterminate(indeterminate);
    }

    public void setProgress(int progress){
        progressBar.setProgress(progress);
    }

    public void setProgress(int progress, String info){
        setProgress(progress);
        infoView.setText(info);
    }

    public void show() {
        dialog.show();
        infoView = dialog.findViewById(R.id.dialogProgressInfo);
        progressBar = dialog.findViewById(R.id.dialogProgressBar);
    }

    public void cancel(){
        dialog.cancel();
    }
}
