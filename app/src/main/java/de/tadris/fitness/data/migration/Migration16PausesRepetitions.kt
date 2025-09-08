package de.tadris.fitness.data.migration

import android.content.Context
import de.tadris.fitness.Instance
import de.tadris.fitness.data.BaseSample
import de.tadris.fitness.data.RepetitionSample
import de.tadris.fitness.data.Workout
import de.tadris.fitness.data.WorkoutDao
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.WorkoutPause
import de.tadris.fitness.data.WorkoutTypeManager

class Migration16PausesRepetitions(context: Context, listener: MigrationListener) :
    Migration(context, listener) {

    companion object {
        private const val MIN_DIFF = 2000
    }

    override fun migrate() {
        val dao = Instance.getInstance(context).db.workoutDao()
        val workouts = dao.getWorkouts()
        workouts.forEachIndexed { index, workout ->
            val data = WorkoutData.fromWorkout(dao, workout)
            if(data.hasRouteData){
                checkSamples(dao, workout, data.samples)
            }else if(data.hasRepetitions){
                checkSamples(dao, workout, data.repetitionSamples)
            }
            migrateRepetitionTypes(dao, data)
            listener.onProgressUpdate(index * 100 / workouts.size)
        }
    }

    private fun checkSamples(dao: WorkoutDao, workout: Workout, samples: List<BaseSample>){
        var currentDiff = samples.first().let { it.timestamp - it.relativeTime }
        for(sample in samples){
            val sampleDiff = sample.timestamp - sample.relativeTime
            if(sampleDiff - currentDiff > MIN_DIFF){
                val endDate = sample.timestamp
                val startDate = endDate - (sampleDiff - currentDiff)
                dao.insertPause(WorkoutPause(
                    workout.id, startDate, startDate,
                    endDate, false
                ))
                currentDiff = sampleDiff
            }
        }
    }

    private fun migrateRepetitionTypes(dao: WorkoutDao, data: WorkoutData){
        for(sample in data.repetitionSamples){
            val typeId = when(data.workout.workoutTypeId){
                WorkoutTypeManager.WORKOUT_TYPE_ID_TREADMILL -> RepetitionSample.REPETITION_TYPE_STEP
                WorkoutTypeManager.WORKOUT_TYPE_ID_ROPE_SKIPPING,
                WorkoutTypeManager.WORKOUT_TYPE_ID_TRAMPOLINE_JUMPING -> RepetitionSample.REPETITION_TYPE_JUMP
                WorkoutTypeManager.WORKOUT_TYPE_ID_PUSH_UPS -> RepetitionSample.REPETITION_TYPE_PUSH_UP
                WorkoutTypeManager.WORKOUT_TYPE_ID_PULL_UPS -> RepetitionSample.REPETITION_TYPE_PULL_UP
                else -> RepetitionSample.REPETITION_TYPE_UNKNOWN
            }
            dao.updateRepetitionSample(sample.copy(repetitionTypeId = typeId))
        }
    }

}