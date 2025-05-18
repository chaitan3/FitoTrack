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

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import de.tadris.fitness.R;
import de.tadris.fitness.data.SettingsStore;
import de.tadris.fitness.data.SettingsStore.WorkoutSettings.WorkoutSetting;
import static de.tadris.fitness.data.SettingsStore.WorkoutSettings.WorkoutSetting.*;
import de.tadris.fitness.ui.Main;

public class SettingsItem extends LinearLayout{
    private final String name;
    private WorkoutSetting setting;
    private final TextView value;

    SettingsItem(Context context, String name, int labelText, WorkoutSetting setting){
        super(context);
        this.name = name;
        this.setting = setting;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.menu_item, this, true);
        findViewById(R.id.menu_item).getLayoutParams().height = Main.menuItemHeight;
        ((TextView)findViewById(R.id.label)).setText(labelText);
        value = findViewById(R.id.value);
        setValue();
        setOnClickListener(v->toggleValue());
    }
    private void toggleValue(){
        if(setting == UNAVAILABLE){
            Toast.makeText(getContext(), R.string.fail_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        setting = ws2b() ? DISABLE : ENABLE;
        setValue();
        Main.runInBackground(()->
                SettingsStore.storeSetting(
                        getContext()
                        ,name
                        ,RecordActivity.workoutSettings.workoutType
                        ,ws2b()
                )
        );
        switch(name){
            case "screenOn" -> RecordActivity.workoutSettings.screenOn = setting;
            case "autoPause" -> RecordActivity.workoutSettings.autoPause = setting;
            case "screenMap" -> RecordActivity.workoutSettings.screenMap = setting;
            case "screenCurrent" -> RecordActivity.workoutSettings.screenCurrent = setting;
            case "screenTotal" -> RecordActivity.workoutSettings.screenTotal = setting;
            case "screenSpeed" -> RecordActivity.workoutSettings.screenSpeed = setting;
            case "screenElevation" -> RecordActivity.workoutSettings.screenElevation = setting;
            case "screenHeartRate" -> RecordActivity.workoutSettings.screenHeartRate = setting;
            case "screenTarget" -> RecordActivity.workoutSettings.screenTarget = setting;
        }
    }
    private void setValue(){
        if(setting == UNAVAILABLE){
            value.setText(R.string.unavailable);
        }else if(name.equals("screenOn")){
            value.setText(ws2b() ? R.string.settings_screen_on : R.string.settings_screen_off);
        }else{
            value.setText(ws2b() ? R.string.on : R.string.off);
        }
    }
    private boolean ws2b(){return setting == ENABLE || setting == ENABLED;}
}
