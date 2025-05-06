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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;

import de.tadris.fitness.R;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.ui.Main;

public class TargetActivity extends Activity{
    private int colorPrimaryDark;
    private Button target_type_distance;
    private Button target_type_duration;
    private Spinner target_distance;
    private Spinner target_duration;
    private Button target_auto_pause;

    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_target);
        colorPrimaryDark = getColor(R.color.colorPrimaryDark);

        target_type_distance = findViewById(R.id.target_type_distance);
        if(!RecordActivity.workoutData.isWorkoutTypeGps()) target_type_distance.setVisibility(View.GONE);
        target_type_distance.setOnClickListener(v->{
            if(RecordActivity.workoutData.targetType == WorkoutData.TargetType.DISTANCE){
                RecordActivity.workoutData.targetType = WorkoutData.TargetType.NONE;
                target_distance.setVisibility(View.GONE);
                target_distance.setSelection(0);
                target_type_distance.setBackgroundColor(0);
                target_duration.setVisibility(View.GONE);
                target_duration.setSelection(0);
                target_type_duration.setBackgroundColor(0);
                target_auto_pause.setVisibility(View.GONE);
            }else{
                RecordActivity.workoutData.targetType = WorkoutData.TargetType.DISTANCE;
                target_distance.setVisibility(View.VISIBLE);
                target_type_distance.setBackgroundColor(colorPrimaryDark);
                target_duration.setVisibility(View.GONE);
                target_duration.setSelection(0);
                target_type_duration.setBackgroundColor(0);
                target_auto_pause.setVisibility(View.VISIBLE);
            }
        });
        target_type_duration = findViewById(R.id.target_type_duration);
        target_type_duration.setOnClickListener(v->{
            if(RecordActivity.workoutData.targetType == WorkoutData.TargetType.DURATION){
                RecordActivity.workoutData.targetType = WorkoutData.TargetType.NONE;
                target_distance.setVisibility(View.GONE);
                target_distance.setSelection(0);
                target_type_distance.setBackgroundColor(0);
                target_duration.setVisibility(View.GONE);
                target_duration.setSelection(0);
                target_type_duration.setBackgroundColor(0);
                target_auto_pause.setVisibility(View.GONE);
            }else{
                RecordActivity.workoutData.targetType = WorkoutData.TargetType.DURATION;
                target_distance.setVisibility(View.GONE);
                target_distance.setSelection(0);
                target_type_distance.setBackgroundColor(0);
                target_duration.setVisibility(View.VISIBLE);
                target_type_duration.setBackgroundColor(colorPrimaryDark);
                target_auto_pause.setVisibility(View.VISIBLE);
            }
        });

        target_distance = findViewById(R.id.target_distance);
        target_distance.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id){
                RecordActivity.workoutData.targetValue = position;
            }
            @Override public void onNothingSelected(AdapterView<?> parentView){}
        });

        target_duration = findViewById(R.id.target_duration);
        target_duration.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id){
                RecordActivity.workoutData.targetValue = position*5;
            }
            @Override public void onNothingSelected(AdapterView<?> parentView){}
        });

        target_auto_pause = findViewById(R.id.target_auto_pause);
        target_auto_pause.setOnClickListener(v->{
            RecordActivity.workoutData.targetAutoPause = !RecordActivity.workoutData.targetAutoPause;
            target_auto_pause.setBackgroundColor(RecordActivity.workoutData.targetAutoPause ? colorPrimaryDark : 0);
        });
        findViewById(R.id.target_done).setOnClickListener(v->{
            if(RecordActivity.workoutData.targetValue == 0)
                RecordActivity.workoutData.targetType = WorkoutData.TargetType.NONE;
            finishAndRemoveTask();
        });
        if(Main.isScreenRound){
            target_type_distance.setPadding(Main.vw10, 0, 5, 0);
            target_type_duration.setPadding(5, 0, Main.vw10, 0);
            target_auto_pause.setPadding(Main.vw10, 0, Main.vw10, 0);
        }
        if(RecordActivity.workoutData.targetType == WorkoutData.TargetType.DISTANCE){
            target_type_distance.setBackgroundColor(colorPrimaryDark);
            target_distance.setVisibility(View.VISIBLE);
            target_distance.setSelection(RecordActivity.workoutData.targetValue);
            target_auto_pause.setVisibility(View.VISIBLE);
        }else if(RecordActivity.workoutData.targetType == WorkoutData.TargetType.DURATION){
            target_type_duration.setBackgroundColor(colorPrimaryDark);
            target_duration.setVisibility(View.VISIBLE);
            if(RecordActivity.workoutData.targetValue >= 5)
                target_duration.setSelection(RecordActivity.workoutData.targetValue/5);
            target_auto_pause.setVisibility(View.VISIBLE);
        }
        target_auto_pause.setBackgroundColor(RecordActivity.workoutData.targetAutoPause ? colorPrimaryDark : 0);
    }
    @Override public void onPause(){
        if(RecordActivity.workoutData.targetValue == 0)
            RecordActivity.workoutData.targetType = WorkoutData.TargetType.NONE;
        super.onPause();
    }
}
