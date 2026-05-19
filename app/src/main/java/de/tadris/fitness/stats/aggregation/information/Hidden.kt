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

package de.tadris.fitness.stats.aggregation.information

import android.content.Context
import de.tadris.fitness.R
import de.tadris.fitness.data.Workout
import de.tadris.fitness.stats.aggregation.AggregationType

class Hidden(ctx: Context) : AbstractWorkoutInformation(ctx) {
    override fun getTitleRes(): Int {
        return R.string.hidden
    }

    override fun getUnit(): String {
        return ""
    }

    override fun getId() = "hidden"

    override fun getValueFromWorkout(workout: Workout?): Double {
        return 0.0
    }

    override fun getAggregationType(): AggregationType {
        // Hacky. But this should only be used when Sharing, so hopefully this doesn't matter.
        return AggregationType.SUM
    }
}