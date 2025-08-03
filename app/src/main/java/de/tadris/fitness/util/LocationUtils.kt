/*
 * Copyright (c) 2025 Jannis Scheibe <jannis@tadris.de>
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

package de.tadris.fitness.util

import de.tadris.fitness.data.RouteSample

val List<RouteSample>.totalDistance get() =
    sumByPairsDouble { first, second -> first.toLatLong().vincentyDistance(second.toLatLong()) }

inline fun <T> List<T>.sumByPairsDouble(action: (first: T, second: T) -> Double): Double {
    var sum = 0.0
    forEachPair { first, second ->
        sum += action(first, second)
    }
    return sum
}

inline fun <T> List<T>.forEachPair(action: (first: T, second: T) -> Unit){
    if(size <= 1) return
    var last = first()
    for(i in 1..lastIndex){
        val next = this[i]
        action(last, next)
        last = next
    }
}