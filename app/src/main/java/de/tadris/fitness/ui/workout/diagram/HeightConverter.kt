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
package de.tadris.fitness.ui.workout.diagram

import android.content.Context
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import de.tadris.fitness.R
import de.tadris.fitness.data.BaseSample
import de.tadris.fitness.data.RouteSample
import de.tadris.fitness.data.WorkoutData

class HeightConverter(context: Context) : AbstractSampleConverter(context) {

    override fun getValue(sample: BaseSample): Float {
        return (sample as RouteSample).height()
    }

    override val name: String
        get() = getString(R.string.height)

    override val yValueFormatter: ValueFormatter
        get() = DefaultValueFormatter(0)

    override val unit: String
        get() = distanceUnitUtils.distanceUnitSystem.elevationUnit

    override val color: Int
        get() = R.color.diagramHeight

    override fun getMinValue(data: WorkoutData): Float {
        return data.samples.minOf { it.elevationMSL }.height()
    }

    override fun getMaxValue(data: WorkoutData): Float {
        return data.samples.maxOf { it.elevationMSL }.height()
    }

    override fun getEntries(data: WorkoutData): List<Pair<BaseSample, Float>> {
        return data.samples.map { it to it.height() }
    }

    private fun RouteSample.height() = elevationMSL.height()

    private fun Double.height() =
        distanceUnitUtils.distanceUnitSystem.getElevationFromMeters(this).toFloat()
}
