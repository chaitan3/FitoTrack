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

import androidx.annotation.ColorRes
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.formatter.ValueFormatter
import de.tadris.fitness.data.BaseSample
import de.tadris.fitness.data.WorkoutData

interface SampleConverter {

    fun getMinValue(data: WorkoutData): Float

    fun getMaxValue(data: WorkoutData): Float

    fun getEntries(data: WorkoutData): List<Pair<BaseSample, Float>>

    fun getValue(sample: BaseSample): Float

    val name: String

    val xAxisLabel: String

    val xValueFormatter: ValueFormatter

    val yAxisLabel: String

    val yValueFormatter: ValueFormatter

    val isIntervalSetVisible: Boolean

    fun afterAdd(chart: CombinedChart)

    @get:ColorRes
    val color: Int

    val unit: String

}
