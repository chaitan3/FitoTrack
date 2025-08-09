package de.tadris.fitness.util

import de.tadris.fitness.data.BaseSample
import de.tadris.fitness.data.HeartRateSample
import de.tadris.fitness.data.RepetitionSample
import de.tadris.fitness.data.RouteSample
import de.tadris.fitness.data.StatsDataTypes.TimeSpan
import java.util.LinkedList
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

object SampleAggregator {

    fun <T : BaseSample?> aggregatedSamples(
        samples: MutableList<T?>,
        maxSampleNumber: Int,
        viewFieldSpan: TimeSpan
    ): MutableList<T?> {
        var firstViewFieldSampleOriginalIndex = 0

        val aggregatedSamples = LinkedList<T?>()
        val viewFieldSamples = LinkedList<T?>()

        // Generating a list with all samples in the current view field
        for (sample in samples) {
            if (sample is RouteSample) {
                if (viewFieldSpan.contains(sample.relativeTime)) {
                    viewFieldSamples.add(sample)
                }
                if (viewFieldSamples.isEmpty()) {
                    firstViewFieldSampleOriginalIndex++
                }
            }
        }

        // Aggregate samples
        val aggregationSize =
            if (viewFieldSamples.size > maxSampleNumber) (2.0.pow(ceil(ln(viewFieldSamples.size / 3.0 / maxSampleNumber))) * 3).toInt() else 1
        val firstGroupIndexInView = firstViewFieldSampleOriginalIndex / aggregationSize
        var currentGroupIndex = firstGroupIndexInView
        // where there is a list combining GpsSamples and other  BaseSample types
        val combinedSamples = mutableListOf<T>()

        val i = viewFieldSamples.listIterator()
        while (i.hasNext()) {
            val originalIndex = i.nextIndex() + firstViewFieldSampleOriginalIndex
            val groupIndex = originalIndex / aggregationSize
            if (groupIndex != currentGroupIndex) {
                val combinedSample = combine(combinedSamples)
                aggregatedSamples.add(combinedSample)
                combinedSamples.clear()

                currentGroupIndex++
            }

            combinedSamples.add(i.next()!!)
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

    @Suppress("UNCHECKED_CAST")
    fun <T> combine(samples: List<T>): T {
        val first = samples.first()!!
        val size = samples.size
        when (first) {
            is RouteSample -> {
                val routeSamples = samples.map { it as RouteSample }
                return first.copy(
                    relativeTime = routeSamples.sumOf { it.relativeTime } / size,
                    latitude = routeSamples.sumOf { it.latitude } / size,
                    longitude = routeSamples.sumOf { it.longitude } / size,
                    elevation = routeSamples.sumOf { it.elevation } / size,
                    elevationMSL = routeSamples.sumOf { it.elevationMSL } / size,
                    speed = routeSamples.sumOf { it.speed } / size,
                    pressure = (routeSamples.sumOf { it.pressure.toDouble() } / size).toFloat(),
                ) as T
            }
            is HeartRateSample -> {
                val hrSamples = samples.map { it as HeartRateSample }
                return first.copy(
                    relativeTime = hrSamples.sumOf { it.relativeTime } / size,
                    heartRate = hrSamples.sumOf { it.heartRate } / size,
                ) as T
            }
            is RepetitionSample -> {
                val repetitionSamples = samples.map { it as RepetitionSample }
                return first.copy(
                    relativeTime = repetitionSamples.sumOf { it.relativeTime } / size,
                    repetitions = repetitionSamples.sumOf { it.repetitions } / size,
                    intensity = repetitionSamples.sumOf { it.intensity } / size,
                    frequency = repetitionSamples.sumOf { it.frequency } / size,
                ) as T
            }
            else -> throw Exception("Unknown sample type to combine: ${first::class.simpleName}")
        }
    }

}
