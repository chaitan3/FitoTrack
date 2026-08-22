/*
 * Copyright (c) 2026 Jannis Scheibe <jannis@tadris.de>
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
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    fun editWorkout(id: Long, editor: (Workout) -> Workout) {
        val workout = findById(id)!!
        updateWorkout(editor(workout))
    }

    @Query("select * from workout where `end` == -1")
    fun findRunningWorkout(): Workout?

    @Transaction
    fun resumeWorkout(workoutId: Long) {
        val workout = findById(workoutId) ?: return
        if (workout.end == -1L) return // already running, nothing to do

        val now = System.currentTimeMillis()
        val pauseStart = listOfNotNull(
            workout.end,
            workout.start,
            getLastSampleTimestamp(workoutId),
            getLastHeartRateSampleTimestamp(workoutId),
            getLastRepetitionSampleTimestamp(workoutId),
        ).max()
        if (pauseStart < now) {
            insertPause(WorkoutPause(workoutId, 0, pauseStart, now, true))
        }
        updateWorkout(workout.copy(end = -1L))
    }

    @Query("SELECT MAX(timestamp) FROM route_sample WHERE workoutId = :workoutId")
    fun getLastSampleTimestamp(workoutId: Long): Long?

    @Query("SELECT MAX(timestamp) FROM heart_rate_sample WHERE workoutId = :workoutId")
    fun getLastHeartRateSampleTimestamp(workoutId: Long): Long?

    @Query("SELECT MAX(timestamp) FROM repetition_sample WHERE workoutId = :workoutId")
    fun getLastRepetitionSampleTimestamp(workoutId: Long): Long?

    @Query("select * from workout where `end` == -1")
    fun findAllRunningWorkouts(): List<Workout>

    @Query("SELECT * FROM workout WHERE id = :id")
    fun findById(id: Long): Workout?

    @Query("SELECT * FROM route_sample WHERE id = :id")
    fun findSampleById(id: Long): RouteSample?

    @Query("SELECT * FROM route_sample WHERE workoutId = :workoutId order by relativeTime asc")
    fun getAllSamplesOfWorkout(workoutId: Long): Array<RouteSample>

    @Query("SELECT * FROM workout_pause WHERE workoutId = :workoutId order by startDate asc")
    fun getAllPausesOfWorkout(workoutId: Long): Array<WorkoutPause>

    @Query("SELECT * FROM workout_event WHERE workoutId = :workoutId order by relativeTime asc")
    fun getEventsOfWorkout(workoutId: Long): Array<WorkoutEvent>

    @Query("SELECT * FROM repetition_sample WHERE workoutId = :workoutId order by relativeTime asc")
    fun getAllRepetitionSamplesOfWorkout(workoutId: Long): Array<RepetitionSample>

    @Query("SELECT * FROM heart_rate_sample WHERE workoutId = :workoutId order by relativeTime asc")
    fun getAllHeartRateSamplesOfWorkout(workoutId: Long): Array<HeartRateSample>

    @Query("SELECT * FROM workout ORDER BY start DESC")
    fun getWorkouts(): List<Workout>

    @Query("select * from workout order by start desc")
    fun getWorkoutsFlow(): Flow<List<Workout>>

    @Query("SELECT * FROM workout ORDER BY start DESC LIMIT 1")
    fun getLastWorkout(): Workout?

    @Query("SELECT * FROM workout WHERE workoutTypeId = :type ORDER BY start DESC LIMIT 1")
    fun getLastWorkoutByType(type: String): Workout?

    @Query("SELECT * FROM workout WHERE start = :start")
    fun getWorkoutByStart(start: Long): Workout?

    @Query("SELECT * FROM workout WHERE id = :id")
    fun getWorkoutById(id: Long): Workout?

    @Query("SELECT COUNT(*) FROM workout")
    fun countWorkouts(): Long

    @Query("SELECT COUNT(*) FROM route_sample")
    fun countRouteSamples(): Long

    @Query("SELECT COUNT(*) FROM repetition_sample")
    fun countRepetitionSamples(): Long

    @Query("SELECT COUNT(*) FROM heart_rate_sample")
    fun countHeartRateSamples(): Long

    @Query("SELECT COUNT(*) FROM workout_event")
    fun countWorkoutEvents(): Long

    @Query("SELECT COUNT(*) FROM workout_pause")
    fun countWorkoutPauses(): Long


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

        // Pauses are recalculated with fresh IDs so replace
        deletePausesOfWorkout(data.workout.id)
        for (pause in data.pauses) {
            insertPause(pause)
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

    @Delete
    fun deleteSample(sample: HeartRateSample)

    @Delete
    fun deleteSample(sample: RepetitionSample)

    @Delete
    fun deleteWorkoutEvent(event: WorkoutEvent)

    @Update
    fun updateSamples(samples: List<RouteSample>)

    @Update
    fun updateSample(sample: RouteSample)

    @Update
    fun updateSample(sample: HeartRateSample)

    @Update
    fun updateSample(sample: RepetitionSample)

    @Query("DELETE FROM workout_pause WHERE workoutId = :workoutId")
    fun deletePausesOfWorkout(workoutId: Long)

    @Update
    fun updateWorkoutEvent(event: WorkoutEvent)

    @Update
    fun updateRepetitionSample(sample: RepetitionSample)

    @Insert
    fun insertWorkoutEvent(event: WorkoutEvent)
}
