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

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.BarData;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.stats.NoDataException;
import de.tadris.fitness.stats.StatsDataTypes;
import de.tadris.fitness.stats.StatsProvider;
import de.tadris.fitness.ui.FitoTrackActivity;
import de.tadris.fitness.ui.statistics.TimeSpanSelection;
import de.tadris.fitness.util.charts.ChartStyles;

public class StatsOverviewFragment extends StatsFragment {
    StatsProvider statsProvider;
    FitoTrackActivity activity;

    TimeSpanSelection timeSpanSelection;
    HorizontalBarChart distanceChart;
    HorizontalBarChart numberOfActivitiesChart;
    HorizontalBarChart durationChart;

    public StatsOverviewFragment(FitoTrackActivity ctx) {
        super(R.layout.fragment_stats_overview, ctx);
        statsProvider = new StatsProvider(ctx);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activity = (FitoTrackActivity)getContext();

        timeSpanSelection = view.findViewById(R.id.time_span_selection);
        timeSpanSelection.setForegroundColor(getResources().getColor(R.color.textDarkerWhite));

        numberOfActivitiesChart = view.findViewById(R.id.stats_number_of_workout_chart);
        distanceChart = view.findViewById(R.id.stats_distances_chart);
        timeSpanSelection.addOnTimeSpanSelectionListener((aggregationSpan, instance) -> updateCharts());

        ChartStyles.defaultBarChart(numberOfActivitiesChart);
        animateChart(numberOfActivitiesChart);

        ChartStyles.setXAxisLabel(distanceChart, Instance.getInstance(context).distanceUnitUtils.getDistanceUnitSystem().getLongDistanceUnit(), activity);
        ChartStyles.defaultBarChart(distanceChart);
        animateChart(distanceChart);

        durationChart = view.findViewById(R.id.stats_duration_chart);
        ChartStyles.setXAxisLabel(durationChart, getContext().getString(R.string.timeHourShort), activity);
        ChartStyles.defaultBarChart(durationChart);
        animateChart(durationChart);

        updateCharts();
    }

    @Override
    public String getTitle() {
        return context.getString(R.string.stats_overview_title);
    }

    private void animateChart (HorizontalBarChart chart) {
        chart.animateY(500, Easing.EaseInExpo);
    }

    private void updateCharts() {
        long start = timeSpanSelection.getSelectedDate();
        StatsDataTypes.TimeSpan span = new StatsDataTypes.TimeSpan(start, timeSpanSelection.getSelectedAggregationSpan().getAggregationEnd(start));

        try {
            BarData distanceData = new BarData(statsProvider.totalDistances(span));
            ChartStyles.horizontalBarChartIconLabel(distanceChart, distanceData, context);
            ChartStyles.setXAxisLabel(distanceChart, Instance.getInstance(getContext()).distanceUnitUtils.getDistanceUnitSystem().getLongDistanceUnit(), activity);
        } catch (NoDataException e) {
            ChartStyles.barChartNoData(distanceChart, (FitoTrackActivity)getContext());
        }
        distanceChart.invalidate();

        try {
            BarData durationData = new BarData(statsProvider.totalDurations(span));
            ChartStyles.horizontalBarChartIconLabel(durationChart, durationData, context);
            ChartStyles.setXAxisLabel(durationChart, getContext().getString(R.string.timeHourShort), activity);
        } catch (NoDataException e) {
            ChartStyles.barChartNoData(durationChart, (FitoTrackActivity)getContext());
        }
        durationChart.invalidate();

        try {
            BarData numberOfActivitiesData = new BarData(statsProvider.numberOfActivities(span));
            ChartStyles.horizontalBarChartIconLabel(numberOfActivitiesChart,numberOfActivitiesData, context);
        } catch (NoDataException e) {
            ChartStyles.barChartNoData(numberOfActivitiesChart, (FitoTrackActivity)getContext());
        }
        numberOfActivitiesChart.invalidate();
    }
}
