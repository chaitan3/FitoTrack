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
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.ui.Main;
import de.tadris.fitness.util.Util;

public class ScreenElevation extends Screen{
    private ScreenField elevation;
    private LineChart chartElevation;
    private XAxis timeAxis;
    private YAxis elevationAxis;
    private LineDataSet lineDataSet;

    private boolean timeAxisReset = false;
    private boolean elevationAxisReset = false;
    private long lastChartItemTime = 0;

    @Override public @Nullable View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ){
        View rootView = inflater.inflate(R.layout.record_screen_elevation, container, false);
        time = rootView.findViewById(R.id.time);
        time.setText(recordActivity.currentTime);

        elevation = rootView.findViewById(R.id.elevation);
        elevation.setType(ScreenField.Type.ELEVATION);
        elevation.setPadding(0, 0, Main.vw10, 0);

        eleGain = rootView.findViewById(R.id.eleGain);
        eleGain.setType(ScreenField.Type.ELEVATION);
        eleGain.setUnitIcon(R.drawable.ic_uphill);

        eleLoss = rootView.findViewById(R.id.eleLoss);
        eleLoss.setType(ScreenField.Type.ELEVATION);
        eleLoss.setUnitIcon(R.drawable.ic_downhill);

        chartElevation = rootView.findViewById(R.id.chartElevation);
        showElevationChart();

        duration = rootView.findViewById(R.id.duration);
        duration.setType(ScreenField.Type.DURATION);
        duration.setCenter();

        targetProgress = rootView.findViewById(R.id.targetProgress);

        if(Main.isScreenRound){
            duration.setPadding(Main.vw10, 0, Main.vw10, 0);
            ((ConstraintLayout.LayoutParams) chartElevation.getLayoutParams())
                    .setMargins(Main.vw10, 0, Main.vw10, 0);
            duration.setPadding(Main.vw10, 0, Main.vw10, 0);
        }
        return rootView;
    }

    private void showElevationChart(){
        chartElevation.getDescription().setEnabled(false);
        chartElevation.getLegend().setEnabled(false);
        chartElevation.setTouchEnabled(false);

        lineDataSet = new LineDataSet(new ArrayList<>(), "Heart rate");
        lineDataSet.setColor(Screen.colorPrimary);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);
        chartElevation.setData(new LineData(lineDataSet));

        timeAxis = chartElevation.getXAxis();
        timeAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        timeAxis.setTextColor(Screen.colorActive);
        timeAxis.setValueFormatter(Util.timeAxisValueFormatter);
        timeAxis.setLabelCount(5, true);
        timeAxis.setAxisMinimum(0);
        timeAxis.setAxisMaximum(60000);

        elevationAxis = chartElevation.getAxisLeft();
        elevationAxis.setTextColor(Screen.colorActive);
        elevationAxis.setLabelCount(5, true);
        elevationAxis.setAxisMinimum(0);
        elevationAxis.setAxisMaximum(100);

        chartElevation.getAxisRight().setEnabled(false);
    }

    @Override void show(){
        super.show();
        if(!RecordActivity.workoutData.locationSamples.isEmpty())
            RecordActivity.workoutData.locationSamples.forEach(
                    sample->addElevation(sample.time, sample.ele)
            );
        backFill = false;
    }

    @Override void updateLocation(WorkoutData.LocationSample sample){
        elevation.updateValue(sample.ele);
        if(!backFill) addElevation(sample.time, sample.ele);
    }
    private void addElevation(long time, double ele){
        if(time < lastChartItemTime + 4500 ||
                (lastDuration < 60000 && ele == 0.0)//don't trust an elevation of exactly 0.0 in the first minute
        ) return;
        lastChartItemTime = time;

        long duration = recordActivity.timeToDuration(time);
        if(!timeAxisReset && duration >= 60000){
            timeAxisReset = true;
            timeAxis.resetAxisMinimum();
            timeAxis.resetAxisMaximum();
        }
        if(!elevationAxisReset){
            elevationAxisReset = true;
            elevationAxis.resetAxisMinimum();
            elevationAxis.resetAxisMaximum();
        }

        lineDataSet.addEntry(new Entry(duration, (int)ele));
        chartElevation.getData().notifyDataChanged();
        chartElevation.notifyDataSetChanged();
        chartElevation.fitScreen();//repaint
    }
}
