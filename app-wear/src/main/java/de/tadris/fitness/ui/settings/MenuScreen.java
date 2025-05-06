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
package de.tadris.fitness.ui.settings;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import de.tadris.fitness.R;
import de.tadris.fitness.ui.Main;

public class MenuScreen extends Activity{
    private ScrollView scrollView;
    protected LinearLayout list;
    TextView label;
    private static int itemHeight = 0;
    private static float scalePerPixel;
    private static float bottom_quarter;
    private static float below_screen;

    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_screen);
        list = findViewById(R.id.list);
        label = findViewById(R.id.label);

        scrollView = findViewById(R.id.scrollView);
        if(Main.isScreenRound){
            label.setPadding(Main.vw10, Main.vw10, Main.vw10, 0);
            list.setPadding(0, 0, 0, Main.vh25);
            list.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
                @Override public void onGlobalLayout(){
                    list.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if(itemHeight == 0 && list.getChildCount() > 1){
                        itemHeight = list.getChildAt(1).getHeight();
                        bottom_quarter = Main.vh75 - itemHeight;
                        below_screen = Main.heightPixels - itemHeight;
                        scalePerPixel = 0.2f / Main.vh25;
                    }
                    scaleItems(0);
                }
            });
            scrollView.setOnScrollChangeListener((v, sx, sy, osx, osy)->scaleItems(sy));
        }
    }
    @Override public void onResume(){
        super.onResume();
        scrollView.requestFocus();
    }

    private void scaleItems(int scrollY){
        float top;
        float scale;
        for(int i = 1; i < list.getChildCount(); i++){
            View view = list.getChildAt(i);

            top = view.getY() - scrollY;
            scale = 1.0f;
            if(top < 0){
                //the item is above the screen
                scale = 0.8f;
            }else if(top < Main.vh25){
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
