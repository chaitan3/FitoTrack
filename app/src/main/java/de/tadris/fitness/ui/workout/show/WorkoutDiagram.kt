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

package de.tadris.fitness.ui.workout.show

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import de.tadris.fitness.data.Workout
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.stats.StatsDataTypes
import de.tadris.fitness.ui.workout.diagram.SampleConverter
import de.tadris.fitness.util.SampleAggregator
import de.tadris.fitness.util.charts.ChartStyles
import de.tadris.fitness.util.charts.marker.DisplayValueMarker

@Composable
internal fun WorkoutDiagram(
    converter: SampleConverter,
    data: WorkoutData,
    workout: Workout,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    AndroidView(
        factory = { ctx ->
            val chart = CombinedChart(ctx)
            buildCombinedChart(chart, converter, data, workout)
            if (onClick != null) chart.setOnClickListener { onClick() }
            chart
        },
        modifier = modifier,
    )
}

private fun buildCombinedChart(
    chart: CombinedChart,
    converter: SampleConverter,
    data: WorkoutData,
    workout: Workout,
) {
    chart.setScaleXEnabled(false)
    chart.setScaleYEnabled(false)
    chart.isHighlightPerDragEnabled = false
    chart.isHighlightPerTapEnabled = false

    val entries: List<Entry> = SampleAggregator.aggregateSamples(
        converter.getEntries(data).map { (sample, value) ->
            Entry(sample.relativeTime / 1000f / 60f, value, sample)
        },
        100,
        StatsDataTypes.TimeSpan(0, workout.activeDuration)
    )

    chart.xAxis.valueFormatter = converter.xValueFormatter

    val dataSet = LineDataSet(entries, converter.name)
    dataSet.color = chart.context.resources.getColor(converter.color, chart.context.theme)
    dataSet.setDrawValues(false)
    dataSet.setDrawCircles(false)
    dataSet.lineWidth = 2f
    dataSet.highlightLineWidth = 2.5f
    dataSet.mode = LineDataSet.Mode.LINEAR

    chart.axisLeft.valueFormatter = converter.yValueFormatter
    chart.axisRight.valueFormatter = converter.yValueFormatter
    chart.axisRight.isEnabled = false

    val lineData = LineData(dataSet)
    val combinedData = CombinedData()
    combinedData.setData(lineData)
    combinedData.setData(BarData())

    chart.data = combinedData
    chart.marker =
        DisplayValueMarker(chart.context, converter.yValueFormatter, converter.unit, lineData)

    ChartStyles.setXAxisLabel(chart, converter.xAxisLabel, chart.context)
    ChartStyles.setYAxisLabel(chart, converter.yAxisLabel, chart.context)
    ChartStyles.defaultLineChart(chart, chart.context)

    chart.description.isEnabled = true
    chart.legend.isEnabled = true
    chart.invalidate()
}