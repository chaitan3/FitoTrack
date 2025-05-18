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

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import de.tadris.fitness.R;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.ui.Main;

public class RecordProgressBar extends ConstraintLayout{
    private View diff_ahead;
    private View diff_behind;
    private final View progress;
    private View progress_line;
    private TextView progress_text;
    private boolean flipProgressText = true;
    private static int widthPixels = 0;

    @SuppressLint("CutPasteId")//reassigning diff_ahead/diff_behind for SPEED
    public RecordProgressBar(Context context, AttributeSet attrs){
        super(context, attrs);
        ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.record_progress_bar, this, true);
        View root = findViewById(R.id.root);
        diff_ahead = findViewById(R.id.diff_ahead);
        diff_behind = findViewById(R.id.diff_behind);
        progress = findViewById(R.id.progress);
        progress_text = findViewById(R.id.progress_text_start);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            @Override public void onGlobalLayout(){
                if(root.getWidth() == 0) return;
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                widthPixels = root.getWidth();
                if(RecordActivity.workoutData.secondaryTargetType == WorkoutData.TargetType.DURATION ||
                        RecordActivity.workoutData.secondaryTargetType == WorkoutData.TargetType.DISTANCE
                ){
                    forceHorizontalMargin(root, Main.vw10);
                    widthPixels -= Main.vw20;
                }
            }
        });

        if(RecordActivity.workoutData.secondaryTargetType == WorkoutData.TargetType.DURATION){
            //in case of duration, being behind is a good thing
            diff_ahead.setBackgroundColor(context.getColor(R.color.progressBarBehind));
            diff_behind.setBackgroundColor(context.getColor(R.color.progressBarAhead));
        }
        if(RecordActivity.workoutData.primaryTargetType == WorkoutData.TargetType.SPEED ||
                RecordActivity.workoutData.secondaryTargetType == WorkoutData.TargetType.SPEED
        ){
            //for speed, ahead is above, not below
            diff_ahead = findViewById(R.id.diff_behind);
            diff_behind = findViewById(R.id.diff_ahead);
            diff_ahead.setBackgroundColor(context.getColor(R.color.progressBarAhead));
            diff_behind.setBackgroundColor(context.getColor(R.color.progressBarBehind));
            progress_text = findViewById(R.id.progress_text_center);
            findViewById(R.id.border).setVisibility(View.GONE);
            progress.setVisibility(View.INVISIBLE);
        }
        if(RecordActivity.workoutData.primaryTargetType == WorkoutData.TargetType.HEART_RATE ||
                RecordActivity.workoutData.secondaryTargetType == WorkoutData.TargetType.HEART_RATE
        ){
            diff_ahead = findViewById(R.id.diff_before);
            diff_behind.setBackgroundColor(context.getColor(R.color.progressBarBehind));
            progress_text = findViewById(R.id.progress_text_center);
            progress_line = findViewById(R.id.progress_line);
            progress_line.setVisibility(View.VISIBLE);
            findViewById(R.id.border).setVisibility(View.GONE);
        }
    }
    void setProgress(int percent, int lastTargetProgress){
        if(percent > 50 && flipProgressText){
            flipProgressText = false;
            progress_text.setText("");
            progress_text = findViewById(R.id.progress_text_end);
        }
        forceWidth(progress, Math.max(widthPixels * percent / 100, 1));

        if(percent + lastTargetProgress < 2) return;
        if(percent < lastTargetProgress){
            diff_ahead.setVisibility(View.GONE);
            forceWidth(diff_behind, Math.max(widthPixels * (lastTargetProgress - percent) / 100, 1));
            diff_behind.setVisibility(View.VISIBLE);
        }else{
            diff_behind.setVisibility(View.GONE);
            forceWidth(diff_ahead, Math.max(widthPixels * (percent - lastTargetProgress) / 100, 1));
            diff_ahead.setVisibility(View.VISIBLE);
        }
    }
    void setProgressText(String value){progress_text.setText(value);}
    void setProgressSpeed(){
        if(widthPixels == 0){
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
                @Override public void onGlobalLayout(){
                    if(findViewById(R.id.root).getWidth() == 0) return;
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    widthPixels = findViewById(R.id.root).getWidth();
                    setProgressSpeed();
                }
            });
        }
        forceWidth(progress, widthPixels/2);
    }
    void setSpeed(double value, double target){
        if(value < target){
            diff_ahead.setVisibility(View.GONE);
            int diffPixels;
            if(target > 5){
                diffPixels = (int) (widthPixels * (target - value) / 10);
            }else{
                diffPixels = (int) (widthPixels * (target - value) / (target*2));
            }
            forceWidth(diff_behind, Math.max(diffPixels, 3));
            diff_behind.setVisibility(View.VISIBLE);
        }else{
            diff_behind.setVisibility(View.GONE);
            int diffPixels;
            if(target > 5){
                diffPixels = (int) (widthPixels * (value - target) / 10);
            }else{
                diffPixels = (int) (widthPixels * (value - target) / (target * 2));
            }
            forceWidth(diff_ahead, Math.max(diffPixels, 1));
            diff_ahead.setVisibility(View.VISIBLE);
        }
    }
    void setProgressHeartRate(){
        if(widthPixels == 0){
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
                @Override
                public void onGlobalLayout(){
                    if(findViewById(R.id.root).getWidth() == 0) return;
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    widthPixels = findViewById(R.id.root).getWidth();
                    setProgressHeartRate();
                }
            });
        }
        forceWidth(progress, widthPixels/2);
        forceLeftMargin(progress, widthPixels/4);
        progress.setBackgroundResource(R.drawable.bg_progress_bar);

        forceWidth(progress_text, widthPixels/2);
    }
    void setHeartRate(int heartRate, int target, int targetHigh){
        if(heartRate < target){
            progress_line.setVisibility(View.GONE);
            diff_behind.setVisibility(View.GONE);
            forceWidth(diff_ahead, Math.max(widthPixels/4 * (target - heartRate) / (targetHigh-target), 1));
            diff_ahead.setVisibility(View.VISIBLE);
        }else if(heartRate > targetHigh){
            progress_line.setVisibility(View.GONE);
            diff_ahead.setVisibility(View.GONE);
            forceWidth(diff_behind, Math.max(widthPixels/4 * (heartRate - targetHigh) / (targetHigh-target), 1));
            diff_behind.setVisibility(View.VISIBLE);
        }else{
            diff_behind.setVisibility(View.GONE);
            diff_ahead.setVisibility(View.GONE);
            forceLeftMargin(progress_line, widthPixels/2 * (heartRate - target) / (targetHigh-target));
            progress_line.setVisibility(View.VISIBLE);
        }
    }
    private void forceWidth(View view, int width){
        LayoutParams params = (LayoutParams) view.getLayoutParams();
        params.width = width;
        params.matchConstraintMinWidth = width;
        params.matchConstraintMaxWidth = width;
        view.setLayoutParams(params);
    }
    private void forceLeftMargin(View view, int leftMargin){
        LayoutParams params = (LayoutParams) view.getLayoutParams();
        params.leftMargin = leftMargin;
        view.setLayoutParams(params);
    }
    private void forceHorizontalMargin(View view, int margin){
        LayoutParams params = (LayoutParams) view.getLayoutParams();
        params.leftMargin = margin;
        params.rightMargin = margin;
        view.setLayoutParams(params);
    }
}
