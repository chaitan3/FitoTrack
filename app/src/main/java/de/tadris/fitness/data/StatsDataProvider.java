/*
 * Copyright (c) 2022 Jannis Scheibe <jannis@tadris.de>
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

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.tadris.fitness.Instance;
import de.tadris.fitness.util.WorkoutProperty;
import de.tadris.fitness.util.exceptions.NoDataException;

public class StatsDataProvider {

    private final Context context;

    public StatsDataProvider(Context context) {
        this.context = context;
    }

    public StatsDataTypes.DataPoint getFirstData(WorkoutProperty requestedProperty, List<WorkoutType> workoutTypes) throws NoDataException {
        ArrayList<StatsDataTypes.DataPoint> points = getData(requestedProperty, workoutTypes);
        if (points.isEmpty())
            throw new NoDataException();
        return points.get(0);
    }

    public StatsDataTypes.DataPoint getLastData(WorkoutProperty requestedProperty, List<WorkoutType> workoutTypes) throws NoDataException {
        ArrayList<StatsDataTypes.DataPoint> points = getData(requestedProperty, workoutTypes);
        if (points.size() == 0)
            throw new NoDataException();
        return points.get(points.size() - 1);
    }

    public ArrayList<StatsDataTypes.DataPoint> getData(WorkoutProperty requestedProperty, List<WorkoutType> workoutTypes) {
        return getData(requestedProperty, workoutTypes, null);
    }

    public ArrayList<StatsDataTypes.DataPoint> getData(WorkoutProperty requestedProperty, List<WorkoutType> workoutTypes, @Nullable StatsDataTypes.TimeSpan timeSpan) {
        ArrayList<StatsDataTypes.DataPoint> data = new ArrayList<>();
        List<Workout> workouts = Instance.getInstance(context).db.getAllWorkouts();
        for (Workout workout : workouts) {
            if ((timeSpan == null) || timeSpan.contains(workout.start)) {
                WorkoutType type = workout.getWorkoutType(context);
                if (workoutTypes.contains(type)) { // in separate if cause getWorkoutType is sometimes costly
                    if (requestedProperty.getType().canBeApplied(workout)) { // check if property can provide data for workout
                        try {
                            double val = getPropertyValue(requestedProperty, workout);
                            data.add(new StatsDataTypes.DataPoint(type, workout.id, workout.start, val));
                        } catch (Exception e) {
                            // This should never happen, cause it is checked by the if clauses above
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        Collections.sort(data, StatsDataTypes.DataPoint.timeComparator);
        return data;
    }

    private double getPropertyValue(WorkoutProperty property, Workout workout) throws Exception {
        switch (property.getType()) {
            case BASE:
                return getBasePropertyValue(property, workout);
            case GPS:
                return getGPSPropertyValue(property, (GpsWorkout) workout);
            case INDOOR:
                return getIndoorPropertyValue(property, (IndoorWorkout) workout);
            default:
                throw new IllegalArgumentException("Property type unknown");
        }
    }

    private double getBasePropertyValue(WorkoutProperty property, Workout workout) throws Exception {
        switch (property) {
            case NUMBER:
                return 1;
            case START:
                return workout.start;
            case END:
                return workout.end;
            case DURATION:
                return workout.duration;
            case PAUSE_DURATION:
                return workout.pauseDuration;
            case AVG_HEART_RATE:
                return workout.avgHeartRate;
            case MAX_HEART_RATE:
                return workout.maxHeartRate;
            case CALORIE:
                return workout.calorie;
            default:
                throw new Exception("Property is no BaseProperty");
        }
    }

    private double getGPSPropertyValue(WorkoutProperty property, GpsWorkout workout) throws Exception {
        switch (property) {
            case LENGTH:
                return workout.length;
            case AVG_SPEED:
                return workout.avgSpeed;
            case TOP_SPEED:
                return workout.topSpeed;
            case AVG_PACE:
                return workout.avgPace;
            case ASCENT:
                return workout.ascent;
            case DESCENT:
                return workout.descent;
            default:
                throw new Exception("Property is no GPS Property");
        }
    }

    private double getIndoorPropertyValue(WorkoutProperty property, IndoorWorkout workout) throws Exception {
        switch (property) {
            case AVG_FREQUENCY:
                return workout.avgFrequency;
            case MAX_FREQUENCY:
                return workout.maxFrequency;
            case MAX_INTENSITY:
                return workout.maxIntensity;
            case AVG_INTENSITY:
                return workout.avgIntensity;
            default:
                throw new Exception("Property is no Indoor Property");
        }
    }
}