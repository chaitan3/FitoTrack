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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import de.tadris.fitness.R;
import de.tadris.fitness.data.WorkoutData.TargetType;
import de.tadris.fitness.ui.Main;
import de.tadris.fitness.ui.settings.MenuItem;

public class TargetActivity extends Activity{
    private TargetType primaryTargetType;
    private int primaryTargetValue;
    private int primaryTargetValueHigh;
    private boolean targetAutoPause;
    private TargetType secondaryTargetType;
    private int secondaryTargetValue;
    private int secondaryTargetValueHigh;

    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        primaryTargetType = RecordActivity.workoutData.primaryTargetType;
        primaryTargetValue = RecordActivity.workoutData.primaryTargetValue;
        primaryTargetValueHigh = RecordActivity.workoutData.primaryTargetValueHigh;
        targetAutoPause = RecordActivity.workoutData.targetAutoPause;
        secondaryTargetType = RecordActivity.workoutData.secondaryTargetType;
        secondaryTargetValue = RecordActivity.workoutData.secondaryTargetValue;
        secondaryTargetValueHigh = RecordActivity.workoutData.secondaryTargetValueHigh;

        if(primaryTargetType == TargetType.NONE){
            loadTypeSelect(true);
        }else{
            loadMain();
        }
    }
    @Override public void onPause(){
        super.onPause();
        if(primaryTargetValue == 0){
            primaryTargetType = TargetType.NONE;
            secondaryTargetType = TargetType.NONE;
            secondaryTargetValue = 0;
            secondaryTargetValueHigh = 0;
        }else if(secondaryTargetValue == 0){
            secondaryTargetType = TargetType.NONE;
        }
        RecordActivity.workoutData.primaryTargetType = primaryTargetType;
        RecordActivity.workoutData.primaryTargetValue = primaryTargetValue;
        RecordActivity.workoutData.primaryTargetValueHigh = primaryTargetValueHigh;
        RecordActivity.workoutData.targetAutoPause = targetAutoPause;
        RecordActivity.workoutData.secondaryTargetType = secondaryTargetType;
        RecordActivity.workoutData.secondaryTargetValue = secondaryTargetValue;
        RecordActivity.workoutData.secondaryTargetValueHigh = secondaryTargetValueHigh;
    }
    private void loadMain(){
        setContentView(R.layout.record_target);
        LinearLayout record_main = findViewById(R.id.target_main);
        MenuItem primary = new MenuItem(this, R.string.record_target, Main.vh25);
        primary.setOnClickListener(v->loadTypeSelect(true));
        record_main.addView(primary);
        if(Main.isScreenRound){
            ((LinearLayout.LayoutParams)primary.getLayoutParams()).setMargins(Main.vw15, 0, Main.vw15, 0);
        }

        switch(primaryTargetType){
            case DISTANCE:
                primary.value.setText(getString(R.string.distance_main, primaryTargetValue));
                break;
            case DURATION:
                primary.value.setText(durationValue(R.string.duration_main, primaryTargetValue, 60));
                break;
            case SPEED:
                int number_high = Math.floorDiv(primaryTargetValue, 10);
                int number_low = primaryTargetValue % 10;
                if(number_low == 0){
                    primary.value.setText(getString(R.string.speed_main_whole, number_high));
                }else{
                    primary.value.setText(getString(R.string.speed_main, number_high, number_low));
                }
                break;
            case HEART_RATE:
                primary.value.setText(getString(R.string.heart_rate_main, primaryTargetValue, primaryTargetValueHigh));
                break;
        }
        if(primaryTargetType == TargetType.DISTANCE || primaryTargetType == TargetType.DURATION){
            MenuItem auto_pause = new MenuItem(this, R.string.record_target_auto_pause, Main.vh25);
            auto_pause.value.setText(getString(targetAutoPause ? R.string.on : R.string.off));
            auto_pause.setOnClickListener(v->{
                targetAutoPause = !targetAutoPause;
                auto_pause.value.setText(getString(targetAutoPause ? R.string.on : R.string.off));
            });
            record_main.addView(auto_pause);
            MenuItem secondary = new MenuItem(this, R.string.target_secondary, Main.vh25);
            switch(secondaryTargetType){
                case DISTANCE:
                    secondary.value.setText(getString(R.string.distance_main, secondaryTargetValue));
                    break;
                case DURATION:
                    secondary.value.setText(durationValue(R.string.duration_main, secondaryTargetValue, 60));
                    break;
                case SPEED:
                    int number_high = Math.floorDiv(secondaryTargetValue, 10);
                    int number_low = secondaryTargetValue % 10;
                    if(number_low == 0){
                        secondary.value.setText(getString(R.string.speed_main_whole, number_high));
                    }else{
                        secondary.value.setText(getString(R.string.speed_main, number_high, number_low));
                    }
                    break;
                case HEART_RATE:
                    secondary.value.setText(getString(R.string.heart_rate_main, secondaryTargetValue, secondaryTargetValueHigh));
                    break;
                case NONE:
                    secondary.value.setText(getString(R.string.none));
                    break;
            }
            secondary.setOnClickListener(v->loadTypeSelect(false));
            record_main.addView(secondary);
            if(Main.isScreenRound){
                ((LinearLayout.LayoutParams)secondary.getLayoutParams()).setMargins(Main.vw15, 0, Main.vw15, 0);
                findViewById(R.id.target_label).getLayoutParams().height = Main.vw15;
            }
        }
    }
    private void loadTypeSelect(boolean primary){
        setContentView(R.layout.record_target_type);
        findViewById(R.id.target_none).setOnClickListener(v->{
            if(primary){
                primaryTargetType = TargetType.NONE;
                primaryTargetValue = 0;
                targetAutoPause = false;
                secondaryTargetType = TargetType.NONE;
                secondaryTargetValue = 0;
                finishAndRemoveTask();
            }else{
                secondaryTargetType = TargetType.NONE;
                secondaryTargetValue = 0;
                loadMain();
            }
        });
        TextView target_distance = findViewById(R.id.target_distance);
        TextView target_duration = findViewById(R.id.target_duration);
        TextView target_speed = findViewById(R.id.target_speed);
        TextView target_heart_rate = findViewById(R.id.target_heart_rate);
        target_distance.setOnClickListener(v->loadNumber(primary, TargetType.DISTANCE));
        target_duration.setOnClickListener(v->loadNumber(primary, TargetType.DURATION));
        target_speed.setOnClickListener(v->loadNumber(primary, TargetType.SPEED));
        target_heart_rate.setOnClickListener(v->loadNumber(primary, TargetType.HEART_RATE));
        if(!primary){
            switch(primaryTargetType){
                case DISTANCE:
                    target_distance.setTextColor(Screen.colorInactive);
                    break;
                case DURATION:
                    target_duration.setTextColor(Screen.colorInactive);
                    break;
                case SPEED:
                    target_speed.setTextColor(Screen.colorInactive);
                    break;
                case HEART_RATE:
                    target_heart_rate.setTextColor(Screen.colorInactive);
                    break;
            }
        }
    }

    private TextView number_preview;
    private TextView number_preview_high;
    private TextView number_back;
    private TextView number_done;
    private TargetType number_type;
    private int number_value;
    private void loadNumber(boolean primary, TargetType targetType){
        if(!primary && primaryTargetType == targetType) return;
        number_type = targetType;
        number_value = 0;
        setContentView(R.layout.record_target_number);
        number_preview = findViewById(R.id.number_preview);
        if(number_type == TargetType.HEART_RATE){
            findViewById(R.id.number_preview_dash).setVisibility(View.VISIBLE);
            number_preview_high = findViewById(R.id.number_preview_high);
            number_preview_high.setVisibility(View.VISIBLE);
        }
        number_back = findViewById(R.id.b_back);
        number_done = findViewById(R.id.b_done);
        updateNumberPreview(-1);
        ((ConstraintLayout.LayoutParams)findViewById(R.id.b_0).getLayoutParams()).setMargins(Main.vw10, 0, Main.vw10, 0);
        findViewById(R.id.b_0).setOnClickListener(v->updateNumberPreview(0));
        findViewById(R.id.b_1).setOnClickListener(v->updateNumberPreview(1));
        findViewById(R.id.b_2).setOnClickListener(v->updateNumberPreview(2));
        findViewById(R.id.b_3).setOnClickListener(v->updateNumberPreview(3));
        findViewById(R.id.b_4).setOnClickListener(v->updateNumberPreview(4));
        findViewById(R.id.b_5).setOnClickListener(v->updateNumberPreview(5));
        findViewById(R.id.b_6).setOnClickListener(v->updateNumberPreview(6));
        findViewById(R.id.b_7).setOnClickListener(v->updateNumberPreview(7));
        findViewById(R.id.b_8).setOnClickListener(v->updateNumberPreview(8));
        findViewById(R.id.b_9).setOnClickListener(v->updateNumberPreview(9));
        findViewById(R.id.b_back).setOnClickListener(v->{
            if(number_value == 0) return;
            number_value = Math.floorDiv(number_value, 10);
            updateNumberPreview(-1);
        });
        findViewById(R.id.b_done).setOnClickListener(v->{
            if(number_value == 0) return;
            if(number_type == TargetType.DURATION){
                number_value = (number_value % 100) + Math.floorDiv(number_value, 100) * 60;
            }
            if(primary){
                primaryTargetType = number_type;
                if(number_type == TargetType.HEART_RATE){
                    primaryTargetValue = Math.floorDiv(number_value, 1000);
                    primaryTargetValueHigh = number_value % 1000;
                }else{
                    primaryTargetValue = number_value;
                }
                if(secondaryTargetType == primaryTargetType){
                    secondaryTargetType = TargetType.NONE;
                    secondaryTargetValue = 0;
                }
            }else{
                secondaryTargetType = number_type;
                if(number_type == TargetType.HEART_RATE){
                    secondaryTargetValue = Math.floorDiv(number_value, 1000);
                    secondaryTargetValueHigh = number_value % 1000;
                }else{
                    secondaryTargetValue = number_value;
                }
            }
            loadMain();
        });
        if(Main.isScreenRound){
            if(number_type == TargetType.HEART_RATE){
                ((ConstraintLayout.LayoutParams)number_preview_high.getLayoutParams()).setMargins(Main.vw30, 0, 0, 0);
                ((ConstraintLayout.LayoutParams)number_preview.getLayoutParams()).setMargins(0, 0, Main.vw30, 0);
            }else{
                ((ConstraintLayout.LayoutParams)number_preview.getLayoutParams()).setMargins(Main.vw30, 0, Main.vw30, 0);
            }
        }
    }
    private void updateNumberPreview(int number){
        switch(number_type){
            case DISTANCE:
                if(number >= 0 && number_value < 1000) number_value = number_value * 10 + number;
                number_preview.setText(getString(R.string.distance_preview, number_value));
                break;
            case DURATION:
                if(number >= 0 && number_value < 1000) number_value = number_value * 10 + number;
                number_preview.setText(durationValue(R.string.duration_preview, number_value, 100));
                break;
            case SPEED:
                if(number >= 0 && number_value < 1000) number_value = number_value * 10 + number;
                int speed_high = Math.floorDiv(number_value, 10);
                int speed_low = number_value % 10;
                number_preview.setText(getString(R.string.speed_preview, speed_high, speed_low));
                break;
            case HEART_RATE:
                if(number >= 0 && number_value < 100000) number_value = number_value * 10 + number;
                String bpm = String.valueOf(number_value);
                number_preview_high.setText(bpm.length() > 3 ? bpm.substring(0, bpm.length()-3) : "0");
                number_preview_high.setTextColor(bpm.length() <= 3 ? Screen.colorInactive : Screen.colorActive);
                number_preview.setText(bpm.length() > 3 ? bpm.substring(bpm.length()-3) : bpm);
                break;
        }
        number_preview.setTextColor(number_value == 0 ? Screen.colorInactive : Screen.colorActive);
        number_back.setTextColor(number_value == 0 ? Screen.colorInactive : Screen.colorActive);
        number_done.setTextColor(number_value == 0 ? Screen.colorInactive : Screen.colorActive);
    }

    private String durationValue(int template, int value, int divider){
        return getString(template,
                Math.floorDiv(value, divider),
                Math.floorDiv(value % divider, 10),
                value % 10
        );
    }
}
