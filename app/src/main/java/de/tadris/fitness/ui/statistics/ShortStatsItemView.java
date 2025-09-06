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

package de.tadris.fitness.ui.statistics;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.data.WorkoutTypeManager;
import de.tadris.fitness.stats.NoDataException;
import de.tadris.fitness.stats.StatsDataProvider;
import de.tadris.fitness.stats.StatsDataTypes;
import de.tadris.fitness.stats.StatsProvider;
import de.tadris.fitness.stats.WorkoutProperty;
import de.tadris.fitness.ui.FitoTrackActivity;
import de.tadris.fitness.util.charts.ChartStyles;

public class ShortStatsItemView extends LinearLayout {
    BarChart chart;
    TextView title;
    TimeSpanSelection timeSpanSelection;

    StatsProvider statsProvider;

    public int chartType;

    public ShortStatsItemView(Context context) {
        this(context, null);
    }

    public ShortStatsItemView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.short_stats_item, this);

        chart = findViewById(R.id.short_stats_chart);
        timeSpanSelection = findViewById(R.id.short_stats_time_span_selection);
        title = findViewById(R.id.short_stats_title);

        statsProvider = new StatsProvider(context);

        chart.setOnTouchListener(null);
        ChartStyles.defaultBarChart(chart);

        StatsDataProvider statsDataProvider = new StatsDataProvider(context);

        setOnClickListener(view -> context.startActivity(new Intent(getContext(), StatisticsActivity.class)));

        long firstWorkoutTime;
        long lastWorkoutTime;
        try {
            firstWorkoutTime = statsDataProvider.getFirstData(WorkoutProperty.LENGTH, WorkoutTypeManager.getInstance().getAllTypes(context)).time;
            lastWorkoutTime = statsDataProvider.getLastData(WorkoutProperty.LENGTH, WorkoutTypeManager.getInstance().getAllTypes(context)).time;

        } catch (NoDataException e) {
            return;
        }
        timeSpanSelection.addOnTimeSpanSelectionListener((aggregationSpan, instance) -> updateChart());
    }

    public void updateChart() {
        BarData data;
        FitoTrackActivity activity = (FitoTrackActivity)getContext();

        timeSpanSelection.loadAggregationSpanFromPreferences();
        long start = timeSpanSelection.getSelectedDate();
        StatsDataTypes.TimeSpan span = new StatsDataTypes.TimeSpan(start,
                start + timeSpanSelection.getSelectedAggregationSpan().spanInterval);

        try {
            switch (chartType)
            {
                case 0:
                    title.setText(getContext().getString(R.string.workoutDistance));
                    data = new BarData(statsProvider.totalDistances(span));
                    ChartStyles.setXAxisLabel(chart, Instance.getInstance(getContext()).distanceUnitUtils.getDistanceUnitSystem().getLongDistanceUnit(), activity);
                    break;
                case 1:
                    title.setText(getContext().getString(R.string.workoutDuration));
                    data = new BarData(statsProvider.totalDurations(span));
                    ChartStyles.setXAxisLabel(chart, getContext().getString(R.string.timeHourShort), activity);
                    break;
                default:
                    title.setText(getContext().getString(R.string.workoutCount));
                    data = new BarData(statsProvider.numberOfActivities(span));
                    ChartStyles.setXAxisLabel(chart, "", activity);
                    break;
            }
            ChartStyles.barChartIconLabel(chart, data, getContext());
        } catch (NoDataException e) {
            ChartStyles.barChartNoData(chart, activity);
        }
        chart.invalidate();
    }
}
