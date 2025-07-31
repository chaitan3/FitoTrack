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

package de.tadris.fitness.recording.gps;

import android.content.Context;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import de.tadris.fitness.data.RouteSample;
import de.tadris.fitness.data.WorkoutData;

public class WorkoutCutter extends WorkoutSaver {

    public WorkoutCutter(Context context, WorkoutData data) {
        super(context, data);
    }

    public void cutWorkout(@Nullable RouteSample startSample, @Nullable RouteSample endSample) {
        if (startSample != null) {
            cutStart(startSample);
        }
        if (endSample != null) {
            cutEnd(endSample);
        }
        setStartAndEnd(); // Start/End might change by cutting
        calculateData(false); // Recalculate data

        updateWorkoutAndSamples();
    }

    private void cutStart(RouteSample startSample) {
        for (RouteSample sample : new ArrayList<>(samples)) {
            if (sample.id == startSample.id) {
                break;
            } else {
                deleteSample(sample);
            }
        }
        // Move relative times
        long startTime = startSample.relativeTime;
        for (RouteSample sample : samples) {
            sample.relativeTime -= startTime;
        }
    }

    private void cutEnd(RouteSample endSample) {
        boolean found = false;
        for (RouteSample sample : new ArrayList<>(samples)) {
            if (found) {
                deleteSample(sample);
            } else if (sample.id == endSample.id) {
                found = true;
            }
        }
    }

    private void deleteSample(RouteSample sample) {
        samples.remove(sample);
        db.workoutDao().deleteSample(sample);
    }

}
