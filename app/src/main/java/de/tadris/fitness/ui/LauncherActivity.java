/*
 * Copyright (c) 2022 Jannis Scheibe <jannis@tadris.de>
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.core.splashscreen.SplashScreen;

import java.util.ArrayList;
import java.util.List;

import de.tadris.fitness.BuildConfig;
import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.data.RecordingType;
import de.tadris.fitness.data.migration.Migration;
import de.tadris.fitness.data.migration.Migration15DistancePeriod;
import de.tadris.fitness.data.migration.Migration16Energy;
import de.tadris.fitness.data.migration.Migration16PausesRepetitions;
import de.tadris.fitness.data.migration.MigrationCleanData;
import de.tadris.fitness.data.preferences.UserPreferences;
import de.tadris.fitness.map.MapManager;
import de.tadris.fitness.recording.RecordingState;
import de.tadris.fitness.recording.event.AggregatedRecordingValuesEvent;
import de.tadris.fitness.recording.event.RecordingStateChangeEvent;
import de.tadris.fitness.ui.dialog.ProgressDialogController;
import de.tadris.fitness.ui.record.RecordWorkoutActivity;

public class LauncherActivity extends Activity implements Migration.MigrationListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle the splash screen transition.
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setTheme(R.style.AppThemeNoActionbar);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        try {
            Instance.getInstance(this); // Initially load instance class
            Instance.getInstance(this).themes.updateDarkModeSetting();
            if (Instance.getInstance(this).userPreferences.getLastVersionCode() < BuildConfig.VERSION_CODE) {
                runMigrations();
            } else {
                Instance.getInstance(this).userPreferences.updateLastVersionCode();
                Instance.getInstance(this).shortcuts.init();
                MapManager.initMapProvider(this);
                start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorDialog(e);
        }
    }

    ProgressDialogController progressDialog;

    private void runMigrations() {
        UserPreferences preferences = Instance.getInstance(this).userPreferences;
        List<Migration> migrations = new ArrayList<>();
        if (preferences.getLastVersionCode() < 1300) {
            migrations.add(new MigrationCleanData(this, this));
        }
        if (preferences.getLastVersionCode() < 1500) {
            migrations.add(new Migration15DistancePeriod(this, this));
        }
        if(preferences.getLastVersionCode() < 1600) {
            migrations.add(new Migration16PausesRepetitions(this, this));
        }
        if(preferences.getLastVersionCode() < 1601) {
            migrations.add(new Migration16Energy(this, this));
        }
        progressDialog = new ProgressDialogController(this, getString(R.string.runningMigrations));
        progressDialog.show();
        new Thread(() -> {
            try {
                for (Migration migration : migrations) {
                    Log.i("Migration", "Running migration " + migration.getClass().getSimpleName());
                    migration.migrate();
                }
                preferences.updateLastVersionCode();
                runOnUiThread(() -> {
                    progressDialog.cancel();
                    init();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> showErrorDialog(e));
            }
        }).start();
    }

    private void showErrorDialog(Exception e) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.launchError)
                .setMessage(e.getMessage())
                .setPositiveButton(R.string.okay, null)
                .setOnDismissListener(dialog -> finish())
                .show();
    }

    @Override
    public void onProgressUpdate(int progress) {
        runOnUiThread(() -> progressDialog.setProgress(progress));
    }

    private void start() {
        RecordingState state = RecordingStateChangeEvent.Companion.getCurrent().getNewState();
        if (state != RecordingState.IDLE && state != RecordingState.STOPPED) {
            // currently recording a workout -> go to recording activity
            RecordingType type = AggregatedRecordingValuesEvent.Companion.getCurrent().getWorkoutType().getRecordingType();
            Intent recorderActivityIntent = new Intent(this, type.recorderActivityClass);
            recorderActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            recorderActivityIntent.setAction(RecordWorkoutActivity.RESUME_ACTION);
            startActivity(recorderActivityIntent);
        } else {
            // Go to Workout List
            Intent listWorkoutActivityIntent = new Intent(this, ListWorkoutsActivity.class);
            listWorkoutActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(listWorkoutActivityIntent);
        }

        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.stay);
    }
}
