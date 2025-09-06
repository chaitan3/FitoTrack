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
import de.tadris.fitness.R
import de.tadris.fitness.data.BaseSample
import de.tadris.fitness.data.WorkoutData

class IntensityConverter(context: Context) : AbstractSampleConverter(context) {

    override fun getMinValue(data: WorkoutData): Float {
        return data.repetitionSamples.minOf { it.intensity }.toFloat()
    }

    override fun getMaxValue(data: WorkoutData): Float {
        return data.repetitionSamples.maxOf { it.intensity }.toFloat()
    }

    override fun getEntries(data: WorkoutData): List<Pair<BaseSample, Float>> {
        return data.repetitionSamples.map { it to it.intensity.toFloat() }
    }

    override fun getValue(sample: BaseSample) = 0f

    override val name: String
        get() = getString(R.string.workoutIntensity)

    override val color: Int
        get() = R.color.diagramIntensity

    override val unit: String
        get() = ""
}
