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

package de.tadris.fitness.ui.workout.diagram

import android.graphics.Color
import android.view.MotionEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import de.tadris.fitness.data.RouteSample
import de.tadris.fitness.data.Workout
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.stats.StatsDataTypes
import de.tadris.fitness.util.SampleAggregator
import de.tadris.fitness.util.WorkoutCalculator
import de.tadris.fitness.util.charts.ChartStyles
import de.tadris.fitness.util.charts.marker.DisplayValueMarker

private class WorkoutDiagramHolder {
    var chart: CombinedChart? = null
}

/** Single-converter overload — backward-compatible with existing usages. */
@Composable
internal fun WorkoutDiagram(
    converter: SampleConverter,
    data: WorkoutData,
    workout: Workout,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    onSelectionChanged: ((RouteSample?) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) = WorkoutDiagram(
    converters = listOf(converter),
    data = data,
    workout = workout,
    modifier = modifier,
    interactive = interactive,
    onSelectionChanged = onSelectionChanged,
    onClick = onClick,
)

/** Multi-converter overload — used by the map+diagram screen. */
@Composable
internal fun WorkoutDiagram(
    converters: List<SampleConverter>,
    data: WorkoutData,
    workout: Workout,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    showIntervals: Boolean = false,
    onSelectionChanged: ((RouteSample?) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    require(converters.isNotEmpty())
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val holder = remember { WorkoutDiagramHolder() }
    // Tracks visible X range in milliseconds after a zoom/pan gesture.
    var visibleRange by remember { mutableStateOf<Pair<Long, Long>?>(null) }

    AndroidView(
        factory = { ctx ->
            val chart = CombinedChart(ctx)
            holder.chart = chart

            if (interactive) {
                chart.setScaleXEnabled(true)
                chart.isHighlightPerDragEnabled = true
                chart.isHighlightPerTapEnabled = true

                chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry, h: Highlight) {
                        onSelectionChanged?.invoke(e.data as? RouteSample)
                    }

                    override fun onNothingSelected() {
                        onSelectionChanged?.invoke(null)
                    }
                })

                chart.onChartGestureListener = object : OnChartGestureListener {
                    override fun onChartGestureStart(
                        me: MotionEvent,
                        lastPerformedGesture: ChartTouchListener.ChartGesture
                    ) {
                    }

                    override fun onChartGestureEnd(
                        me: MotionEvent,
                        lastPerformedGesture: ChartTouchListener.ChartGesture
                    ) {
                    }

                    override fun onChartLongPressed(me: MotionEvent) {}
                    override fun onChartDoubleTapped(me: MotionEvent) {
                        updateRange()
                    }

                    override fun onChartSingleTapped(me: MotionEvent) {}
                    override fun onChartFling(
                        me1: MotionEvent,
                        me2: MotionEvent,
                        velocityX: Float,
                        velocityY: Float
                    ) {
                    }

                    override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) {
                        updateRange()
                    }

                    override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) {
                        updateRange()
                    }

                    private fun updateRange() {
                        val low = (chart.lowestVisibleX * 60000).toLong()
                        val high = (chart.highestVisibleX * 60000).toLong()
                        if (high > low) visibleRange = low to high
                    }
                }
            } else {
                chart.setScaleXEnabled(false)
                chart.isHighlightPerDragEnabled = false
                chart.isHighlightPerTapEnabled = false
                if (onClick != null) chart.setOnClickListener { onClick() }
            }

            chart.setScaleYEnabled(false)
            buildChartData(
                chart,
                converters,
                data,
                workout,
                textColor,
                primaryColor,
                showIntervals,
                visibleRange
            )
            chart
        },
        update = { chart ->
            buildChartData(
                chart,
                converters,
                data,
                workout,
                textColor,
                primaryColor,
                showIntervals,
                visibleRange
            )
        },
        modifier = modifier,
    )
}

private fun buildChartData(
    chart: CombinedChart,
    converters: List<SampleConverter>,
    data: WorkoutData,
    workout: Workout,
    textColor: Int,
    primaryColor: Int,
    showIntervals: Boolean,
    visibleRange: Pair<Long, Long>?,
) {
    val timeSpan = visibleRange
        ?.let { (start, end) -> StatsDataTypes.TimeSpan(start, end) }
        ?: StatsDataTypes.TimeSpan(0, workout.activeDuration)

    val hasMultiple = converters.size > 1
    val lineData = LineData()

    converters.forEachIndexed { index, converter ->
        val entries: List<Entry> = SampleAggregator.aggregateSamples(
            converter.getEntries(data).map { (sample, value) ->
                Entry(sample.relativeTime / 1000f / 60f, value, sample)
            },
            100,
            timeSpan,
        )

        chart.xAxis.valueFormatter = converter.xValueFormatter

        val dataSet = LineDataSet(entries, converter.name)
        dataSet.color = if (hasMultiple) {
            chart.context.resources.getColor(converter.color, chart.context.theme)
        } else {
            primaryColor
        }
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)
        dataSet.lineWidth = 2f
        dataSet.highlightLineWidth = 2.5f
        dataSet.mode = LineDataSet.Mode.LINEAR

        if (hasMultiple) {
            val axis = if (index == 0) YAxis.AxisDependency.LEFT else YAxis.AxisDependency.RIGHT
            dataSet.axisDependency = axis
            chart.getAxis(axis).valueFormatter = converter.yValueFormatter
            chart.axisRight.isEnabled = true
        } else {
            chart.axisLeft.valueFormatter = converter.yValueFormatter
            chart.axisRight.valueFormatter = converter.yValueFormatter
            chart.axisRight.isEnabled = false
        }

        lineData.addDataSet(dataSet)
    }

    // Fix axis scale based on data ranges
    chart.axisLeft.axisMaximum = converters[0].getMaxValue(data) * 1.05f
    chart.axisLeft.axisMinimum = converters[0].getMinValue(data) * 0.95f
    if (hasMultiple) {
        chart.axisRight.axisMaximum = converters[1].getMaxValue(data) * 1.05f
        chart.axisRight.axisMinimum = converters[1].getMinValue(data) * 0.95f
    }

    val combinedData = CombinedData()
    combinedData.setData(lineData)

    if (showIntervals) {
        combinedData.setData(buildIntervalBars(chart, data, lineData))
    } else {
        combinedData.setData(BarData())
    }

    chart.data = combinedData

    val marker = if (hasMultiple) {
        DisplayValueMarker(chart.context, converters[0].yValueFormatter, "", lineData)
    } else {
        DisplayValueMarker(
            chart.context,
            converters[0].yValueFormatter,
            converters[0].unit,
            lineData
        )
    }
    chart.marker = marker

    ChartStyles.setXAxisLabel(chart, converters[0].xAxisLabel, chart.context)
    ChartStyles.setYAxisLabel(
        chart,
        if (hasMultiple) "" else converters[0].yAxisLabel,
        chart.context
    )
    ChartStyles.defaultLineChart(chart, chart.context)

    chart.xAxis.textColor = textColor
    chart.axisLeft.textColor = textColor
    chart.axisRight.textColor = textColor
    chart.description.textColor = textColor
    chart.legend.textColor = textColor
    chart.description.isEnabled = true
    chart.legend.isEnabled = true
    chart.invalidate()
}

private fun buildIntervalBars(
    chart: CombinedChart,
    data: WorkoutData,
    lineData: LineData,
): BarData {
    val yMax = lineData.getDataSetByIndex(0).yMax * 1.05f
    val barEntries = WorkoutCalculator.getIntervalSetTimesFromWorkout(data)
        .map { relativeTime ->
            BarEntry(relativeTime / 1000f / 60f, yMax)
        }
    if (barEntries.isEmpty()) return BarData()

    val barDataSet = BarDataSet(barEntries, "")
    barDataSet.barBorderWidth = 3f
    barDataSet.color = Color.GRAY
    val barData = BarData(barDataSet)
    barData.barWidth = 0.01f
    barData.setDrawValues(false)
    return barData
}
