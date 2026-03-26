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

package de.tadris.fitness.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.InputStream;

import de.tadris.fitness.R;
import de.tadris.fitness.ui.dialog.ProgressDialogController;
import de.tadris.fitness.util.io.general.IOHelper;

public class ImportGpxActivity extends FitoTrackActivity {

    private final Handler mHandler = new Handler();
    private ProgressDialogController dialogController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dialogController = new ProgressDialogController(this, getString(R.string.importWorkout));

        Uri file = getIntent().getData();

        if (file == null && getIntent().hasExtra(Intent.EXTRA_STREAM)) {
            file = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        }

        Log.d("ImportGpx", "Reading file: " + file);

        if (file != null) {
            importFile(file);
        } else {
            Toast.makeText(this, R.string.fileNotFound, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void importFile(Uri uri) {
        dialogController.show();

        new Thread(() -> {
            try {
                InputStream stream = getContentResolver().openInputStream(uri);
                IOHelper.GpxImporter.importWorkout(getApplicationContext(), stream);
                mHandler.post(() -> {
                    dialogController.cancel();
                    Toast.makeText(this, R.string.workoutImported, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LauncherActivity.class));
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                mHandler.post(() -> {
                    dialogController.cancel();
                    showErrorDialog(e, R.string.error, R.string.errorImportFailed, this::finish);
                });
            }
        }).start();
    }

}