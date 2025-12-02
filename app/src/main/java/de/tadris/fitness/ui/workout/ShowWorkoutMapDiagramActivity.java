/*
 * Copyright (c) 2021 Jannis Scheibe <jannis@tadris.de>
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

package de.tadris.fitness.ui.workout;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

import de.tadris.fitness.R;
import de.tadris.fitness.data.RouteSample;
import de.tadris.fitness.ui.dialog.SampleConverterPickerDialog;
import de.tadris.fitness.ui.workout.diagram.HeartRateConverter;
import de.tadris.fitness.ui.workout.diagram.HeightConverter;
import de.tadris.fitness.ui.workout.diagram.SampleConverter;
import de.tadris.fitness.ui.workout.diagram.SpeedConverter;

public class ShowWorkoutMapDiagramActivity extends ShowWorkoutColoredMapActivity {

    public static final String DIAGRAM_TYPE_EXTRA = "de.tadris.fitness.ShowWorkoutMapDiagramActivity.DIAGRAM_TYPE";

    public static final String DIAGRAM_TYPE_HEIGHT = "height";
    public static final String DIAGRAM_TYPE_SPEED = "speed";
    public static final String DIAGRAM_TYPE_HEART_RATE = "heartrate";

    private CombinedChart chart;
    private TextView selection;
    private CheckBox showIntervals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBeforeContent();

        setContentView(R.layout.activity_show_workout_map_diagram);
        initRoot();

        this.selection = findViewById(R.id.showWorkoutDiagramInfo);
        this.showIntervals = findViewById(R.id.showWorkoutDiagramIntervals);

        initAfterContent();

        fullScreenItems = true;
        addMap();

        mapView.setClickable(true);

        diagramsInteractive = true;
        root = findViewById(R.id.showWorkoutDiagramParent);

        initDiagram();

        findViewById(R.id.showWorkoutDiagramSelector).setOnClickListener(v -> new SampleConverterPickerDialog(this, this::updateChart, converterManager).show());
        showIntervals.setOnCheckedChangeListener((buttonView, isChecked) -> updateChart());
        showIntervals.setVisibility(intervals != null && intervals.length > 0 ? View.VISIBLE : View.GONE);

        refreshColoring();
    }

    @Override
    protected boolean isDiagramActivity() {
        return true;
    }

    @Override
    public void onMapSelectionChanged(RouteSample sample) {

        if (sample == null) {
            chart.highlightValue(null);
        } else {
            float dataIndex = (sample.getRelativeTime()) / 1000f / 60f;
            Highlight h = new Highlight((float) dataIndex, 0, -1);
            h.setDataIndex(0);
            chart.highlightValue(h);
            chart.centerViewTo(dataIndex, 0, YAxis.AxisDependency.LEFT);
        }
        onChartSelectionChanged(sample);
    }

    private void initDiagram() {
        SampleConverter defaultConverter = getDefaultConverter();
        converterManager.selectedConverters.add(defaultConverter);
        chart = addDiagram(defaultConverter);

        Runnable update = this::updateChart;
        chart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartLongPressed(MotionEvent me) {

            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {
                chart.getHandler().post(update);
            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {

            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
                chart.getHandler().post(update);
            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {
                chart.getHandler().post(update);
            }
        });

        updateChart();
    }

    private SampleConverter getDefaultConverter() {
        String typeExtra = getIntent().getStringExtra(DIAGRAM_TYPE_EXTRA);
        if (typeExtra == null) typeExtra = "";
        switch (typeExtra) {
            default:
            case DIAGRAM_TYPE_SPEED:
                return new SpeedConverter(this);
            case DIAGRAM_TYPE_HEIGHT:
                return new HeightConverter(this);
            case DIAGRAM_TYPE_HEART_RATE:
                return new HeartRateConverter(this);
        }
    }

    private void updateChart() {
        updateChart(chart, converterManager.selectedConverters, showIntervals.isChecked());
        updateChartSelection(chart, converterManager.selectedConverters);
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (SampleConverter converter : converterManager.selectedConverters) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(converter.getName());
        }
        selection.setText(converterManager.selectedConverters.size() > 0 ? sb.toString() : getString(R.string.nothingSelected));
        refreshColoring();
    }

    @Override
    protected void initRoot() {
        root = findViewById(R.id.showWorkoutMapParent);
    }
}