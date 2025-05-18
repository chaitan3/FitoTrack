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
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import de.tadris.fitness.R;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.ui.Main;

public class TypeSelectActivity extends Activity{
    private LinearLayout list;
    private static int _50dp;
    private static int screenTop;
    private static float padPerPixel;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        _50dp = (int) getResources().getDimension(R.dimen._50dp);
        setContentView(R.layout.record_type_select);
        ScrollView scrollView = findViewById(R.id.scrollView);
        list = findViewById(R.id.list);
        for(WorkoutData.WorkoutType workoutType : WorkoutData.WorkoutType.values()){
            addWorkoutType(workoutType);
        }
        if(Main.isScreenRound){
            list.setPadding(Main.vw10, Main.vh25, Main.vw10, Main.vh25);
            list.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
                @Override
                public void onGlobalLayout(){
                    list.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if(screenTop == 0){
                        screenTop = Main.vh50 - _50dp;
                        padPerPixel = (float) Main.vw20 / screenTop;
                    }
                    padItems(0);
                    scrollView.setOnScrollChangeListener((v, sx, sy, ox, oy)->padItems(sy));
                    scrollView.requestFocus();
                }
            });
        }
    }
    private void addWorkoutType(WorkoutData.WorkoutType workoutType){
        TextView textView = new TextView(this);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, _50dp));
        textView.setText(WorkoutData.getWorkoutTypeName(workoutType));
        textView.setCompoundDrawablesWithIntrinsicBounds(WorkoutData.getWorkoutTypeIcon(workoutType), 0, 0, 0);
        textView.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        textView.setLines(1);
        textView.setTextColor(Screen.colorActive);
        textView.setOnClickListener(v->workoutTypeSelected(workoutType));
        list.addView(textView);
    }
    private void workoutTypeSelected(WorkoutData.WorkoutType workoutType){
        RecordActivity.updateWorkoutType(this, workoutType);
        finish();
    }
    private void padItems(int scrollY){
        for(int i = 0; i < list.getChildCount(); i++){
            View view = list.getChildAt(i);
            int top = (int) (view.getY() - scrollY);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
            if(top < screenTop){
                params.setMarginStart((int) ((screenTop - top) * padPerPixel));
            }else if(top > Main.vh50){
                params.setMarginStart((int) ((top - Main.vh50) * padPerPixel));
            }else{
                params.setMarginStart(0);
            }
            view.setLayoutParams(params);
        }
    }
}
