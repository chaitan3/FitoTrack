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
import android.hardware.SensorManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.tadris.fitness.Instance;
import de.tadris.fitness.data.AppDatabase;
import de.tadris.fitness.data.RouteSample;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.data.preferences.UserMeasurements;
import de.tadris.fitness.util.AltitudeCorrection;
import de.tadris.fitness.util.WorkoutCalculator;
import de.tadris.fitness.util.calorie.CalorieCalculator;

/**
 * Calculates data for a workout+samples and saves everything to the database.
 */
public class WorkoutSaver {

    protected final Context context;
    protected final GpsWorkout workout;
    protected final List<RouteSample> samples;
    protected final AppDatabase db;

    public WorkoutSaver(Context context, WorkoutData data) {
        this.context = context;
        this.workout = data.workout;
        this.samples = data.samples;
        this.db = Instance.getInstance(context).db;
    }

    public void finalizeWorkout() {
        clearSamplesWithSameTime(true);

        calculateData(true);

        updateWorkoutAndSamples();
    }

    public void discardWorkout() {
        deleteWorkoutAndSamples();
    }

    public synchronized void addSample(RouteSample sample) {
        if (samples.size() == 0) {
            sample.id = this.workout.id + this.samples.size();
        } else {
            sample.id = getLastSample().id + 1;
        }
        sample.workoutId = this.workout.id;
        db.workoutDao().insertSample(sample);
    }

    private RouteSample getLastSample() {
        return samples.get(samples.size() - 1);
    }

    public void saveWorkout() {
        setIds();

        clearSamplesWithSameTime(false);
        calculateData(true);

        storeInDatabase();
    }

    protected void setIds() {
        workout.id = System.nanoTime();
        int i = 0;
        for (RouteSample sample : samples) {
            i++;
            sample.id = workout.id + i;
            sample.workoutId = workout.id;
        }
    }

    protected void calculateData(boolean calculateElevation) {
        calculateDurations(); // set duration/pauses
        setLength();
        setTopSpeed();

        setHeartRate();

        if (calculateElevation) {
            setElevation();
        }
        setAscentAndDescentMinMax();

        setCalories();
    }

    private void clearSamplesWithSameTime(boolean delete) {
        for (int i = samples.size() - 2; i >= 0; i--) {
            RouteSample sample = samples.get(i);
            RouteSample lastSample = samples.get(i + 1);
            if (sample.absoluteTime == lastSample.absoluteTime) {
                samples.remove(lastSample);
                if (delete) {
                    db.workoutDao().deleteSample(lastSample); // delete sample also from DB
                }
                Log.i("WorkoutManager", "Removed samples at " + sample.absoluteTime + " rel: " + sample.relativeTime + "; " + lastSample.relativeTime);
            }
        }
    }

    protected void setLength() {
        double length = 0;
        for (int i = 1; i < samples.size(); i++) {
            RouteSample currentSample = samples.get(i);
            RouteSample lastSample = samples.get(i - 1);
            double sampleLength = lastSample.toLatLong().sphericalDistance(currentSample.toLatLong());
            length += sampleLength;
        }
        workout.length = (int) length;
        workout.avgSpeed = ((double) workout.length) / ((double) workout.duration / 1000);
        workout.avgPace = ((double) workout.duration / 1000 / 60) / ((double) workout.length / 1000);
    }

    protected void setTopSpeed() {
        double topSpeed = 0;
        for (RouteSample sample : samples) {
            if (sample.speed > topSpeed) {
                topSpeed = sample.speed;
            }
        }
        workout.topSpeed = topSpeed;
    }

    protected void setHeartRate() {
        int heartRateSum = 0;
        int maxHeartRate = -1;
        for (RouteSample sample : samples) {
            heartRateSum += sample.heartRate;
            if (sample.heartRate > maxHeartRate) {
                maxHeartRate = sample.heartRate;
            }
        }
        workout.maxHeartRate = maxHeartRate;
        workout.avgHeartRate = heartRateSum / samples.size();
    }

    private void setElevation() {
        setPressureElevation();
        setRoundedSampleElevation();
        setMSLElevation();
    }

    private void setPressureElevation() {
        boolean pressureDataAvailable = samples.get(0).pressure != -1;

        if (!pressureDataAvailable) {
            // Because pressure data isn't available we just use the use GPS elevation
            // in WorkoutSample.elevation which was already set
            return;
        }

        double avgElevation = getAverageElevation();
        double avgPressure = getAveragePressure();

        for (int i = 0; i < samples.size(); i++) {
            RouteSample sample = samples.get(i);

            // Altitude Difference to Average Elevation in meters
            float altitude_difference =
                    SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, sample.pressure) -
                            SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, (float) avgPressure);
            if (!Double.isNaN(altitude_difference)) {
                sample.elevation = avgElevation + altitude_difference;
            } else {
                Log.w("WorkoutSaver", "Cannot determine elevation for sample " + i + ": pressure=" + sample.pressure);
            }
        }
    }

    private double getAverageElevation() {
        return getAverageElevation(samples);
    }

    private double getAverageElevation(List<RouteSample> samples) {
        double elevationSum = 0; // Sum of elevation
        for (RouteSample sample : samples) {
            elevationSum += sample.elevation;
        }

        return elevationSum / samples.size();
    }

    private double getAveragePressure() {
        double pressureSum = 0;
        for (RouteSample sample : samples) {
            pressureSum += sample.pressure;
        }
        return pressureSum / samples.size();
    }

    private void setRoundedSampleElevation() {
        // Should only be done on newly recorded samples
        roundSampleElevation();
        for (RouteSample sample : samples) {
            sample.elevation = sample.tmpElevation;
        }
    }

    private void roundSampleElevation() {
        int range = 7;
        for (int i = 0; i < samples.size(); i++) {
            int minIndex = Math.max(i - range, 0);
            int maxIndex = Math.min(i + range, samples.size() - 1);
            samples.get(i).tmpElevation = getAverageElevation(samples.subList(minIndex, maxIndex));
        }
    }

    protected void setMSLElevation() {
        // Set the median sea level elevation value for all samples
        // Please see the AltitudeCorrection.java for more information
        try {
            int lat = (int) Math.round(samples.get(0).latitude);
            int lon = (int) Math.round(samples.get(0).longitude);
            AltitudeCorrection correction = new AltitudeCorrection(context, lat, lon);
            for (RouteSample sample : samples) {
                sample.elevationMSL = correction.getHeightOverSeaLevel(sample.elevation);
            }
        } catch (IOException e) {
            // If we can't read the file, we cannot correct the values
            e.printStackTrace();
        }
    }

    protected void setAscentAndDescentMinMax() {
        workout.ascent = 0f;
        workout.descent = 0f;

        // Eliminate noise
        roundSampleElevation();

        // Now sum up the ascent/descent
        if (samples.size() > 1) {
            RouteSample firstSample = samples.get(0);
            workout.minElevationMSL = (float) firstSample.elevationMSL;
            workout.maxElevationMSL = (float) firstSample.elevationMSL;

            RouteSample prevSample = firstSample;
            for (int i = 1; i < samples.size(); i++) {
                RouteSample sample = samples.get(i);

                workout.minElevationMSL = Math.min(workout.minElevationMSL, (float) sample.elevationMSL);
                workout.maxElevationMSL = Math.max(workout.maxElevationMSL, (float) sample.elevationMSL);

                // Use rounded sample elevation
                double diff = sample.tmpElevation - prevSample.tmpElevation;

                if (Double.isNaN(diff)) {
                    Log.e("WorkoutSaver", "ElevationDiff is NaN fallback to 0");
                    diff = 0d;
                }
                if (diff > 0) {
                    // If this sample is higher than the last one, add difference to ascent
                    workout.ascent += diff;
                } else {
                    // If this sample is lower than the last one, add difference to descent
                    workout.descent += Math.abs(diff);
                }
                prevSample = sample;
            }
        }
    }

    protected void setStartAndEnd() {
        workout.start = samples.get(0).absoluteTime;
        workout.end = samples.get(samples.size() - 1).absoluteTime;
    }

    private void calculateDurations() {
        if (samples.isEmpty()) {
            return;
        }
        workout.pauseDuration = WorkoutCalculator.calculatePauseDuration(getBaseWorkoutData());
        workout.duration = workout.end - workout.start - workout.pauseDuration;
    }

    protected void setCalories() {
        // Ascent has to be set previously
        workout.calorie = new CalorieCalculator(context).calculateCalories(UserMeasurements.from(context), workout);
    }

    protected WorkoutData getWorkoutData() {
        return new WorkoutData(workout, samples);
    }

    protected BaseWorkoutData getBaseWorkoutData() {
        return new BaseWorkoutData(workout, new ArrayList<>(samples));
    }

    protected void storeInDatabase() {
        db.workoutDao().insertWorkoutAndSamples(workout, samples.toArray(new RouteSample[0]));
    }

    protected void storeWorkoutInDatabase() {
        db.workoutDao().insertWorkout(workout);
    }

    protected void updateWorkoutAndSamples() {
        updateWorkoutInDatabase();
        updateSamples();
    }

    protected void updateSamples() {
        db.workoutDao().updateSamples(samples.toArray(new RouteSample[0]));
    }

    protected void updateWorkoutInDatabase() {
        db.workoutDao().updateWorkout(workout);
    }

    protected void deleteWorkoutAndSamples() {
        db.workoutDao().deleteWorkoutAndSamples(workout, samples.toArray(new RouteSample[0]));
    }
}
