package de.tadris.fitness.data.migration

import android.content.Context
import de.tadris.fitness.Instance

class Migration16RunningWorkouts(context: Context, listener: MigrationListener) :
    Migration(context, listener) {

    override fun migrate() {
        val dao = Instance.getInstance(context).db.workoutDao()
        dao.findAllRunningWorkouts().forEach {
            dao.updateWorkout(it.copy(end = it.start))
        }
    }

}