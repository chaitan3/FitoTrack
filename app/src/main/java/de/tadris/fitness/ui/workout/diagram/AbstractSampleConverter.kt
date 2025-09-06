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
package de.tadris.fitness.ui.workout.diagram

import android.content.Context
import androidx.annotation.StringRes
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.util.charts.formatter.TimeFormatter
import java.util.concurrent.TimeUnit

abstract class AbstractSampleConverter(@JvmField protected val context: Context) : SampleConverter {

    protected val distanceUnitUtils = Instance.getInstance(context).distanceUnitUtils!!
    protected val energyUnitUtils = Instance.getInstance(context).energyUnitUtils!!

    protected fun getString(@StringRes resId: Int): String {
        return context.getString(resId)
    }

    override val xAxisLabel: String
        get() = context.getString(R.string.workoutTime)

    override val xValueFormatter: ValueFormatter
        get() = TimeFormatter(TimeUnit.MINUTES)

    override val yAxisLabel: String
        get() = unit

    override val yValueFormatter: ValueFormatter
        get() = DefaultValueFormatter(1)

    override fun afterAdd(chart: CombinedChart) {}

    override val isIntervalSetVisible: Boolean
        get() = false // Defaults to false

}
