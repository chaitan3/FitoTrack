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
import de.tadris.fitness.data.WorkoutData.HeartRateSample;
import de.tadris.fitness.ui.Main;
import de.tadris.fitness.util.Util;

public class ScreenHeartRate extends Screen{
    private LineChart chartHeartRate;
    private XAxis timeAxis;
    private YAxis heartRateAxis;
    private LineDataSet lineDataSet;

    private boolean timeAxisReset = false;
    private float heartRateAxisMinimum = 100;
    private float heartRateAxisMaximum = 0;
    private long lastChartItemTime = 0;

    @Override public @Nullable View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ){
        View rootView = inflater.inflate(R.layout.record_screen_heart_rate, container, false);

        time = rootView.findViewById(R.id.time);
        time.setText(recordActivity.currentTime);

        heartRate = rootView.findViewById(R.id.heartRate);
        heartRate.setType(ScreenField.Type.HEART_RATE_BPM);
        heartRate.setPadding(0, 0, Main.vw10, 0);

        chartHeartRate = rootView.findViewById(R.id.chartHeartRate);
        showHeartRateChart();

        duration = rootView.findViewById(R.id.duration);
        duration.setType(ScreenField.Type.DURATION);
        duration.setCenter();
        if(Main.isScreenRound){
            duration.setPadding(Main.vw10, 0, Main.vw10, 0);
            ((ConstraintLayout.LayoutParams) chartHeartRate.getLayoutParams())
                    .setMargins(Main._10dp, 0, Main.vw10, 0);
        }

        targetProgress = rootView.findViewById(R.id.targetProgress);

        return rootView;
    }

    private void showHeartRateChart(){
        chartHeartRate.getDescription().setEnabled(false);
        chartHeartRate.getLegend().setEnabled(false);
        chartHeartRate.setTouchEnabled(false);

        lineDataSet = new LineDataSet(new ArrayList<>(), "Heart rate");
        lineDataSet.setColor(Screen.colorPrimary);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);
        chartHeartRate.setData(new LineData(lineDataSet));

        timeAxis = chartHeartRate.getXAxis();
        timeAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        timeAxis.setTextColor(Screen.colorActive);
        timeAxis.setValueFormatter(Util.timeAxisValueFormatter);
        timeAxis.setLabelCount(4, true);
        timeAxis.setAxisMinimum(0);
        timeAxis.setAxisMaximum(60000);

        heartRateAxis = chartHeartRate.getAxisLeft();
        heartRateAxis.setTextColor(Screen.colorActive);
        heartRateAxis.setAxisMinimum(50);
        heartRateAxis.setAxisMaximum(100);

        chartHeartRate.getAxisRight().setEnabled(false);
    }

    @Override void show(){
        super.show();
        if(!RecordActivity.workoutData.heartRateSamples.isEmpty())
            RecordActivity.workoutData.heartRateSamples.forEach(this::addHeartRate);
        backFill = false;
    }

    @Override void updateHeartRate(HeartRateSample sample, boolean isPaused){
        super.updateHeartRate(sample, isPaused);
        if(!isPaused && !backFill) addHeartRate(sample);
    }
    private void addHeartRate(HeartRateSample sample){
        if(sample.time < lastChartItemTime + 4500) return;
        lastChartItemTime = sample.time;

        if(sample.heartRate < heartRateAxisMinimum){
            heartRateAxisMinimum = sample.heartRate;
            heartRateAxis.setAxisMinimum(heartRateAxisMinimum-5);
        }
        if(sample.heartRate > heartRateAxisMaximum){
            heartRateAxisMaximum = sample.heartRate;
            heartRateAxis.setAxisMaximum(heartRateAxisMaximum+5);
        }
        long duration = recordActivity.timeToDuration(sample.time);
        if(!timeAxisReset && duration >= 60000){
            timeAxisReset = true;
            timeAxis.resetAxisMinimum();
            timeAxis.resetAxisMaximum();
        }
        lineDataSet.addEntry(new Entry(duration, sample.heartRate));
        if(lineDataSet.getEntryCount() > Main.vw80) lineDataSet.removeFirst();
        chartHeartRate.getData().notifyDataChanged();
        chartHeartRate.notifyDataSetChanged();
        chartHeartRate.fitScreen();//repaint
    }
}
