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

package de.tadris.fitness.recording

import android.content.Context
import de.tadris.fitness.data.WorkoutDao
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.util.calorie.CalorieCalculator
import de.tadris.fitness.util.totalDistance

class WorkoutFinalizer(val context: Context, val workoutDao: WorkoutDao, val workoutId: Long) {

    private val workout =
        workoutDao.getWorkoutById(workoutId) ?: throw Exception("Workout not found")
    private val data = WorkoutData.fromWorkout(workoutDao, workout)

    fun finalize() {
        workoutDao.updateWorkout(
            workout.copy(
                activeDuration =,
                distance = data.samples.totalDistance,
                repetitions = data.repetitionSamples.sumOf { it.repetitions },
                energy = getEnergy(),
            )
        )
    }

    fun recalculateElevation() {

    }

    fun updateMSLValues() {

    }

    private fun getActiveDuration(): Long {
        var activeDuration = workout.end - workout.start
        workoutDao.getAllPausesOfWorkout(workoutId).forEach {
            activeDuration -= it.endDate - it.startDate
        }
        return activeDuration
    }

    private fun getDistance(): Double {
        return workoutDao.getAllSamplesOfWorkout(workoutId).toList().totalDistance
    }

    private fun getEnergy() {
        CalorieCalculator(context).calculateCalories()
    }

}