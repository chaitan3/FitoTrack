/*
 * Copyright (c) 2022 Jannis Scheibe <jannis@tadris.de>
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

package de.tadris.fitness.ui.statistics.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.renderer.CombinedChartRenderer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.tadris.fitness.R;
import de.tadris.fitness.aggregation.AggregationSpan;
import de.tadris.fitness.data.StatsDataProvider;
import de.tadris.fitness.data.StatsDataTypes;
import de.tadris.fitness.data.StatsProvider;
import de.tadris.fitness.data.WorkoutType;
import de.tadris.fitness.data.WorkoutTypeManager;
import de.tadris.fitness.data.preferences.UserPreferences;
import de.tadris.fitness.ui.FitoTrackActivity;
import de.tadris.fitness.ui.statistics.DetailStatsActivity;
import de.tadris.fitness.ui.statistics.TextToggle;
import de.tadris.fitness.ui.statistics.WorkoutTypeSelection;
import de.tadris.fitness.util.WorkoutProperty;
import de.tadris.fitness.util.charts.ChartStyles;
import de.tadris.fitness.util.charts.DataSetStyles;
import de.tadris.fitness.util.exceptions.NoDataException;
import de.tadris.fitness.util.statistics.ChartSynchronizer;
import de.tadris.fitness.util.statistics.OnChartGestureMultiListener;

public class StatsHistoryFragment extends StatsFragment {
    FitoTrackActivity activity;

    TextToggle speedTitle;
    CombinedChart speedChart;

    TextToggle distanceTitle;
    CombinedChart distanceChart;

    TextToggle durationTitle;
    CombinedChart durationChart;

    Spinner exploreTitle;
    Switch exploreChartSwitch;
    CombinedChart exploreChart;

    WorkoutTypeSelection selection;

    ChartSynchronizer synchronizer;

    StatsProvider statsProvider;
    ArrayList<CombinedChart> combinedChartList = new ArrayList<>();

    AggregationSpan aggregationSpan = AggregationSpan.YEAR;

    UserPreferences preferences;

    public StatsHistoryFragment(FitoTrackActivity ctx) {
        super(R.layout.fragment_stats_history, ctx);
        synchronizer = new ChartSynchronizer();
        statsProvider = new StatsProvider(ctx);
        preferences = new UserPreferences(ctx);
        activity = ctx;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Register WorkoutType selection listeners
        selection = view.findViewById(R.id.stats_history_workout_type_selector);
        ((TextView) selection.findViewById(R.id.view_workout_type_selection_text)).setTextColor(getResources().getColor(R.color.textDarkerWhite));
        selection.addOnWorkoutTypeSelectListener(workoutType -> updateCharts(selection.getSelectedWorkoutTypes()));
        selection.addOnWorkoutTypeSelectListener(workoutType -> preferences.setStatisticsSelectedTypes(selection.getSelectedWorkoutTypes()));

        // Setup switch functionality
        speedTitle = view.findViewById(R.id.stats_history_speed_toggle);
        speedTitle.setOnToggleListener(current -> updateSpeedChart(selection.getSelectedWorkoutTypes()));

        speedChart = view.findViewById(R.id.stats_speed_chart);
        speedChart.setDoubleTapToZoomEnabled(false);


        distanceTitle = view.findViewById(R.id.stats_history_distance_toggle);
        distanceTitle.setOnToggleListener(current -> updateDistanceChart(selection.getSelectedWorkoutTypes()));

        distanceChart = view.findViewById(R.id.stats_history_distance_chart);
        distanceChart.setDoubleTapToZoomEnabled(false);


        durationTitle = view.findViewById(R.id.stats_history_duration_toggle);
        durationTitle.setOnToggleListener(current -> updateDurationChart(selection.getSelectedWorkoutTypes()));

        durationChart = view.findViewById(R.id.stats_duration_chart);
        durationChart.setDoubleTapToZoomEnabled(false);


        exploreTitle = view.findViewById(R.id.stats_explore_title);
        // Register WorkoutType selection listeners
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), R.layout.stats_title, WorkoutProperty.getStringRepresentations(getContext()));
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        exploreTitle.setAdapter(spinnerAdapter);
        exploreTitle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                WorkoutProperty property = WorkoutProperty.getById(exploreTitle.getSelectedItemPosition());
                if (!property.summable()) {
                    exploreChartSwitch.setChecked(false);
                    exploreChartSwitch.setEnabled(false);
                    exploreChartSwitch.setVisibility(View.GONE);
                } else {
                    exploreChartSwitch.setEnabled(true);
                    exploreChartSwitch.setVisibility(View.VISIBLE);
                }
                updateExploreChart(selection.getSelectedWorkoutTypes());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        exploreChart = view.findViewById(R.id.stats_explore_chart);
        exploreChart.setDoubleTapToZoomEnabled(false);
        exploreChartSwitch = view.findViewById(R.id.stats_explore_switch);
        exploreChartSwitch.setOnClickListener(view1 -> updateExploreChart(selection.getSelectedWorkoutTypes()));


        combinedChartList.add(speedChart);
        combinedChartList.add(distanceChart);
        combinedChartList.add(durationChart);
        combinedChartList.add(exploreChart);

        for (CombinedChart combinedChart : combinedChartList) {
            ChartStyles.animateChart(combinedChart);
            ChartStyles.fixViewPortOffsets(combinedChart, 120);
            ChartStyles.defaultLineChart(combinedChart, activity);
            statsProvider.setAxisLimits(combinedChart.getXAxis(), WorkoutProperty.TOP_SPEED);
            OnChartGestureMultiListener multiListener = new OnChartGestureMultiListener(new ArrayList<>());
            multiListener.listeners.add(synchronizer.addChart(combinedChart));
            multiListener.listeners.add(new OnChartGestureListener() {
                @Override
                public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

                }

                @Override
                public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                    if(lastPerformedGesture == ChartTouchListener.ChartGesture.LONG_PRESS) {
                        float[] viewPortValues = new float[9];
                        combinedChart.getViewPortHandler().getMatrixTouch().getValues(viewPortValues);

                        Intent i = new Intent(context, DetailStatsActivity.class);
                        WorkoutProperty workoutProperty;
                        boolean summed = false;
                        switch (combinedChartList.indexOf(combinedChart)) {
                            case 0:
                                workoutProperty = speedTitle.isSwapped() ?
                                        WorkoutProperty.AVG_PACE :
                                        WorkoutProperty.AVG_SPEED;
                                break;
                            case 1:
                                workoutProperty = WorkoutProperty.LENGTH;
                                summed = distanceTitle.isSwapped();
                                break;
                            case 2:
                                workoutProperty = WorkoutProperty.DURATION;
                                summed = durationTitle.isSwapped();
                                break;
                            case 3:
                            default:
                                workoutProperty = WorkoutProperty.getById(exploreTitle.getSelectedItemPosition());
                                summed = exploreChartSwitch.isChecked();
                        }
                        i.putExtra("property", (Serializable) workoutProperty);
                        i.putExtra("summed", summed);
                        i.putExtra("types", (Serializable) selection.getSelectedWorkoutTypes());
                        i.putExtra("formatter", combinedChart.getAxisLeft().getValueFormatter().getClass());
                        //i.putExtra("viewPort", viewPortValues);
                        i.putExtra("xScale", combinedChart.getScaleX());
                        i.putExtra("xTrans", combinedChart.getLowestVisibleX());
                        i.putExtra("aggregationSpan", aggregationSpan);
                        String label = "";
                        if (combinedChart.getLegend().getEntries().length > 0)
                            label = combinedChart.getLegend().getEntries()[0].label;
                        i.putExtra("ylabel", label);
                        context.startActivity(i);
                    }
                }

                @Override
                public void onChartLongPressed(MotionEvent me) { // function implemented in onChartGestureListenerEnd to avoid call on zoom / pan etc
                }

                @Override
                public void onChartDoubleTapped(MotionEvent me) {
                }

                @Override
                public void onChartSingleTapped(MotionEvent me) {
                }

                @Override
                public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

                }

                @Override
                public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
                    scaleChart(combinedChart);
                }

                @Override
                public void onChartTranslate(MotionEvent me, float dX, float dY) {

                }
            });
            combinedChart.setOnChartGestureListener(multiListener);
        }

        List<WorkoutType> selected = preferences.getStatisticsSelectedTypes();
        if (selected.size() == 0 || selected.get(0) == null) {
            selected.clear();
            selected.addAll(WorkoutTypeManager.getInstance().getAllTypes(context));
        }
        selection.setSelectedWorkoutTypes(selected);

        displaySpan(preferences.getStatisticsAggregationSpan()); // set viewport according to other statistic views

        exploreTitle.setSelection(WorkoutProperty.PAUSE_DURATION.getId());
        distanceTitle.toggle();
        durationTitle.toggle();
    }

    private void scaleChart(CombinedChart chart) {
        AggregationSpan newAggSpan = ChartStyles.statsAggregationSpan(chart);
        if (aggregationSpan != newAggSpan) {
            aggregationSpan = newAggSpan;
            updateCharts(selection.getSelectedWorkoutTypes());
        }
    }

    private void displaySpan(AggregationSpan span) {
        // set span for aggregation -> one smaller
        if (span == AggregationSpan.ALL) {
            aggregationSpan = AggregationSpan.YEAR;
        } else if (span == AggregationSpan.YEAR) {
            aggregationSpan = AggregationSpan.MONTH;
        } else if (span == AggregationSpan.MONTH) {
            aggregationSpan = AggregationSpan.WEEK;
        } else if (span == AggregationSpan.WEEK) {
            aggregationSpan = AggregationSpan.SINGLE;
        }
        updateCharts(selection.getSelectedWorkoutTypes());

        // set view port
        final StatsDataProvider dataProvider = new StatsDataProvider(context);
        final ArrayList<StatsDataTypes.DataPoint> data = dataProvider.getData(WorkoutProperty.LENGTH, selection.getSelectedWorkoutTypes());
        if (data.size() > 0) {
            final StatsDataTypes.DataPoint firstEntry = data.get(0);
            final StatsDataTypes.DataPoint lastEntry = data.get(data.size() - 1);
            final long leftTime = lastEntry.time - span.spanInterval;
            final float zoom = (float) (lastEntry.time- firstEntry.time) / span.spanInterval;
            for (CombinedChart chart:combinedChartList) { // need to iterate cause code zoom doesn't trigger GestureListeners
                chart.zoom(zoom, 1, 0, 0);
                chart.moveViewToX(leftTime);
            }
        }
    }

    private void updateCharts(List<WorkoutType> workoutTypes) {
        updateSpeedChart(workoutTypes);
        updateDurationChart(workoutTypes);
        updateExploreChart(workoutTypes);
        updateDistanceChart(workoutTypes);
    }

    private void updateSpeedChart(List<WorkoutType> workoutTypes) {
        CandleDataSet candleDataSet;

        WorkoutProperty property = speedTitle.isSwapped() ?
                WorkoutProperty.AVG_PACE :
                WorkoutProperty.AVG_SPEED;

        try {
            // Retrieve candle data
            candleDataSet = statsProvider.getCandleData(aggregationSpan, workoutTypes, property);
            ChartStyles.setYAxisLabel(speedChart, property.getUnit(context, candleDataSet.getYMax()), context);

            // Add candle data
            CombinedData combinedData = new CombinedData();
            combinedData.setData(new CandleData(candleDataSet));

            // Create background line
            LineDataSet lineDataSet = StatsProvider.convertCandleToMeanLineData(candleDataSet);
            combinedData.setData(new LineData(DataSetStyles.applyBackgroundLineStyle(context, lineDataSet)));

            ChartStyles.updateStatsHistoryCombinedChartToSpan(speedChart, combinedData, aggregationSpan, activity);
        } catch (NoDataException e) {
            speedChart.clear();
        }
        speedChart.invalidate();
    }

    private void updateDistanceChart(List<WorkoutType> workoutTypes) {
        CombinedData combinedData = new CombinedData();

        try {
            if (distanceTitle.isSwapped()) {
                BarDataSet barDataSet = statsProvider.getSumData(aggregationSpan, workoutTypes, WorkoutProperty.LENGTH);
                BarData barData = new BarData(barDataSet);
                combinedData.setData(barData);
            } else {
                CandleDataSet candleDataSet = statsProvider.getCandleData(aggregationSpan, workoutTypes, WorkoutProperty.LENGTH);
                combinedData.setData(new CandleData(candleDataSet));
                // Create background line
                LineDataSet lineDataSet = StatsProvider.convertCandleToMeanLineData(candleDataSet);
                combinedData.setData(new LineData(DataSetStyles.applyBackgroundLineStyle(context, lineDataSet)));
            }

            // It is very dumb but CombinedChart.setData() calls the initBuffer method of all renderer before resetting the renderer (because the super call is executed before).
            // In case a bar chart was displayed before but not longer, the activity would crash.
            // Therefore the following two lines resets all renderers manually.
            distanceChart.clear();
            ((CombinedChartRenderer) distanceChart.getRenderer()).createRenderers();
            ChartStyles.updateStatsHistoryCombinedChartToSpan(distanceChart, combinedData, aggregationSpan, activity);
            ChartStyles.setYAxisLabel(distanceChart, WorkoutProperty.LENGTH.getUnit(context, combinedData.getYMax()), context);
        } catch (NoDataException e) {
            distanceChart.clear();
        }
        distanceChart.invalidate();
    }

    private void updateDurationChart(List<WorkoutType> workoutTypes) {
        CombinedData combinedData = new CombinedData();

        try {
            if (durationTitle.isSwapped()) {
                BarDataSet barDataSet = statsProvider.getSumData(aggregationSpan, workoutTypes, WorkoutProperty.DURATION);
                BarData barData = new BarData(barDataSet);
                combinedData.setData(barData);
            } else {
                CandleDataSet candleDataSet = statsProvider.getCandleData(aggregationSpan, workoutTypes, WorkoutProperty.DURATION);
                combinedData.setData(new CandleData(candleDataSet));
                // Create background line
                LineDataSet lineDataSet = StatsProvider.convertCandleToMeanLineData(candleDataSet);
                combinedData.setData(new LineData(DataSetStyles.applyBackgroundLineStyle(context, lineDataSet)));
            }

            // It is very dumb but CombinedChart.setData() calls the initBuffer method of all renderer before resetting the renderer (because the super call is executed before).
            // In case a bar chart was displayed before but not longer, the activity would crash.
            // Therefore the following two lines resets all renderers manually.
            durationChart.clear();
            ((CombinedChartRenderer) durationChart.getRenderer()).createRenderers();
            ChartStyles.updateStatsHistoryCombinedChartToSpan(durationChart, combinedData, aggregationSpan, activity);
            ChartStyles.setYAxisLabel(durationChart, WorkoutProperty.DURATION.getUnit(context, combinedData.getYMax()), context);
        } catch (NoDataException e) {
            durationChart.clear();
        }
        durationChart.invalidate();
    }

    private void updateExploreChart(List<WorkoutType> workoutTypes) {
        WorkoutProperty property = WorkoutProperty.getById(exploreTitle.getSelectedItemPosition());
        CombinedData combinedData = new CombinedData();

        try {
            if (exploreChartSwitch.isChecked()) {
                BarDataSet barDataSet = statsProvider.getSumData(aggregationSpan, workoutTypes, property);
                BarData barData = new BarData(barDataSet);
                combinedData.setData(barData);
            } else {
                CandleDataSet candleDataSet = statsProvider.getCandleData(aggregationSpan, workoutTypes, property);
                combinedData.setData(new CandleData(candleDataSet));
                // Create background line
                LineDataSet lineDataSet = StatsProvider.convertCandleToMeanLineData(candleDataSet);
                combinedData.setData(new LineData(DataSetStyles.applyBackgroundLineStyle(context, lineDataSet)));
            }

            // It is very dumb but CombinedChart.setData() calls the initBuffer method of all renderer before resetting the renderer (because the super call is executed before).
            // In case a bar chart was displayed before but not longer, the activity would crash.
            // Therefore the following two lines resets all renderers manually.
            exploreChart.clear();
            ((CombinedChartRenderer) exploreChart.getRenderer()).createRenderers();
            if(!(property == WorkoutProperty.START) && !(property == WorkoutProperty.END)) {
                String unit = property.getUnit(getContext(), combinedData.getYMax());
                ChartStyles.setYAxisLabel(exploreChart, unit, context);
            }
            ChartStyles.updateStatsHistoryCombinedChartToSpan(exploreChart, combinedData, aggregationSpan, activity);

        } catch (NoDataException e) {
            exploreChart.clear();
        }
        exploreChart.invalidate();
    }


    @Override
    public String getTitle() {
        return context.getString(R.string.stats_history_title);
    }
}