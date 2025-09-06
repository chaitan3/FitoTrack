/*
 * Copyright (c) 2023 Jannis Scheibe <jannis@tadris.de>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     FitoTrack is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.tadris.fitness.ui.statistics;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

import java.util.ArrayList;
import java.util.List;

import de.tadris.fitness.R;
import de.tadris.fitness.aggregation.AggregationSpan;
import de.tadris.fitness.data.WorkoutType;
import de.tadris.fitness.data.WorkoutTypeManager;
import de.tadris.fitness.stats.NoDataException;
import de.tadris.fitness.stats.StatsDataTypes;
import de.tadris.fitness.stats.StatsProvider;
import de.tadris.fitness.stats.WorkoutProperty;
import de.tadris.fitness.ui.FitoTrackActivity;
import de.tadris.fitness.ui.workout.ShowWorkoutActivity;
import de.tadris.fitness.util.charts.ChartStyles;
import de.tadris.fitness.util.charts.DataSetStyles;

public class DetailStatsActivity extends FitoTrackActivity {

    CombinedChart chart;
    WorkoutTypeManager workoutTypeManager;
    ArrayList<WorkoutType> workoutTypes;
    AggregationSpan aggregationSpan;
    StatsProvider statsProvider;
    float xScale, xTrans;
    WorkoutProperty property;
    boolean summed;
    CandleDataSet currentCandleDataSet = null;
    BarDataSet currentBarDataSet = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics_detail);
        setTitle(getString(R.string.details));
        setupActionBar();

        workoutTypeManager = WorkoutTypeManager.getInstance();
        statsProvider = new StatsProvider(this);
        chart = findViewById(R.id.stats_detail_chart);
        aggregationSpan = (AggregationSpan) getIntent().getSerializableExtra("aggregationSpan");
    }

    @Override
    protected void onStart() {
        super.onStart();
        property = (WorkoutProperty) getIntent().getSerializableExtra("property");
        summed = getIntent().getBooleanExtra("summed", false);
        List<WorkoutType> types = (ArrayList<WorkoutType>) getIntent().getSerializableExtra("types");
        String label = (String) getIntent().getSerializableExtra("ylabel");
        xScale = getIntent().getFloatExtra("xScale", 0);
        xTrans = getIntent().getFloatExtra("xTrans", 0);

        ChartStyles.defaultLineChart(chart, this);
        ChartStyles.setYAxisLabel(chart, label, this);

        // Direct adding of the types from the Intent results in black screen...
        workoutTypes = new ArrayList<>();
        for (WorkoutType workoutType : types) {
            workoutTypes.add(WorkoutTypeManager.getInstance().getWorkoutTypeById(this, workoutType.id));
        }

        chart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartLongPressed(MotionEvent me) {
                Highlight highlight = chart.getHighlightByTouchPoint(me.getX(), me.getY());
                if (highlight != null) {
                    openWorkout(highlight.getX());
                }
            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {
                aggregateChart(chart);
            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {

            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
                aggregateChart(chart);
            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {

            }
        });

        updateChart(workoutTypes);

        chart.zoom(xScale, 1, 0, 0);
        chart.moveViewToX(xTrans);

        ChartStyles.animateChart(chart);
    }

    private void openWorkout(float xPosition) {
        if (currentCandleDataSet != null && aggregationSpan == AggregationSpan.SINGLE) {
            CandleEntry entry = currentCandleDataSet.getEntryForXValue(xPosition, 0);
            StatsDataTypes.DataPoint dataPoint = (StatsDataTypes.DataPoint) entry.getData();

            Intent intent = new Intent(this, ShowWorkoutActivity.class);
            intent.putExtra(ShowWorkoutActivity.WORKOUT_ID_EXTRA, dataPoint.workoutID);
            startActivity(intent);
        }
    }

    private void updateChart(List<WorkoutType> workoutTypes) {

        CombinedData combinedData = new CombinedData();

        String additionalTitle=" - ";
        for (WorkoutType type : workoutTypes) {
            additionalTitle += type.title + ", ";
        }
        additionalTitle = additionalTitle.substring(0, additionalTitle.length()-2);

        // Draw candle charts
        try {
            if (!summed) {
                currentCandleDataSet = statsProvider.getCandleData(aggregationSpan, workoutTypes, property);
                setTitle(currentCandleDataSet.getLabel() + additionalTitle);
                combinedData.setData(new CandleData(currentCandleDataSet));
                // Create background line data
                LineDataSet lineDataSet = StatsProvider.convertCandleToMeanLineData(currentCandleDataSet);
                combinedData.setData(new LineData(DataSetStyles.applyBackgroundLineStyle(this, lineDataSet)));
            } else {
                currentBarDataSet = statsProvider.getSumData(aggregationSpan, workoutTypes, property);
                setTitle(this.getString(R.string.workoutDistanceSum) + additionalTitle);
                BarData barData = new BarData(currentBarDataSet);
                combinedData.setData(barData);
            }
        } catch (NoDataException e) {
        }
        ChartStyles.updateStatsHistoryCombinedChartToSpan(chart, combinedData, aggregationSpan, this);
    }

    /**
     * Combines samples according to the zoom level.
     * @param chart
     */
    private void aggregateChart(BarLineChartBase chart) {
        AggregationSpan newAggSpan = ChartStyles.statsAggregationSpan(chart);
        if (aggregationSpan != newAggSpan) {
            aggregationSpan = newAggSpan;
            updateChart(workoutTypes);
        }
    }
}
