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
import de.tadris.fitness.data.preferences.UserMeasurements
import de.tadris.fitness.util.calorie.CalorieCalculator
import de.tadris.fitness.util.totalDistance
import kotlin.math.roundToInt

class Migration16Energy(context: Context, listener: MigrationListener) :
    Migration(context, listener) {

    override fun migrate() {
        val dao = Instance.getInstance(context).db.workoutDao()
        val workouts = dao.getWorkouts()
        workouts.forEachIndexed { index, workout ->
            val data = WorkoutData.fromWorkout(dao, workout)
            val energy = CalorieCalculator(context).calculateEnergy(context, data)
            dao.updateWorkout(workout.copy(energy = energy))
            listener.onProgressUpdate(index * 100 / workouts.size)
        }
    }

}