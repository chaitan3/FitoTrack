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
import androidx.constraintlayout.widget.ConstraintLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

import de.tadris.fitness.R;
import de.tadris.fitness.ui.Main;
import de.tadris.fitness.util.Util;

public class ScreenSpeed extends Screen{
    private LineChart chartSpeed;
    private XAxis timeAxis;
    private YAxis speedAxis;
    private LineDataSet lineDataSet;

    private boolean timeAxisReset = false;
    private float speedAxisMaximum = 3;
    private long lastChartItemTime = 0;

    @Override public @Nullable View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ){
        View rootView = inflater.inflate(R.layout.record_screen_speed, container, false);
        time = rootView.findViewById(R.id.time);
        time.setText(recordActivity.currentTime);

        speed = rootView.findViewById(R.id.speed);
        speed.setType(ScreenField.Type.SPEED_KMH);
        speed.setPadding(0, 0, Main.vw10, 0);

        chartSpeed = rootView.findViewById(R.id.chartSpeed);
        showSpeedChart();

        duration = rootView.findViewById(R.id.duration);
        duration.setType(ScreenField.Type.DURATION);
        duration.setCenter();
        if(Main.isScreenRound){
            duration.setPadding(Main.vw10, 0, Main.vw10, 0);
            ((ConstraintLayout.LayoutParams) chartSpeed.getLayoutParams())
                    .setMargins(Main.vw10, 0, Main.vw10, 0);
        }

        targetProgress = rootView.findViewById(R.id.targetProgress);

        return rootView;
    }

    private void showSpeedChart(){
        chartSpeed.getDescription().setEnabled(false);
        chartSpeed.getLegend().setEnabled(false);
        chartSpeed.setTouchEnabled(false);

        lineDataSet = new LineDataSet(new ArrayList<>(), "Heart rate");
        lineDataSet.setColor(Screen.colorPrimary);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);
        chartSpeed.setData(new LineData(lineDataSet));

        timeAxis = chartSpeed.getXAxis();
        timeAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        timeAxis.setTextColor(Screen.colorActive);
        timeAxis.setValueFormatter(Util.timeAxisValueFormatter);
        timeAxis.setLabelCount(4, true);
        timeAxis.setAxisMinimum(0);
        timeAxis.setAxisMaximum(60000);

        speedAxis = chartSpeed.getAxisLeft();
        speedAxis.setTextColor(Screen.colorActive);
        timeAxis.setLabelCount(5, true);
        speedAxis.setAxisMinimum(0);
        speedAxis.setAxisMaximum(5);

        chartSpeed.getAxisRight().setEnabled(false);
    }

    @Override void show(){
        super.show();
        if(!RecordActivity.workoutData.locationSamples.isEmpty())
            RecordActivity.workoutData.locationSamples.forEach(sample->addSpeed(sample.time, sample.speed));
        backFill = false;
    }

    @Override void updateSpeed(long time, double speedMS){
        super.updateSpeed(time, speedMS);
        if(!backFill) addSpeed(time, speedMS);
    }
    private void addSpeed(long time, double speedMS){
        if(time < lastChartItemTime + 4500) return;
        lastChartItemTime = time;

        float speedKMH = (float)(speedMS * 3.6);//meters/second > kmh
        if(speedKMH > speedAxisMaximum){
            speedAxisMaximum = speedKMH;
            speedAxis.setAxisMaximum(speedAxisMaximum*1.1F);
        }
        long duration = recordActivity.timeToDuration(time);
        if(!timeAxisReset && duration >= 60000){
            timeAxisReset = true;
            timeAxis.resetAxisMinimum();
            timeAxis.resetAxisMaximum();
        }
        lineDataSet.addEntry(new Entry(duration, speedKMH));
        if(lineDataSet.getEntryCount() > Main.vw80) lineDataSet.removeFirst();
        chartSpeed.getData().notifyDataChanged();
        chartSpeed.notifyDataSetChanged();
        chartSpeed.fitScreen();//repaint
    }
}
