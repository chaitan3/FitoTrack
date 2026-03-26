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

package de.tadris.fitness.util

import com.github.mikephil.charting.data.Entry
import de.tadris.fitness.stats.StatsDataTypes.TimeSpan
import java.util.LinkedList
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

object SampleAggregator {

    fun aggregateSamples(
        samples: List<Entry>,
        maxSampleNumber: Int,
        viewFieldSpan: TimeSpan
    ): List<Entry> {
        var firstViewFieldSampleOriginalIndex = 0

        val aggregatedSamples = LinkedList<Entry>()
        val viewFieldSamples = LinkedList<Entry>()

        // Generating a list with all samples in the current view field
        for (sample in samples) {
            if (viewFieldSpan.contains((sample.x * 1000 * 60).toLong())) {
                viewFieldSamples.add(sample)
            }
            if (viewFieldSamples.isEmpty()) {
                firstViewFieldSampleOriginalIndex++
            }
        }

        // Aggregate samples
        val aggregationSize =
            if (viewFieldSamples.size > maxSampleNumber) (2.0.pow(ceil(ln(viewFieldSamples.size / 3.0 / maxSampleNumber))) * 3).toInt() else 1
        val firstGroupIndexInView = firstViewFieldSampleOriginalIndex / aggregationSize
        var currentGroupIndex = firstGroupIndexInView
        // where there is a list combining GpsSamples and other  BaseSample types
        val combinedSamples = mutableListOf<Entry>()

        val i = viewFieldSamples.listIterator()
        while (i.hasNext()) {
            val originalIndex = i.nextIndex() + firstViewFieldSampleOriginalIndex
            val groupIndex = originalIndex / aggregationSize
            if (groupIndex != currentGroupIndex) {
                val combinedSample = Entry(
                    (combinedSamples.sumOf { it.x.toDouble() } / combinedSamples.size).toFloat(),
                    (combinedSamples.sumOf { it.y.toDouble() } / combinedSamples.size).toFloat(),
                    combinedSamples.first().data
                )
                aggregatedSamples.add(combinedSample)
                combinedSamples.clear()

                currentGroupIndex++
            }

            combinedSamples.add(i.next())
        }
        if (combinedSamples.isNotEmpty()) {
            val combinedSample = Entry(
                (combinedSamples.sumOf { it.x.toDouble() } / combinedSamples.size).toFloat(),
                (combinedSamples.sumOf { it.y.toDouble() } / combinedSamples.size).toFloat(),
                combinedSamples.first().data
            )
            aggregatedSamples.add(combinedSample)
        }

        // Index of element directly behind the last shown group
        val behindViewIndex = (currentGroupIndex + 1) * aggregationSize
        // Index of element directly in front of the first shown group
        val beforeViewIndex = firstGroupIndexInView * aggregationSize - 1

        // Add those samples
        val samplesSize = samples.size
        if (behindViewIndex < samplesSize) {
            aggregatedSamples.add(samples[behindViewIndex])
            // Also add the last sample to the list
            aggregatedSamples.add(samples[samples.size - 1])
        }
        if (beforeViewIndex > 0) {
            aggregatedSamples.add(0, samples[beforeViewIndex])
            // Also add the first sample to the list
            aggregatedSamples.add(0, samples.first())
        }

        return aggregatedSamples
    }

}
