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

package de.tadris.fitness.ui.record;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import de.tadris.fitness.R;
import static de.tadris.fitness.data.SettingsStore.WorkoutSettings.WorkoutSetting.*;

import de.tadris.fitness.ui.Main;
import de.tadris.fitness.ui.settings.MenuItem;
import de.tadris.fitness.ui.settings.MenuMaps;
import de.tadris.fitness.ui.settings.MenuScreen;

public class SettingsActivity extends MenuScreen{
    private MenuItem target;
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        if(RecordActivity.workoutSettings == null){
            Log.e(Main.LOG_TAG, "SettingsActivity No workoutSettings, cancel");
            Toast.makeText(this, R.string.fail_open_settings, Toast.LENGTH_LONG).show();
            finishAndRemoveTask();
            return;
        }

        TextView label = findViewById(R.id.label);
        String label_text = getString(RecordActivity.workoutData.getWorkoutTypeName()) + " " + getString(R.string.settings_label);
        label.setText(label_text);

        if(!RecordActivity.isPreparing){
            target = new MenuItem(this, R.string.record_target);
            target.setOnClickListener(v->startActivity(new Intent(this, TargetActivity.class)));
            setTargetValue();
            list.addView(target);
        }
        list.addView(new SettingsItem(
                this
                ,"screenOn"
                ,R.string.settings_screen
                ,RecordActivity.workoutSettings.screenOn
        ));
        if(RecordActivity.workoutData.isWorkoutTypeGps()){
            boolean available = RecordActivity.hasLocationPermission && RecordActivity.locationEnabled;
            list.addView(new SettingsItem(
                    this
                    ,"screenMap"
                    ,R.string.settings_screen_map
                    ,available ? RecordActivity.workoutSettings.screenMap : UNAVAILABLE
            ));
            MenuItem mapSource = new MenuItem(this, R.string.map_source);
            mapSource.setOnClickListener(v->MenuMaps.onMapSourceClick(mapSource));
            MenuMaps.setMapSourceValue(mapSource);
            list.addView(mapSource);

            list.addView(new SettingsItem(
                    this
                    ,"screenCurrent"
                    ,R.string.settings_screen_current
                    ,available ? RecordActivity.workoutSettings.screenCurrent : UNAVAILABLE
            ));
            if(RecordActivity.workoutData.isWorkoutTypeEle()){
                list.addView(new SettingsItem(
                        this
                        ,"screenTotal"
                        ,R.string.settings_screen_total
                        ,available ? RecordActivity.workoutSettings.screenTotal : UNAVAILABLE
                ));
                list.addView(new SettingsItem(
                        this
                        ,"screenElevation"
                        ,R.string.settings_screen_ele
                        ,available ? RecordActivity.workoutSettings.screenElevation : UNAVAILABLE
                ));
            }
            list.addView(new SettingsItem(
                    this
                    ,"screenSpeed"
                    ,R.string.settings_screen_speed
                    ,available ? RecordActivity.workoutSettings.screenSpeed : UNAVAILABLE
            ));
        }
        list.addView(new SettingsItem(
                this
                ,"screenHeartRate"
                ,R.string.settings_screen_heart_rate
                ,RecordActivity.hasHRPermission ? RecordActivity.workoutSettings.screenHeartRate : UNAVAILABLE
        ));
        list.addView(new SettingsItem(
                this
                ,"screenTarget"
                ,R.string.settings_screen_target
                ,RecordActivity.workoutSettings.screenTarget
        ));
    }
    @Override public void onResume(){
        super.onResume();
        setTargetValue();
    }
    private void setTargetValue(){
        if(target == null) return;
        switch(RecordActivity.workoutData.primaryTargetType){
            case NONE:
                target.value.setText(R.string.none);
                break;
            case DISTANCE:
                target.value.setText(R.string.distance);
                break;
            case DURATION:
                target.value.setText(R.string.duration);
                break;
            case SPEED:
                target.value.setText(R.string.speed);
                break;
            case HEART_RATE:
                target.value.setText(R.string.heart_rate);
                break;
        }
    }
}
