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

package de.tadris.fitness.ui.workouts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import de.tadris.fitness.R;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.data.WorkoutDataStore;
import de.tadris.fitness.ui.Main;

public class WorkoutsActivity extends Activity{
    private ScrollView scrollView;
    private LinearLayout list;
    private static int itemHeight = 0;
    private static float scalePerPixel;
    private static float bottom_quarter;
    private static float below_screen;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.workouts);
        list = findViewById(R.id.list);
        scrollView = findViewById(R.id.scrollView);
        ((LinearLayout.LayoutParams) list.getLayoutParams()).bottomMargin += Main.old_vh25;
        findViewById(R.id.label).setMinimumHeight(Main.old_vh25);
        if(!WorkoutDataStore.workouts.isEmpty()) loadItems();
        Main.runInBackgroundJAVA(()->WorkoutDataStore.readWorkouts(this));
    }
    @Override
    public void onResume(){
        list.removeAllViews();
        loadItems();
        if(Main.isScreenRound){
            scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
                @Override
                public void onGlobalLayout(){
                    scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    scaleItems(scrollView.getScrollY());
                }
            });
        }
        super.onResume();
    }
    public void loadItems(){//Thread: BG
        runOnUiThread(()->{
            list.removeAllViews();
            for(int i=0; i < WorkoutDataStore.workouts.size(); i++){
                WorkoutData workoutData = WorkoutDataStore.workouts.get(i);
                WorkoutsItem workoutsItem = new WorkoutsItem(this, workoutData);
                workoutsItem.setOnClickListener(v->itemClicked(workoutData.id));
                list.addView(workoutsItem);
            }
            if(Main.isScreenRound){
                list.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
                    @Override
                    public void onGlobalLayout(){
                        list.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        if(itemHeight == 0 && list.getChildCount() > 0){
                            itemHeight = list.getChildAt(0).getHeight();
                            bottom_quarter = Main.old_vh75 - itemHeight;
                            below_screen = Main.heightPixels - itemHeight;
                            scalePerPixel = 0.2f / Main.old_vh25;
                        }
                        scaleItems(0);
                    }
                });
                scrollView.setOnScrollChangeListener((v, sx, sy, osx, osy)->scaleItems(sy));
            }
            scrollView.requestFocus();
        });
    }
    private void itemClicked(long id){
        Intent intent = new Intent(this, WorkoutActivity.class);
        intent.putExtra("workoutDataId", id);
        startActivity(intent);
    }

    private void scaleItems(int scrollY){
        float top;
        float scale;
        for(int i = 0; i < list.getChildCount(); i++){
            View view = list.getChildAt(i);

            top = Main.old_vh25 + (view.getY() - scrollY);//add vh25 which is the height of the label
            scale = 1.0f;
            if(top < 0){
                //the item is above the screen
                scale = 0.8f;
            }else if(top < Main.old_vh25){
                //the item is in the top quarter
                scale = 0.8f + (scalePerPixel * top);
            }else if(top > below_screen){
                //the item is below the screen
                scale = 0.8f;
            }else if(top > bottom_quarter){
                //the item is in the bottom quarter
                scale = 1.0f - (scalePerPixel * (top - bottom_quarter));
            }
            view.setScaleX(scale);
            view.setScaleY(scale);
        }
    }
}
