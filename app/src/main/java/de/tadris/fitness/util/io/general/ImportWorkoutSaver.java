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

package de.tadris.fitness.util.io.general;

import static de.tadris.fitness.recording.WorkoutRecorder.MIN_DURATION_DIFF;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import de.tadris.fitness.data.RouteSample;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.recording.WorkoutRecorder;
import de.tadris.fitness.recording.gps.WorkoutSaver;

public class ImportWorkoutSaver extends WorkoutSaver {

    public ImportWorkoutSaver(Context context, WorkoutData data) {
        super(context, data);
    }

    public void saveWorkout() {
        // Init
        setIds();

        // Clean up geo data
        eliminateDenseSamples();
        calculateRelativeTimes();

        // Update sample data
        setMSLElevationToElevation();
        setSpeed();

        // Update aggregated data
        setStartAndEnd();
        calculateData(false);

        // Save
        storeInDatabase();
    }

    private void setMSLElevationToElevation() {
        // Usually the elevation used in GPX files is already the median sea level
        for (RouteSample sample : samples) {
            sample.elevationMSL = sample.elevation;
        }
    }

    /**
     * Calculate speed values for each sample if not available
     */
    private void setSpeed() {
        setTopSpeed();
        if (samples.size() == 0) {
            return;
        }
        if (workout.topSpeed != 0) {
            // Speed values already present
            return;
        }
        RouteSample lastSample = samples.get(0);
        for (RouteSample sample : samples) {
            double distance = lastSample.toLatLong().sphericalDistance(sample.toLatLong());
            long timeDiff = sample.absoluteTime - lastSample.absoluteTime;
            if (timeDiff != 0) {
                sample.speed = distance / ((double) timeDiff / 1000);
            }
            lastSample = sample;
        }
    }

    /**
     * Eliminate samples that are too close to each other using WorkoutType.minDistance or 500ms
     */
    private void eliminateDenseSamples() {
        int minDistance = workout.getWorkoutType(context).minDistance;

        List<RouteSample> removedSamples = new ArrayList<>();

        RouteSample lastSample = null;
        for (RouteSample sample : samples) {
            if (lastSample == null) {
                lastSample = sample;
                continue;
            }

            double distance = Math.abs(sample.toLatLong().sphericalDistance(lastSample.toLatLong()));
            long timediff = Math.abs(sample.absoluteTime - lastSample.absoluteTime);
            if (distance < minDistance || timediff < MIN_DURATION_DIFF) {
                removedSamples.add(sample); // this sample is dropped
            }
        }

        samples.removeAll(removedSamples);
    }

    /**
     * Replays the workout to calculate correct relative times (and thus pauses)
     */
    private void calculateRelativeTimes() {
        if (samples.isEmpty()) return;

        long absoluteTime = samples.get(0).absoluteTime;
        long relativeTime = 0L;

        for (RouteSample sample : samples) {
            long timeDiffToLastSample = sample.absoluteTime - absoluteTime;
            if (timeDiffToLastSample > WorkoutRecorder.PAUSE_TIME * 2) {
                // insert pause here
                absoluteTime = sample.absoluteTime;
            } else {
                // do nothing
                absoluteTime = sample.absoluteTime;
                relativeTime += timeDiffToLastSample;
            }

            sample.relativeTime = relativeTime;
        }
    }

}
