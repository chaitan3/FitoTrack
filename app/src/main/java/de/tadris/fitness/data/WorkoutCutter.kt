package de.tadris.fitness.data

import android.content.Context
import de.tadris.fitness.Instance

class WorkoutCutter(
    context: Context,
    data: WorkoutData,
    private val startSample: RouteSample?,
    private val endSample: RouteSample?,
) : AbstractWorkoutSaver(context, data) {

    override fun save() {
        if (startSample != null) {
            cutStart(startSample)
        }
        if (endSample != null) {
            cutEnd(endSample)
        }

        calculatePauses() // Update pauses
        calculateRelativeTimes() // Update relative time as start time and pauses might have changed
        calculateAggregatedValues() // Recalculate data

        updateWorkoutAndSamples()
    }

    private fun cutStart(startSample: RouteSample) {
        val removedSamples = mutableListOf<RouteSample>()

        for(sample in routeSamples){
            if(sample.id == startSample.id) break
            removedSamples += sample
        }

        deleteSamples(removedSamples)
        workout = workout.copy(start = startSample.timestamp)
    }

    private fun cutEnd(endSample: RouteSample) {
        val removedSamples = mutableListOf<RouteSample>()

        var found = false
        for(sample in routeSamples){
            if(found){
                removedSamples += sample
            }else if(sample.id == endSample.id){
                found = true
            }
        }

        deleteSamples(removedSamples)
        workout = workout.copy(end = endSample.timestamp)
    }

    private fun deleteSamples(samples: List<RouteSample>) {
        for(sample in samples){
            Instance.getInstance(context).db.workoutDao().deleteSample(sample)
        }
        routeSamples = routeSamples.filter { it !in samples }
    }

}