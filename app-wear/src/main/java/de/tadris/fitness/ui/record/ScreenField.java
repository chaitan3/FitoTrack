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

import static de.tadris.fitness.util.Util.distanceDoubleToString;
import static de.tadris.fitness.util.Util.prettyDuration;
import static de.tadris.fitness.util.Util.speedDoubleToString;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;

import de.tadris.fitness.R;
import de.tadris.fitness.ui.Main;

public class ScreenField extends ConstraintLayout{
    public enum Type{UNKNOWN, CALORIES, DISTANCE_KM, DURATION, ELEVATION, HEART_RATE_BPM, PAUSE, SPEED_KMH, SPEED_MS}
    private Type type = Type.UNKNOWN;

    private final TextView value;
    private final TextView unit;

    public ScreenField(Context context, AttributeSet attrs){
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.record_screen_field, this, true);
        value = findViewById(R.id.value);
        unit = findViewById(R.id.unit);
    }
    void setType(Type type){
        this.type = type;
        switch(type){
            case CALORIES:
                unit.setText(R.string.kcal);
                break;
            case DURATION, PAUSE:
                unit.setVisibility(TextView.GONE);
                value.setText(prettyDuration(0));
                break;
            case ELEVATION:
                unit.setText(R.string.m);
                break;
            case SPEED_KMH:
                unit.setText(R.string.kmh);
                break;
            case SPEED_MS:
                unit.setText(R.string.ms);
                break;
            case HEART_RATE_BPM:
                unit.setPadding(5, 5, 5, 5);
                Drawable drawable = AppCompatResources.getDrawable(getContext(), R.drawable.ic_heart_fixed);
                if(drawable == null) break;
                drawable.setBounds(0, 0, unit.getLineHeight(), unit.getLineHeight());
                unit.setCompoundDrawables(drawable, null, null, null);
                break;
        }
    }
    void setUnitAverage(){
        String avg = getContext().getString(R.string.avg_symbol);
        avg += unit.getText();
        unit.setText(avg);
    }
    void setUnitIcon(int icon){
        Drawable drawable = AppCompatResources.getDrawable(getContext(), icon);
        if(drawable == null) return;
        drawable.setBounds(0, 5, Main._10dp, drawable.getIntrinsicHeight()-10);
        unit.setCompoundDrawables(drawable, null, null, null);
    }
    void setActive(){
        value.setTextColor(Screen.colorActive);
        unit.setTextColor(Screen.colorActive);
    }
    void setInactive(){
        value.setTextColor(Screen.colorInactive);
        unit.setTextColor(Screen.colorInactive);
    }
    void setCenter(){value.setGravity(Gravity.CENTER);}
    void updateValue(long longValue){value.setText(prettyDuration(longValue));}
    void updateValue(int intValue){value.setText(String.valueOf(intValue));}
    void updateValue(Double doubleValue){
        switch(type){
            case CALORIES, ELEVATION:
                value.setText(String.valueOf(doubleValue.intValue()));
                break;
            case DISTANCE_KM:
                if(doubleValue > 1000){
                    doubleValue /= 1000;//meters > kilometers
                    value.setText(distanceDoubleToString(doubleValue));
                    unit.setText(R.string.km);
                }else{
                    value.setText(String.valueOf(doubleValue.intValue()));
                    unit.setText(R.string.m);
                }
                break;
            case SPEED_KMH:
                doubleValue *= 3.6;//meters/second > kmh
            case SPEED_MS:
                value.setText(speedDoubleToString(doubleValue));
                break;
        }
    }
}
