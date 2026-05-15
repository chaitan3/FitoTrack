/*
 * Copyright (c) 2026 Jannis Scheibe <jannis@tadris.de>
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

package de.tadris.fitness.ui.charts;

import static de.tadris.fitness.ui.charts.BitmapHelper.drawableToBitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import de.tadris.fitness.R;
import de.tadris.fitness.data.WorkoutType;
import de.tadris.fitness.stats.aggregation.AggregationSpan;
import de.tadris.fitness.ui.FitoTrackActivity;
import de.tadris.fitness.ui.ThemeExtensionsKt;
import de.tadris.fitness.ui.charts.formatter.FractionedDateFormatter;
import de.tadris.fitness.ui.charts.marker.DisplayValueMarker;
import de.tadris.fitness.ui.charts.marker.WorkoutDisplayMarker;
import de.tadris.fitness.util.Icon;

public class ChartStyles {

    public static float BAR_WIDTH_FACTOR = 2f / 3f;

    public static void defaultChart(BarLineChartBase chart) {
        chart.getAxisLeft().setEnabled(true);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);
    }

    public static void defaultBarChart(BarChart chart) {
        defaultChart(chart);
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getXAxis().setEnabled(false);
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(true);
        chart.setFitBars(true);
    }

    public static void defaultLineChart(BarLineChartBase chart, Context context) {
        defaultChart(chart);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setEnabled(true);
        chart.getXAxis().setDrawGridLines(true);
        chart.getXAxis().setTextColor(ThemeExtensionsKt.getThemeTextColor(context));
        chart.getAxisLeft().setTextColor(ThemeExtensionsKt.getThemeTextColor(context));
        chart.getAxisRight().setTextColor(ThemeExtensionsKt.getThemeTextColor(context));
    }

    public static void setXAxisLabel(Chart chart, String label, Context context) {
        Description description = new Description();
        description.setText(label);
        description.setTextSize(10);
        description.setEnabled(true);
        description.setTextColor(ThemeExtensionsKt.getThemeTextColor(context));
        chart.setDescription(description);
    }

    public static void setYAxisLabel(Chart chart, String label, Context context) {
        LegendEntry legend = new LegendEntry();
        legend.label = label;
        chart.getLegend().setCustom(Collections.singletonList(legend));
        chart.getLegend().setEnabled(true);
        chart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        chart.getLegend().setFormSize(0);
        chart.getLegend().setFormToTextSpace(-16);
        chart.getLegend().setForm(Legend.LegendForm.NONE);
        chart.getLegend().setTextColor(ThemeExtensionsKt.getThemeTextColor(context));
    }

    public static void defaultHistogram(BarChart chart, Context context, ValueFormatter xValueFormatter, ValueFormatter yValueFormatter) {
        float min = chart.getBarData().getDataSets().get(0).getEntryForIndex(0).getX();
        int nBins = chart.getBarData().getDataSets().get(0).getEntryCount();
        float max = chart.getBarData().getDataSets().get(0).getEntryForIndex(nBins - 1).getX();

        // now that we know the meta inf, set the bars to positions between indices, so we can use an IndexAxisValueFormatter
        // simultaneously construct the new labels
        String[] labels = new String[nBins + 1];
        float barWidth = (float) ((max - min) / (nBins - 1));
        for (int i = 0; i < nBins; i++) {
            chart.getBarData().getDataSets().get(0).getEntryForIndex(i).setX(i + 0.5f);
            labels[i] = xValueFormatter.getFormattedValue((float) (min + (i + 0.5) * barWidth));//distanceUnitUtils.getPace(1/x/60, false, false);
        }
        labels[nBins] = xValueFormatter.getFormattedValue((float) (min + (nBins + 0.5) * barWidth));


        chart.getBarData().setBarWidth(1);
        chart.getBarData().setDrawValues(false);
        ChartStyles.defaultBarChart(chart);
        chart.getXAxis().setEnabled(true);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setDrawLabels(true);
        chart.getXAxis().setLabelRotationAngle(-90);
        chart.getAxisLeft().setEnabled(true);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setGranularity(1);
        chart.getXAxis().setLabelCount(labels.length);
        float shiftY = chart.getData().getYMax() / 6;
        chart.getAxisLeft().setAxisMinimum(0 - shiftY + 0.1f * shiftY);
        chart.getAxisLeft().setGranularity(shiftY);
        chart.setScaleEnabled(false);
        chart.setNestedScrollingEnabled(false);
        chart.getXAxis().setAxisMinimum(-0.5f);
        chart.getXAxis().setAxisMaximum(nBins + 0.5f);
        chart.getAxisLeft().setValueFormatter(yValueFormatter);
        chart.getXAxis().setTextColor(ThemeExtensionsKt.getThemeTextColor(context));
        chart.getAxisLeft().setTextColor(ThemeExtensionsKt.getThemeTextColor(context));
        chart.getAxisRight().setTextColor(ThemeExtensionsKt.getThemeTextColor(context));

        LegendEntry[] entries = chart.getLegend().getEntries();
        String unit = entries.length > 0 ? entries[0].label : "";
        chart.setMarker(new DisplayValueMarker(context, chart.getAxisLeft().getValueFormatter(), " " + unit, chart.getBarData()));
    }

    // Be careful to use this function outside StatsHistoryFragment
    public static void updateStatsHistoryCombinedChartToSpan(CombinedChart chart, CombinedData combinedData, AggregationSpan aggregationSpan, FitoTrackActivity ctx) {
        ChartStyles.setTextAppearance(combinedData, ctx);
        if (combinedData.getBarData() != null) {
            float barWidth = Math.max(aggregationSpan.spanInterval, AggregationSpan.DAY.spanInterval);
            combinedData.getBarData().setBarWidth(barWidth * ChartStyles.BAR_WIDTH_FACTOR);
        }

        updateTimeAxisToZoom(ctx, chart, chart.getXAxis(), aggregationSpan);
        chart.getAxisLeft().setValueFormatter(combinedData.getMaxEntryCountSet().getValueFormatter());

        if (chart.getLegend().getEntries().length > 0) {
            String yLabel = chart.getLegend().getEntries()[0].label;
            chart.setMarker(new DisplayValueMarker(ctx, chart.getAxisLeft().getValueFormatter(), yLabel, combinedData.getCandleData()));
        } else {
            chart.setMarker(new DisplayValueMarker(ctx, chart.getAxisLeft().getValueFormatter(), "", combinedData.getCandleData()));
        }

        chart.setData(combinedData);
        chart.getXAxis().setAxisMinimum(combinedData.getXMin() - aggregationSpan.spanInterval / 2);
        chart.getXAxis().setAxisMaximum(combinedData.getXMax() + aggregationSpan.spanInterval / 2);
        chart.invalidate();
    }

    public static void updateTimeAxisToZoom(FitoTrackActivity ctx, Chart chart, AxisBase axis, AggregationSpan aggregationSpan) {
        axis.setValueFormatter(new FractionedDateFormatter(ctx, aggregationSpan));
        axis.setGranularity((float) aggregationSpan.spanInterval);
        if (axis instanceof XAxis) {
            ChartStyles.setXAxisLabel(chart, ctx.getString(aggregationSpan.axisLabel), ctx);
        } else {
            ChartStyles.setYAxisLabel(chart, ctx.getString(aggregationSpan.axisLabel), ctx);
        }
    }

    public static void setTextAppearance(ChartData data, FitoTrackActivity context) {
        data.setValueTextSize(10);
        data.setValueTextColor(context.getThemeTextColor());
    }


    public static void barChartIconLabel(BarChart chart, BarData data, Context ctx) {
        setTextAppearance(data, (FitoTrackActivity) ctx);

        ArrayList<Bitmap> imageList = new ArrayList<>();
        for (int i = 0; i < data.getDataSets().get(0).getEntryCount(); i++) {
            try {
                WorkoutType w = (WorkoutType) data.getDataSets().get(0).getEntryForIndex(i).getData();
                Drawable d = ctx.getDrawable(Icon.getIcon(w.icon));
                d.mutate().setColorFilter(w.color, PorterDuff.Mode.SRC_IN);
                imageList.add(drawableToBitmap(d));
            } catch (Exception e) {
                return; // If drawable not available, its not possible...
            }
        }

        chart.setRenderer(new BarChartIconRenderer(chart, chart.getAnimator(), chart.getViewPortHandler(), imageList, ctx));
        chart.setScaleEnabled(false);
        chart.setExtraOffsets(0, 0, 0, 25);

        chart.setData(data);
        chart.setMarker(new WorkoutDisplayMarker(ctx));
        chart.getAxisLeft().setAxisMinimum(0);
    }

    public static void barChartNoData(BarChart chart, FitoTrackActivity ctx) {
        chart.setDrawMarkers(false);
        chart.setData(new BarData()); // Needed in case there is nothing to clear...
        chart.clearValues();
        chart.setExtraOffsets(0, 0, 0, 0);
        ChartStyles.setXAxisLabel(chart, ctx.getString(R.string.no_workouts_recorded), ctx);
    }

    public static void horizontalBarChartIconLabel(HorizontalBarChart chart, BarData data, Context ctx) {
        setTextAppearance(data, (FitoTrackActivity) ctx);

        ArrayList<Bitmap> imageList = new ArrayList<>();
        for (int i = 0; i < data.getDataSets().get(0).getEntryCount(); i++) {
            try {
                WorkoutType w = (WorkoutType) data.getDataSets().get(0).getEntryForIndex(i).getData();
                Drawable d = ctx.getDrawable(Icon.getIcon(w.icon));
                d.mutate().setColorFilter(w.color, PorterDuff.Mode.SRC_IN);
                imageList.add(drawableToBitmap(d));
            } catch (Exception e) {
                return; // If drawable not available, its not possible... //Todo: But it should be possible to draw the other icons
            }
        }

        chart.setRenderer(new HorizontalBarChartIconRenderer(chart, chart.getAnimator(), chart.getViewPortHandler(), imageList, ctx));
        chart.setScaleEnabled(false);
        chart.setExtraOffsets(30, 0, 0, 0);

        chart.setData(data);
        chart.setMarker(new WorkoutDisplayMarker(ctx));
        chart.setDrawMarkers(true);
        chart.getAxisLeft().setAxisMinimum(0);
        chart.getAxisRight().setAxisMinimum(0);
        chart.getDescription().setEnabled(false);
    }

    public static AggregationSpan statsAggregationSpan(BarLineChartBase chart) {
        long timeSpan = (long) ((chart.getHighestVisibleX() - chart.getLowestVisibleX()));
        return statsAggregationSpan(timeSpan);
    }

    public static AggregationSpan statsAggregationSpan(long timeSpan) {
        AggregationSpan aggregationSpan;

        if (TimeUnit.DAYS.toMillis(1095) < timeSpan) {
            aggregationSpan = AggregationSpan.YEAR;
        } else if (TimeUnit.DAYS.toMillis(93) < timeSpan) {
            aggregationSpan = AggregationSpan.MONTH;
        } else if (TimeUnit.DAYS.toMillis(21) < timeSpan) {
            aggregationSpan = AggregationSpan.WEEK;
        } else {
            aggregationSpan = AggregationSpan.SINGLE;
        }
        return aggregationSpan;
    }

    public static void fixViewPortOffsets(CombinedChart chart, float offset) {
        chart.setViewPortOffsets(offset, offset / 2, offset / 2, offset / 2);
    }

    public static void animateChart(CombinedChart chart) {
        chart.animateY(500, Easing.EaseInExpo);
    }
}
