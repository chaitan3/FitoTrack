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

package de.tadris.fitness.recording;

import android.content.Context;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import de.tadris.fitness.Instance;
import de.tadris.fitness.data.Interval;
import de.tadris.fitness.data.IntervalSet;
import de.tadris.fitness.data.RouteSample;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.data.preferences.UserMeasurements;
import de.tadris.fitness.data.preferences.UserPreferences;
import de.tadris.fitness.recording.component.HeartRateComponent;
import de.tadris.fitness.recording.event.HRBatteryLevelChangeEvent;
import de.tadris.fitness.recording.event.HRBatteryLevelConnectionEvent;
import de.tadris.fitness.recording.event.HeartRateChangeEvent;
import de.tadris.fitness.recording.event.HeartRateConnectionChangeEvent;
import de.tadris.fitness.recording.event.LocationChangeEvent;
import de.tadris.fitness.recording.event.PressureChangeEvent;
import de.tadris.fitness.recording.gps.WorkoutSaver;
import de.tadris.fitness.ui.record.RecordGpsWorkoutActivity;
import de.tadris.fitness.ui.record.RecordWorkoutActivity;
import de.tadris.fitness.util.WorkoutLogger;
import de.tadris.fitness.util.calorie.CalorieCalculator;

/**
 * This class/subclasses is responsible for managing the workout data during a workout recording
 * - receive new samples
 * - save them to the database
 * - provide useful data like current speed, distance, duration, etc
 * - manage the workout state
 * <p>
 * It gets locations, pressure data, etc. from the RecorderService via the EventBus
 */
public class WorkoutRecorder {


    protected final Context context;

    protected RecordingState state;
    protected long startTime = 0;
    protected long time = 0;
    protected long pauseTime = 0;
    protected long lastResume;
    protected long lastPause = 0;
    protected long lastSampleTime = 0;

    protected List<Interval> intervalList;

    protected int lastHeartRate = -1;
    protected int lastHRBatteryLevel = -1;

    // Temporarily saved the last interval that was triggered.
    // It will be added to the next recorded sample.
    protected long lastTriggeredInterval = -1;

    private final GpsWorkout workout;
    private final List<RouteSample> samples = new ArrayList<>();
    private final WorkoutSaver workoutSaver;
    private double distance = 0;

    private boolean saved = false;

    private float lastPressure = -1;

    private boolean useAverageForCurrentSpeed;
    private int currentSpeedAverageTime;

    public WorkoutRecorder(Context context) {
        WorkoutLogger.log("WorkoutRecorder", "Creating workout recorder");
        this.context = context;
        this.state = RecordingState.IDLE;

        EventBus.getDefault().register(this);
        UserPreferences prefs = Instance.getInstance(context).userPreferences;
        this.useAutoPause = prefs.getUseAutoPause();
        this.autoTimeoutMs = prefs.getAutoTimeout() * AUTO_TIMEOUT_MULTIPLIER;

        UserPreferences preferences = Instance.getInstance(context).userPreferences;

        this.workout = new GpsWorkout();
        workout.edited = false;

        // Default values
        this.workout.comment = "";

        this.workout.setWorkoutType(workoutType);

        workoutSaver = new WorkoutSaver(this.context, getWorkoutData());

        useAverageForCurrentSpeed = preferences.getUseAverageForCurrentSpeed();
        currentSpeedAverageTime = preferences.getTimeForCurrentSpeed() * 1000;
    }

    public void start(String reason) {
        WorkoutLogger.log("Recorder", "Called start, reason: " + reason);
        if (state == RecordingState.IDLE) {
            WorkoutLogger.log("Recorder", "Start");
            startTime = System.currentTimeMillis();
            onStart();
            resume();
        } else if (state == RecordingState.PAUSED) {
            resume();
        } else if (state != RecordingState.RUNNING) {
            throw new IllegalStateException("Cannot start or resume recording. state = " + state);
        }
    }

    public void resume() {
        WorkoutLogger.log("Recorder", "Resume");
        state = RecordingState.RUNNING;
        lastResume = System.currentTimeMillis();
        if (lastPause != 0) {
            pauseTime += System.currentTimeMillis() - lastPause;
        }
    }

    public void pause() {
        if (state == RecordingState.RUNNING) {
            WorkoutLogger.log("Recorder", "Pause");
            state = RecordingState.PAUSED;
            time += System.currentTimeMillis() - lastResume;
            lastPause = System.currentTimeMillis();
        }
    }

    public void stop(String reason) {
        WorkoutLogger.log("Recorder", "Stopping workout, reason: " + reason);
        if (state == RecordingState.PAUSED) {
            resume();
        }
        pause();
        onStop();
        state = RecordingState.STOPPED;
        EventBus.getDefault().unregister(this);
    }

    public boolean hasRecordedSomething() {
        return getSampleSize() > 2;
    }

    public void setComment(String comment) {
        getWorkout().comment = comment;
    }

    public void setUsedIntervalSet(IntervalSet set) {
        getWorkout().intervalSetUsedId = set.id;
    }

    public GpsWorkout getWorkout() {
        return this.workout;
    }

    public GpsState getGpsState() {
        return this.gpsState;
    }

    public List<RouteSample> getSamples() {
        return this.samples;
    }

    public WorkoutData getWorkoutData() {
        return new WorkoutData(getWorkout(), getSamples());
    }

    @Override
    protected void onStart() {
        workout.id = System.nanoTime();
        workout.start = System.currentTimeMillis();
        //Init Workout To Be able to Save
        workout.end = -1L;
        workout.avgSpeed = -1d;
        workout.topSpeed = -1d;
        workout.ascent = -1f;
        workout.descent = -1f;
        workoutSaver.storeWorkoutInDatabase(); // Already Persist Workout
    }

    @Override
    public int getSampleSize() {
        return samples.size();
    }

    @Override
    public Class<? extends RecordWorkoutActivity> getActivityClass() {
        return RecordGpsWorkoutActivity.class;
    }

    @Override
    protected void onStop() {
        workout.end = System.currentTimeMillis();
        workout.duration = time;
        workout.pauseDuration = pauseTime;
        WorkoutLogger.log("Recorder", "Stop with " + getSampleCount() + " Samples");
    }

    public void save() {
        if (state != RecordingState.STOPPED) {
            throw new IllegalStateException("Cannot save recording, recorder was not stopped. state = " + state);
        }
        WorkoutLogger.log("Recorder", "Save");
        synchronized (samples) {
            workoutSaver.finalizeWorkout();
        }
        Instance.getInstance(context).planner.onWorkoutRecorded(workout);
        saved = true;
    }

    public boolean isSaved() {
        return saved;
    }

    public int getSampleCount() {
        synchronized (samples) {
            return samples.size();
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onLocationChange(LocationChangeEvent e) {

    }



    private RouteSample getLastSample() {
        synchronized (samples) {
            if (samples.size() > 0) {
                return samples.get(samples.size() - 1);
            } else {
                return null;
            }
        }
    }

    public int getDistanceInMeters() {
        return (int) distance;
    }

    @Subscribe
    public void onPressureChange(PressureChangeEvent e) {
        lastPressure = e.pressure;
    }

    private int maxCalories = 0;

    public int getCalories() {
        workout.avgSpeed = getAvgSpeed();
        workout.duration = getDuration();
        int calories = new CalorieCalculator(context).calculateCalories(UserMeasurements.from(context), workout);
        if (calories > maxCalories) {
            maxCalories = calories;
        }
        return maxCalories;
    }


    // in m/s
    public double getAvgSpeed() {
        return distance / (double) (getDuration() / 1000);
    }

    public double getAvgPace() {
        double speed = getAvgSpeed();
        if (speed < 0.001) {
            return 0;
        } else {
            return (1 / speed) * 1000 / 60;
        }
    }

    // in m/s
    public double getAvgSpeedTotal() {
        return distance / (double) (getTimeSinceStart() / 1000);
    }

    // in m/s
    public double getCurrentSpeed() {
        RouteSample lastSample = getLastSample();
        if (lastSample != null) {
            if (!useAverageForCurrentSpeed || currentSpeedAverageTime == 0 || samples.size() == 1) {
                return lastSample.speed;
            } else {
                return getCurrentSpeed(currentSpeedAverageTime);
            }
        } else {
            return 0;
        }
    }

    // Returns average speed within the given time in m/s
    public double getCurrentSpeed(int time) {
        synchronized (samples) {
            if (samples.size() < 2) {
                return 0;
            }
            long currentTime = getDuration();
            long minTime = currentTime - time;
            double distance = 0;
            RouteSample lastSample = samples.get(samples.size() - 1);
            RouteSample firstSample = lastSample;
            for (int i = samples.size() - 1; i >= 0; i--) { // Go backwards
                RouteSample currentSample = samples.get(i);
                if (lastResume != 0 && currentSample.absoluteTime < lastResume) {
                    break; // We're past the last time we resumed, avoid large jumps during pauses
                } else if (currentSample.relativeTime <= minTime) {
                    break; // We can exit the loop now as every other sample was recorded earlier
                }

                distance += currentSample.toLatLong().sphericalDistance(lastSample.toLatLong());
                lastSample = currentSample;
            }
            // Keep last speed even when losing GPS signal
            // long timeDiff = lastGpsTime - lastSample.absoluteTime;
            // long timeDiff = currentTime - lastSample.relativeTime;
            long timeDiff = firstSample.relativeTime - lastSample.relativeTime;
            if (timeDiff == 0) {
                return 0;
            }
            return distance / (timeDiff / 1000d);
        }
    }

    public void discard() {
        WorkoutLogger.log("WorkoutRecorder", "Discarding workout");
        workoutSaver.discardWorkout();
    }

    public void onIntervalWasTriggered(Interval interval) {
        lastTriggeredInterval = interval.id;
    }

    public long getTimeSinceStart() {
        if (startTime != 0) {
            return System.currentTimeMillis() - startTime;
        } else {
            return 0;
        }
    }

    public long getPauseDuration() {
        if (state == RecordingState.PAUSED) {
            return pauseTime + (System.currentTimeMillis() - lastPause);
        } else {
            return pauseTime;
        }
    }

    public long getDuration() {
        if (state == RecordingState.RUNNING) {
            return time + (System.currentTimeMillis() - lastResume);
        } else {
            return time;
        }
    }

    @Subscribe
    public void onHeartRateChange(HeartRateChangeEvent event) {
        lastHeartRate = event.heartRate;
    }

    @Subscribe
    public void onHeartRateConnectionChange(HeartRateConnectionChangeEvent event) {
        if (event.state != HeartRateComponent.HeartRateConnectionState.CONNECTED) {
            // If heart rate sensor currently not available
            lastHeartRate = -1;
        }
    }

    @Subscribe
    public void onHRBatteryChange(HRBatteryLevelChangeEvent event) {
        lastHRBatteryLevel = event.batteryLevel;
    }

    @Subscribe
    public void onHRBatteryConnectionChange(HRBatteryLevelConnectionEvent event) {
        if (event.state != HeartRateComponent.HeartRateConnectionState.CONNECTED) {
            // If heart rate sensor currently not available
            lastHRBatteryLevel = -1;
        }
    }

    public RecordingState getState() {
        return state;
    }

    public void setIntervalList(List<Interval> intervalList) {
        this.intervalList = intervalList;
    }

    public List<Interval> getIntervalList() {
        return intervalList;
    }

    public int getCurrentHeartRate() {
        return lastHeartRate;
    }

    public int getCurrentHRBatteryLevel() {
        return lastHRBatteryLevel;
    }

    public boolean isAutoPauseEnabled() {
        return useAutoPause;
    }

    public boolean isActive() {
        return state == RecordingState.IDLE || state == RecordingState.RUNNING || state == RecordingState.PAUSED;
    }

    public boolean isPausedOrResumed() {
        return isPaused() || isResumed();
    }

    public boolean isResumed() {
        return state == RecordingState.RUNNING;
    }

    public boolean isPaused() {
        return state == RecordingState.PAUSED;
    }

}
