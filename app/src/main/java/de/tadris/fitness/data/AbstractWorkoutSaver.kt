package de.tadris.fitness.data

import android.content.Context
import de.tadris.fitness.Instance
import de.tadris.fitness.recording.component.AutoPauseComponent
import de.tadris.fitness.util.calorie.CalorieCalculator

abstract class AbstractWorkoutSaver(
    protected val context: Context,
    data: WorkoutData
) {

    var workout = data.workout
    var routeSamples = data.samples
    var hrSamples = data.heartRateSamples
    var pauses = data.pauses

    abstract fun save()

    protected fun calculatePauses(){
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

    protected fun calculateRelativeTimes(){
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

    protected fun calculateSampleSpeed(){
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

    protected fun calculateAggregatedValues(){
        workout = workout.copy(
            activeDuration = workout.totalDuration - pauses.sumOf { it.duration },
            distance = getDistance()
        )

        workout = workout.copy(
            energy = CalorieCalculator(context).calculateEnergy(context, getCurrentWorkoutData())
        )
    }

    protected fun getDistance(): Double {
        if(routeSamples.isEmpty()) return 0.0
        var last = routeSamples.first()
        var distance = 0.0
        for(current in routeSamples){
            distance += last.toLatLong().sphericalDistance(current.toLatLong())
            last = current
        }
        return distance
    }

    protected fun insertWorkoutIntoDatabase(){
        Instance.getInstance(context).db.workoutDao().insertWorkoutData(getCurrentWorkoutData())
    }

    protected fun updateWorkoutAndSamples(){
        Instance.getInstance(context).db.workoutDao().updateWorkoutData(getCurrentWorkoutData())
    }

    protected fun getCurrentWorkoutData() = WorkoutData(workout, routeSamples, pauses, emptyList(), emptyList(), hrSamples)



}