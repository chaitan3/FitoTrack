package de.tadris.fitness.util.io

import android.content.Context
import de.tadris.fitness.data.AbstractWorkoutSaver
import de.tadris.fitness.data.RouteSample
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.recording.RecorderService
import kotlin.math.absoluteValue

class ImportWorkoutSaver(context: Context, data: WorkoutData): AbstractWorkoutSaver(context, data) {

    override fun save(){
        dropDenseSamples()
        calculateIds()
        calculatePauses()
        calculateRelativeTimes()
        calculateSampleSpeed()
        calculateAggregatedValues()
        insertWorkoutIntoDatabase()
    }

    private fun calculateIds(){
        workout = workout.copy(id = System.nanoTime())
        routeSamples = routeSamples.mapIndexed { index, sample ->
            sample.copy(
                workoutId = workout.id,
                id = workout.id + index,
            )
        }
        hrSamples = hrSamples.mapIndexed { index, sample ->
            sample.copy(
                workoutId = workout.id,
                id = workout.id + index,
            )
        }
    }

    private fun dropDenseSamples(){
        if(routeSamples.isEmpty()) return

        val minDistance = workout.getWorkoutType(context).minDistance
        val minTimeDiff = RecorderService.MIN_DURATION_DIFF

        val removedSamples = mutableListOf<RouteSample>()

        var lastSample = routeSamples.first()
        for(sample in routeSamples){
            val distance = sample.toLatLong().sphericalDistance(lastSample.toLatLong()).absoluteValue
            val timeDiff = (sample.timestamp - lastSample.timestamp).absoluteValue
            if(distance < minDistance || timeDiff < minTimeDiff){
                removedSamples += sample
            }else{
                lastSample = sample
            }
        }

        routeSamples = routeSamples.filter { it !in removedSamples }
    }

}