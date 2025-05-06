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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.health.services.client.ExerciseUpdateCallback;
import androidx.health.services.client.data.Availability;
import androidx.health.services.client.data.DataPoint;
import androidx.health.services.client.data.DataPointContainer;
import androidx.health.services.client.data.DataType;
import androidx.health.services.client.data.ExerciseEvent;
import androidx.health.services.client.data.ExerciseLapSummary;
import androidx.health.services.client.data.ExerciseUpdate;
import androidx.health.services.client.data.SampleDataPoint;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import de.tadris.fitness.R;
import de.tadris.fitness.ui.Main;
import de.tadris.fitness.util.Util;

public class Cooldown extends Fragment{
    private TextView time;
    private ScreenField duration;
    private ScreenField heartRate;
    private LineChart chartHeartRate;

    private RecordActivity recordActivity;
    private final SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    private Handler handler;
    private long start = 0;
    private LineDataSet lineDataSet;
    private XAxis timeAxis;
    private YAxis heartRateAxis;
    private float heartRateAxisMinimum = 100;
    private float heartRateAxisMaximum = 0;

    @Override public void onAttach(@NonNull Context context){
        super.onAttach(context);
        try{
            recordActivity = (RecordActivity) context;
        }catch(ClassCastException e){
            Log.e(Main.LOG_TAG, "Cooldown.onAttach exception: " + e.getMessage());
        }
    }
    @Override public @Nullable View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ){
        View rootView = inflater.inflate(R.layout.record_cooldown, container, false);
        recordActivity.forceScreenOn();
        time = rootView.findViewById(R.id.time);
        duration = rootView.findViewById(R.id.duration);
        heartRate = rootView.findViewById(R.id.heartRate);
        chartHeartRate = rootView.findViewById(R.id.chartHeartRate);

        rootView.findViewById(R.id.end).setOnClickListener(v->{
            handler.removeCallbacksAndMessages(null);
            recordActivity.showSummary();
        });

        handler = new Handler(Looper.getMainLooper());
        updateTime.run();
        duration.setType(ScreenField.Type.DURATION);
        start = SystemClock.elapsedRealtime();
        updateDuration.run();

        if(!RecordActivity.hasHRPermission){
            heartRate.setVisibility(TextView.GONE);
            chartHeartRate.setVisibility(TextView.GONE);
            return rootView;
        }

        heartRate.setType(ScreenField.Type.HEART_RATE_BPM);
        showHeartRateChart();

        if(Screen.lastHeartRate != null){
            heartRate.updateValue(Screen.lastHeartRate.heartRate);
            addHeartRate(0, Screen.lastHeartRate.heartRate);
        }

        recordActivity.exerciseClient.setUpdateCallback(exerciseUpdateCallback);

        if(Main.isScreenRound){
            duration.setPadding(Main.vw10, 0, 0, 0);
            heartRate.setPadding(0, 0, Main.vw10, 0);
            ((ConstraintLayout.LayoutParams) chartHeartRate.getLayoutParams())
                    .setMargins(Main._10dp, 0, Main.vw10, 0);
        }
        return rootView;
    }

    private final Runnable updateTime = new Runnable(){@Override public void run(){
        long current = System.currentTimeMillis();
        time.setText(sdfTime.format(current));
        handler.postDelayed(updateTime, 60000-(current%60000));
    }};
    private final Runnable updateDuration = new Runnable(){@Override public void run(){
        long value = SystemClock.elapsedRealtime() - start;
        duration.updateValue(value);
        handler.postDelayed(updateDuration, 1000-(value%1000));
    }};
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

    private void addHeartRate(long duration, int heartRate){
        if(heartRate < heartRateAxisMinimum){
            heartRateAxisMinimum = heartRate;
            heartRateAxis.setAxisMinimum(heartRateAxisMinimum-5);
        }
        if(heartRate > heartRateAxisMaximum){
            heartRateAxisMaximum = heartRate;
            heartRateAxis.setAxisMaximum(heartRateAxisMaximum+5);
        }
        if(duration >= 60000) timeAxis.resetAxisMaximum();

        lineDataSet.addEntry(new Entry(duration, heartRate));
        chartHeartRate.getData().notifyDataChanged();
        chartHeartRate.notifyDataSetChanged();
        chartHeartRate.fitScreen();//repaint
    }

    private final ExerciseUpdateCallback exerciseUpdateCallback = new ExerciseUpdateCallback(){
        @Override public void onRegistrationFailed(@NonNull Throwable throwable){
            Log.e(Main.LOG_TAG, "Cooldown.ExerciseUpdateCallback.onRegistrationFailed: " + throwable);
        }
        @Override public void onRegistered(){}
        @Override public void onLapSummaryReceived(@NonNull ExerciseLapSummary exerciseLapSummary){}
        @SuppressWarnings("unchecked") @Override public void onExerciseUpdateReceived(@NonNull ExerciseUpdate exerciseUpdate){
            DataPointContainer dataPointContainer = exerciseUpdate.getLatestMetrics();
            for(SampleDataPoint<?> dataPoint : dataPointContainer.getSampleDataPoints()){
                DataType<?, ? extends DataPoint<?>> dataType = dataPoint.getDataType();
                if(dataType.equals(DataType.HEART_RATE_BPM)){
                    SampleDataPoint<Double> dataPointHR = (SampleDataPoint<Double>) dataPoint;
                    int value = dataPointHR.getValue().intValue();
                    heartRate.updateValue(value);
                    addHeartRate(dataPointHR.getTimeDurationFromBoot().toMillis()-start, value);
                }
            }
        }
        @Override public void onExerciseEventReceived(@NonNull ExerciseEvent event){ExerciseUpdateCallback.super.onExerciseEventReceived(event);}
        @Override public void onAvailabilityChanged(@NonNull DataType<?, ?> dataType, @NonNull Availability availability){}
    };
}
