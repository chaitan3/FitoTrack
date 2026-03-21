/*
 * Copyright (c) 2021 Jannis Scheibe <jannis@tadris.de>
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
package de.tadris.fitness.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface WorkoutDao {

    fun editWorkout(id: Long, editor: (Workout) -> Workout) {
        val workout = findById(id)!!
        updateWorkout(editor(workout))
    }

    @Query("select * from workout where `end` != 0")
    fun findRunningWorkout(): Workout?

    @Query("SELECT * FROM workout WHERE id = :id")
    fun findById(id: Long): Workout?

    @Query("SELECT * FROM route_sample WHERE id = :id")
    fun findSampleById(id: Long): RouteSample?

    @Query("SELECT * FROM route_sample WHERE workoutId = :workoutId")
    fun getAllSamplesOfWorkout(workoutId: Long): Array<RouteSample>

    @Query("SELECT * FROM workout_pause WHERE workoutId = :workoutId")
    fun getAllPausesOfWorkout(workoutId: Long): Array<WorkoutPause>

    @Query("SELECT * FROM workout_event WHERE workoutId = :workoutId")
    fun getEventsOfWorkout(workoutId: Long): Array<WorkoutEvent>

    @Query("SELECT * FROM repetition_sample WHERE workoutId = :workoutId")
    fun getAllRepetitionSamplesOfWorkout(workoutId: Long): Array<RepetitionSample>

    @Query("SELECT * FROM heart_rate_sample WHERE workoutId = :workoutId")
    fun getAllHeartRateSamplesOfWorkout(workoutId: Long): Array<HeartRateSample>

    @Query("SELECT * FROM workout ORDER BY start DESC")
    fun getWorkouts(): Array<Workout>

    @Query("SELECT * FROM workout ORDER BY start DESC LIMIT 1")
    fun getLastWorkout(): Workout?

    @Query("SELECT * FROM workout WHERE workoutTypeId = :type ORDER BY start DESC LIMIT 1")
    fun getLastWorkoutByType(type: String): Workout?

    @Query("SELECT * FROM workout ORDER BY start ASC")
    fun getAllWorkoutsHistorically(): Array<Workout>

    @Query("SELECT * FROM workout WHERE workoutTypeId = :workoutType ORDER BY start ASC ")
    fun getWorkoutsHistorically(workoutType: String?): Array<Workout>

    @Query("SELECT * FROM workout WHERE start = :start")
    fun getWorkoutByStart(start: Long): Workout?

    @Query("SELECT * FROM workout WHERE id = :id")
    fun getWorkoutById(id: Long): Workout?

    @Query("SELECT * FROM route_sample")
    fun getSamples(): Array<RouteSample>


    @Transaction
    fun insertWorkoutData(data: WorkoutData){
        insertWorkout(data.workout)
        for (sample in data.samples) {
            insertSample(sample)
        }

        for (pause in data.pauses) {
            insertPause(pause)
        }

        for (event in data.events) {
            insertWorkoutEvent(event)
        }

        for (repetitionSample in data.repetitionSamples) {
            insertSample(repetitionSample)
        }

        for (heartRateSample in data.heartRateSamples) {
            insertSample(heartRateSample)
        }
    }

    @Transaction
    fun updateWorkoutData(data: WorkoutData){
        updateWorkout(data.workout)
        for (sample in data.samples) {
            updateSample(sample)
        }

        for (pause in data.pauses) {
            updatePause(pause)
        }

        for (event in data.events) {
            updateWorkoutEvent(event)
        }

        for (repetitionSample in data.repetitionSamples) {
            updateSample(repetitionSample)
        }

        for (heartRateSample in data.heartRateSamples) {
            updateSample(heartRateSample)
        }
    }

    @Insert
    fun insertWorkoutAndSamples(workout: Workout, samples: Array<RouteSample>)

    @Delete
    fun deleteWorkoutAndSamples(workout: Workout, toArray: Array<RouteSample>)

    @Insert
    fun insertWorkout(workout: Workout)

    @Delete
    fun deleteWorkout(workout: Workout)

    @Update
    fun updateWorkout(workout: Workout)

    @Insert
    fun insertSample(sample: RouteSample)

    @Insert
    fun insertSample(sample: HeartRateSample)

    @Insert
    fun insertSample(sample: RepetitionSample)

    @Insert
    fun insertPause(pause: WorkoutPause)

    @Delete
    fun deleteSample(sample: RouteSample)

    @Update
    fun updateSamples(samples: Array<RouteSample>)

    @Update
    fun updateSample(sample: RouteSample)

    @Update
    fun updateSample(sample: HeartRateSample)

    @Update
    fun updateSample(sample: RepetitionSample)

    @Update
    fun updatePause(pause: WorkoutPause)

    @Update
    fun updateWorkoutEvent(event: WorkoutEvent)

    @Update
    fun updateRepetitionSample(sample: RepetitionSample)

    @Insert
    fun insertWorkoutEvent(event: WorkoutEvent)
}
