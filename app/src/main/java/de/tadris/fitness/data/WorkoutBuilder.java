/*
 * Copyright (c) 2023 Jannis Scheibe <jannis@tadris.de>
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

package de.tadris.fitness.data;

import android.content.Context;

import java.util.Calendar;

import de.tadris.fitness.Instance;
import de.tadris.fitness.data.preferences.UserMeasurements;
import de.tadris.fitness.util.calorie.CalorieCalculator;

public class WorkoutBuilder {

    private WorkoutType workoutType;

    private Calendar start;
    private long duration;

    private int length;

    private String comment;

    private Workout existingWorkout;
    private boolean fromExistingWorkout = false;

    public WorkoutBuilder(Context context) {
        workoutType = WorkoutTypeManager.getInstance().getWorkoutTypeById(context, WorkoutTypeManager.WORKOUT_TYPE_ID_RUNNING);
        start = Calendar.getInstance();
        duration = 1000L * 60 * 10;
        length = 500;
        comment = "";
    }

    public Workout create(Context context) {
        long pauseDuration = fromExistingWorkout ? existingWorkout.getPauseDuration() : 0L;
        return new Workout(
                fromExistingWorkout ? existingWorkout.getId() : start.getTimeInMillis(),
                start.getTimeInMillis(),
                start.getTimeInMillis() + duration + pauseDuration,
                duration,
                comment,
                length,
                new CalorieCalculator(context).calculateCalories(UserMeasurements.from(context), workout),
                workoutType.id,
                fromExistingWorkout ? existingWorkout.getIntervalSetUsedId() : 0
        );
    }

    public GpsWorkout saveWorkout(Context context) {
        GpsWorkout workout = create(context);
        if (fromExistingWorkout) {
            updateWorkout(context, workout);
        } else {
            insertWorkout(context, workout);
        }
        return workout;
    }

    private void updateWorkout(Context context, GpsWorkout workout) {
        Instance.getInstance(context).db.workoutDao().updateWorkout(workout);
    }

    private void insertWorkout(Context context, GpsWorkout workout) {
        Instance.getInstance(context).db.workoutDao().insertWorkout(workout);
    }

    public WorkoutType getWorkoutType() {
        return workoutType;
    }

    public void setWorkoutType(WorkoutType workoutType) {
        this.workoutType = workoutType;
    }

    public Calendar getStart() {
        return start;
    }

    public void setStart(Calendar start) {
        this.start = start;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setWasEdited() {
        wasEdited = true;
    }

    public boolean isFromExistingWorkout() {
        return fromExistingWorkout;
    }

    public static WorkoutBuilder fromWorkout(Context context, GpsWorkout workout) {
        WorkoutBuilder builder = new WorkoutBuilder(context);
        builder.fromExistingWorkout = true;
        builder.existingWorkout = workout;
        builder.wasEdited = workout.edited;
        builder.workoutType = workout.getWorkoutType(context);
        builder.start.setTimeInMillis(workout.start);
        builder.duration = workout.duration;
        builder.length = workout.length;
        builder.comment = workout.comment;

        return builder;
    }
}
