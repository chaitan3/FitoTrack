package de.tadris.fitness.util.io

import android.content.Context
import de.tadris.fitness.Instance
import de.tadris.fitness.data.RouteSample
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.WorkoutPause
import de.tadris.fitness.recording.RecorderService
import de.tadris.fitness.recording.component.AutoPauseComponent
import de.tadris.fitness.util.calorie.CalorieCalculator
import kotlin.math.absoluteValue

/**
 * Currently only supports route/GPS and HR data (no repetition data).
 */
class ImportWorkoutSaver(
    private val context: Context,
    data: WorkoutData
) {

    var workout = data.workout
    var routeSamples = data.samples
    var hrSamples = data.heartRateSamples
    var pauses = data.pauses

    fun save(){
        dropDenseSamples()
        calculateIds()
        calculatePauses()
        calculateRelativeTimes()
        calculateSampleSpeed()
        calculateAggregatedValues()
        insertWorkoutIntoDatabase()
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

    private fun calculatePauses(){
        if(routeSamples.isEmpty()) return
        val minPauseTime = AutoPauseComponent.PAUSE_TIME * 2
        val pauses = mutableListOf<WorkoutPause>()

        var lastTimestamp = routeSamples.first().timestamp
        for(sample in routeSamples){
            if(sample.timestamp - lastTimestamp > minPauseTime){
                // insert pause here
                pauses.add(WorkoutPause(
                    workoutId = workout.id,
                    id = workout.id + pauses.size,
                    startDate = lastTimestamp,
                    endDate = sample.timestamp,
                    userInitiated = false,
                ))
            }
            lastTimestamp = sample.timestamp
        }

        this.pauses = pauses
    }

    private fun calculateRelativeTimes(){
        fun getPauseTimeBefore(timestamp: Long) = pauses.sumOf {
            when {
                it.endDate < timestamp -> it.duration // full
                it.startDate < timestamp -> timestamp - it.startDate // partial
                else -> 0L // none
            }
        }
        fun getRelativeTime(timestamp: Long) = timestamp - workout.start - getPauseTimeBefore(timestamp)

        routeSamples = routeSamples.map { it.copy(relativeTime = getRelativeTime(it.timestamp)) }
        hrSamples = hrSamples.map { it.copy(relativeTime = getRelativeTime(it.timestamp)) }
    }

    private fun calculateSampleSpeed(){
        if(routeSamples.isEmpty()) return
        val newSamples = mutableListOf<RouteSample>()
        var lastSample = routeSamples.first()
        for(sample in routeSamples){
            val timeDiffMs = sample.timestamp - lastSample.timestamp
            val speed = when {
                sample.speed != 0.0 -> sample.speed // speed value is already present
                timeDiffMs == 0L -> 0.0
                else -> {
                    val distance = sample.toLatLong().sphericalDistance(lastSample.toLatLong())
                    distance / (timeDiffMs / 1000.0) // m/s
                }
            }
            newSamples.add(sample.copy(speed = speed))
            lastSample = sample
        }

        routeSamples = newSamples
    }

    private fun calculateAggregatedValues(){
        workout = workout.copy(
            activeDuration = workout.totalDuration - pauses.sumOf { it.duration },
            distance = getDistance()
        )

        workout = workout.copy(
            energy = CalorieCalculator(context).calculateEnergy(context, getCurrentWorkoutData())
        )
    }

    private fun getDistance(): Double {
        if(routeSamples.isEmpty()) return 0.0
        var last = routeSamples.first()
        var distance = 0.0
        for(current in routeSamples){
            distance += last.toLatLong().sphericalDistance(current.toLatLong())
            last = current
        }
        return distance
    }

    private fun insertWorkoutIntoDatabase(){
        Instance.getInstance(context).db.workoutDao().insertWorkoutData(getCurrentWorkoutData())
    }

    private fun getCurrentWorkoutData() = WorkoutData(workout, routeSamples, pauses, emptyList(), emptyList(), hrSamples)



}