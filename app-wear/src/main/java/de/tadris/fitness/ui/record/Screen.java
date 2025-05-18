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
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.health.services.client.data.ExerciseState;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import de.tadris.fitness.data.WorkoutData.HeartRateSample;
import de.tadris.fitness.data.WorkoutData.LocationSample;
import de.tadris.fitness.ui.Main;

public abstract class Screen extends Fragment{
    static int colorPrimary = -1;
    static int colorProgressDone;
    static int colorProgressDoneDark;
    static int colorActive;
    static int colorInactive;
    static int colorCircleFill;

    RecordActivity recordActivity;
    TextView time;
    ScreenField calories;
    ScreenField distance;
    ScreenField duration;
    ScreenField eleGain;
    ScreenField eleLoss;
    ScreenField heartRate;
    ScreenField pause;
    ScreenField speed;
    ScreenField speedAvg;
    CircularProgressIndicator targetProgress;

    private static ExerciseState lastExerciseState;
    private static String lastTime;
    private static double lastCalories;
    static double lastDistance;
    static long lastDuration;
    private static boolean hideEleGain = false;
    private static double lastEleGain;
    private static boolean hideEleLoss = false;
    private static double lastEleLoss;
    static HeartRateSample lastHeartRate;
    private static long lastPause;
    private static double lastSpeed;
    private static double lastSpeedAvg;
    static int lastTargetProgress;
    private boolean isPaused = false;
    boolean backFill = true;

    static void resetStaticFields(){
        lastExerciseState = null;
        lastTime = "";
        lastCalories = 0;
        lastDistance = 0;
        lastDuration = 0;
        hideEleGain = false;
        lastEleGain = 0;
        hideEleLoss = false;
        lastEleLoss = 0;
        lastHeartRate = null;
        lastPause = 0;
        lastSpeed = 0;
        lastSpeedAvg = 0;
        lastTargetProgress = 0;
    }
    @Override public void onAttach(@NonNull Context context){
        super.onAttach(context);
        try{
            recordActivity = (RecordActivity) context;
        }catch(ClassCastException e){
            Log.e(Main.LOG_TAG, "Screen.onAttach exception: " + e.getMessage());
        }
    }

    @Override public void onResume(){super.onResume();show();}
    void show(){
        if(time != null) time.setText(lastTime);
        if(calories != null) calories.updateValue(lastCalories);
        if(distance != null) distance.updateValue(lastDistance);
        if(duration != null) duration.updateValue(lastDuration);
        if(eleGain != null) updateEleGain(lastEleGain);
        if(eleLoss != null) updateEleLoss(lastEleLoss);
        if(heartRate != null && lastHeartRate != null) heartRate.updateValue(lastHeartRate.heartRate);
        if(pause != null) pause.updateValue(lastPause);
        if(speed != null) speed.updateValue(lastSpeed);
        if(speedAvg != null) speedAvg.updateValue(lastSpeedAvg);
        if(targetProgress != null) targetProgress.setProgress(lastTargetProgress);
        setPaused();
    }
    @Override public void onPause(){super.onPause();hide();}
    void hide(){backFill = true;}

    void updateState(ExerciseState exerciseState){
        lastExerciseState = exerciseState;
        setPaused();
    }
    private void setPaused(){
        if(recordActivity == null || lastExerciseState == null) return;
        if(!isPaused && lastExerciseState.isPaused()){
            isPaused = true;
            if(distance != null) distance.setInactive();
            if(duration != null) duration.setInactive();
            if(eleGain != null) eleGain.setInactive();
            if(eleLoss != null) eleLoss.setInactive();
            if(pause != null) pause.setActive();
            if(speed != null) speed.setInactive();
            if(speedAvg != null) speedAvg.setInactive();
        }else if(isPaused && lastExerciseState == ExerciseState.ACTIVE){
            isPaused = false;
            if(distance != null) distance.setActive();
            if(duration != null) duration.setActive();
            if(eleGain != null) eleGain.setActive();
            if(eleLoss != null) eleLoss.setActive();
            if(pause != null) pause.setInactive();
            if(speed != null) speed.setActive();
            if(speedAvg != null) speedAvg.setActive();
        }
    }
    void updateTime(String value){
        lastTime = value;
        if(time != null) time.setText(value);
    }
    void updateCalories(double value){
        lastCalories = value;
        if(calories != null) calories.updateValue(value);
    }
    void updateDistance(double value){
        lastDistance = value;
        if(distance != null) distance.updateValue(value);
    }
    void updateDuration(long value){
        lastDuration = value;
        if(duration != null) duration.updateValue(value);
    }
    void updateEleGain(double value){
        lastEleGain = value;
        if(eleGain == null) return;
        if(value < 10 && lastEleLoss > 10){
            if(hideEleGain) return;
            hideEleGain = true;
            eleGain.setVisibility(View.GONE);
            return;
        }
        if(hideEleGain){
            hideEleGain = false;
            eleGain.setVisibility(View.VISIBLE);
        }
        eleGain.updateValue(value);
    }
    void updateEleLoss(double value){
        lastEleLoss = value;
        if(eleLoss == null) return;
        if(value < 10 && lastEleGain > 10){
            if(hideEleLoss) return;
            hideEleLoss = true;
            eleLoss.setVisibility(View.GONE);
            return;
        }
        if(hideEleLoss){
            hideEleLoss = false;
            eleLoss.setVisibility(View.VISIBLE);
        }
        eleLoss.updateValue(value);
    }
    void updateHeartRate(HeartRateSample sample, boolean isPaused){
        lastHeartRate = sample;
        if(heartRate != null) heartRate.updateValue(lastHeartRate.heartRate);
    }
    void updatePause(long value){
        lastPause = value;
        if(pause != null) pause.updateValue(value);
    }
    void updateLocation(LocationSample sample){}
    void updateSpeed(long time, double value){
        lastSpeed = value;
        if(speed != null) speed.updateValue(value);
    }
    void updateSpeedAvg(double value){
        lastSpeedAvg = value;
        if(speedAvg != null) speedAvg.updateValue(value);
    }
    void updateTargetProgress(int value){
        lastTargetProgress = value;
        if(targetProgress != null) targetProgress.setProgress(value);
    }
    void setTargetAchieved(){
        if(targetProgress != null){
            targetProgress.setIndicatorColor(colorProgressDoneDark);
            targetProgress.setTrackColor(colorProgressDone);
        }
    }
    void setTargetUnAchieved(){
        if(targetProgress != null){
            targetProgress.setIndicatorColor(colorPrimary);
            targetProgress.setTrackColor(0);
        }
    }
    void onScroll(boolean direction){
        if(direction){
            recordActivity.swipeUp();
        }else{
            recordActivity.swipeDown();
        }
    }
}