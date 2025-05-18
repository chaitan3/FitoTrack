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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.tadris.fitness.R;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.data.WorkoutData.TargetType;
import de.tadris.fitness.ui.Main;
import de.tadris.fitness.util.Util;

public class ScreenTarget extends Screen{
    private TargetType primaryTargetType;
    private int primaryTargetValue;
    private int primaryTargetValueHigh;
    private TargetType secondaryTargetType;
    private int secondaryTargetValue;
    private int secondaryTargetValueHigh;
    private boolean targetAchieved = false;
    private RecordProgressBar progressBar;
    private RecordProgressBar progressBar2;
    @Override public @Nullable View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ){
        View rootView = inflater.inflate(R.layout.record_screen_target, container, false);
        primaryTargetType = RecordActivity.workoutData.primaryTargetType;
        primaryTargetValue = RecordActivity.workoutData.primaryTargetValue;
        primaryTargetValueHigh = RecordActivity.workoutData.primaryTargetValueHigh;
        secondaryTargetType = RecordActivity.workoutData.secondaryTargetType;
        secondaryTargetValue = RecordActivity.workoutData.secondaryTargetValue;
        secondaryTargetValueHigh = RecordActivity.workoutData.secondaryTargetValueHigh;

        time = rootView.findViewById(R.id.time);
        time.setText(recordActivity.currentTime);
        if(secondaryTargetType == TargetType.NONE &&
                primaryTargetType != TargetType.SPEED &&
                primaryTargetType != TargetType.HEART_RATE
        ){
            duration = rootView.findViewById(R.id.topField);
            duration.setType(ScreenField.Type.DURATION);
            duration.setCenter();
            rootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
            speedAvg = rootView.findViewById(R.id.speedAvg);
            speedAvg.setType(ScreenField.Type.SPEED_KMH);
            speedAvg.setUnitAverage();
            speed = rootView.findViewById(R.id.speed);
            speed.setType(ScreenField.Type.SPEED_KMH);
            distance = rootView.findViewById(R.id.bottomField);
            distance.setType(ScreenField.Type.DISTANCE_KM);
        }else if((primaryTargetType == TargetType.DISTANCE && secondaryTargetType == TargetType.DURATION)){
            distance = rootView.findViewById(R.id.topField);
            distance.setType(ScreenField.Type.DISTANCE_KM);
            progressBar = rootView.findViewById(R.id.progressBar);
            rootView.findViewById(R.id.speedAvg).setVisibility(View.GONE);
            rootView.findViewById(R.id.speed).setVisibility(View.GONE);
            speed = rootView.findViewById(R.id.bottomField);
            speed.setType(ScreenField.Type.SPEED_KMH);
        }else if(primaryTargetType == TargetType.DURATION && secondaryTargetType == TargetType.DISTANCE){
            duration = rootView.findViewById(R.id.topField);
            duration.setType(ScreenField.Type.DURATION);
            duration.setCenter();
            progressBar = rootView.findViewById(R.id.progressBar);
            rootView.findViewById(R.id.speedAvg).setVisibility(View.GONE);
            rootView.findViewById(R.id.speed).setVisibility(View.GONE);
            speed = rootView.findViewById(R.id.bottomField);
            speed.setType(ScreenField.Type.SPEED_KMH);
        }else if(primaryTargetType == TargetType.DURATION){
            duration = rootView.findViewById(R.id.topField);
            duration.setType(ScreenField.Type.DURATION);
            duration.setCenter();
            distance = rootView.findViewById(R.id.bottomField);
            distance.setType(ScreenField.Type.DISTANCE_KM);
        }else{
            distance = rootView.findViewById(R.id.topField);
            distance.setType(ScreenField.Type.DISTANCE_KM);
            duration = rootView.findViewById(R.id.bottomField);
            duration.setType(ScreenField.Type.DURATION);
            duration.setCenter();
        }
        if(primaryTargetType == TargetType.SPEED || secondaryTargetType == TargetType.SPEED){
            progressBar = rootView.findViewById(R.id.progressBar);
            progressBar.setProgressSpeed();
            progressBar2 = rootView.findViewById(R.id.progressBar2);
            progressBar2.setProgressSpeed();
            progressBar2.setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.speedAvg).setVisibility(View.GONE);
            rootView.findViewById(R.id.speed).setVisibility(View.GONE);
        }else if(primaryTargetType == TargetType.HEART_RATE || secondaryTargetType == TargetType.HEART_RATE){
            progressBar = rootView.findViewById(R.id.progressBar);
            progressBar.setProgressHeartRate();
            rootView.findViewById(R.id.speedAvg).setVisibility(View.GONE);
            rootView.findViewById(R.id.speed).setVisibility(View.GONE);
        }
        targetProgress = rootView.findViewById(R.id.targetProgress);
        if(Main.isScreenRound)
            rootView.findViewById(R.id.bottomField).setPadding(Main.vw10, 0, Main.vw10, 0);
        return rootView;
    }
    @Override void show(){
        super.show();
        if(recordActivity == null) return;
        if(RecordActivity.workoutData.primaryTargetType != primaryTargetType ||
                RecordActivity.workoutData.primaryTargetValue != primaryTargetValue ||
                RecordActivity.workoutData.secondaryTargetType != secondaryTargetType ||
                RecordActivity.workoutData.secondaryTargetValue != secondaryTargetValue
        ){
            recordActivity.reloadTargetScreen(this);
            return;
        }
        if(progressBar == null) return;
        if(RecordActivity.workoutData.secondaryTargetType == TargetType.DURATION) updateDuration(lastDuration);
        if(RecordActivity.workoutData.secondaryTargetType == TargetType.DISTANCE) updateDistance(lastDistance);
    }
    @Override void setTargetAchieved(){
        super.setTargetAchieved();
        targetAchieved = true;
    }
    @Override void updateDuration(long value){
        super.updateDuration(value);
        if(secondaryTargetType != TargetType.DURATION) return;
        progressBar.setProgress(
                Math.min((int)Math.floorDiv(value, secondaryTargetValue*600), 100),
                targetAchieved ? 100 : Screen.lastTargetProgress
        );
        progressBar.setProgressText(Util.prettyDurationShort(value));
    }
    @Override void updateDistance(double value){
        super.updateDistance(value);
        if(secondaryTargetType != TargetType.DISTANCE) return;
        progressBar.setProgress(
                (int)Math.min(value/10/secondaryTargetValue, 100),
                targetAchieved ? 100 : Screen.lastTargetProgress
        );
        if(value >= 1000){
            progressBar.setProgressText(Util.distanceDoubleToString(value/1000) + getString(R.string.km));
        }else{
            progressBar.setProgressText(Math.round(value) + getString(R.string.m));
        }
    }
    @Override void updateSpeed(long time, double value){
        super.updateSpeed(time, value);
        if(primaryTargetType != TargetType.SPEED && secondaryTargetType != TargetType.SPEED) return;
        if(primaryTargetType == TargetType.SPEED){
            progressBar.setSpeed(value*3.6, (double) primaryTargetValue/10);
        }else{
            progressBar.setSpeed(value*3.6, (double) secondaryTargetValue/10);
        }
        progressBar.setProgressText(Util.speedDoubleToString(value*3.6) + getString(R.string.kmh));
    }
    @Override void updateSpeedAvg(double value){
        super.updateSpeedAvg(value);
        if(primaryTargetType != TargetType.SPEED && secondaryTargetType != TargetType.SPEED) return;
        if(primaryTargetType == TargetType.SPEED){
            progressBar2.setSpeed(value*3.6, (double) primaryTargetValue/10);
        }else{
            progressBar2.setSpeed(value*3.6, (double) secondaryTargetValue/10);
        }
        progressBar2.setProgressText(Util.speedDoubleToString(value*3.6) + getString(R.string.avg_symbol) + getString(R.string.kmh));
    }
    @Override void updateHeartRate(WorkoutData.HeartRateSample sample, boolean isPaused){
        super.updateHeartRate(sample, isPaused);
        if(primaryTargetType != TargetType.HEART_RATE && secondaryTargetType != TargetType.HEART_RATE) return;
        if(primaryTargetType == TargetType.HEART_RATE){
            progressBar.setHeartRate(sample.heartRate, primaryTargetValue, primaryTargetValueHigh);
        }else{
            progressBar.setHeartRate(sample.heartRate, secondaryTargetValue, secondaryTargetValueHigh);
        }
        progressBar.setProgressText(sample.heartRate + getString(R.string.bpm));
    }
}