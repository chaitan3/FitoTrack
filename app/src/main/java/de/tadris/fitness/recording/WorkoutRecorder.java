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
import android.graphics.Color;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.SystemClock;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.mapsforge.core.model.LatLong;

import java.util.ArrayList;
import java.util.List;

import de.tadris.fitness.Instance;
import de.tadris.fitness.data.Interval;
import de.tadris.fitness.data.IntervalSet;
import de.tadris.fitness.data.RecordingType;
import de.tadris.fitness.data.RouteSample;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.data.preferences.UserMeasurements;
import de.tadris.fitness.data.preferences.UserPreferences;
import de.tadris.fitness.recording.component.GpsComponent;
import de.tadris.fitness.recording.component.HeartRateComponent;
import de.tadris.fitness.recording.event.HRBatteryLevelChangeEvent;
import de.tadris.fitness.recording.event.HRBatteryLevelConnectionEvent;
import de.tadris.fitness.recording.event.HeartRateChangeEvent;
import de.tadris.fitness.recording.event.HeartRateConnectionChangeEvent;
import de.tadris.fitness.recording.event.LocationChangeEvent;
import de.tadris.fitness.recording.event.PressureChangeEvent;
import de.tadris.fitness.recording.event.WorkoutAutoStopEvent;
import de.tadris.fitness.recording.event.WorkoutGPSStateChanged;
import de.tadris.fitness.recording.gps.WorkoutSaver;
import de.tadris.fitness.ui.record.RecordGpsWorkoutActivity;
import de.tadris.fitness.ui.record.RecordWorkoutActivity;
import de.tadris.fitness.util.LocationUtils;
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

    public static final int PAUSE_TIME = 10_000; // 10 Seconds
    public static final int MIN_DURATION_DIFF = 500; // minimum 500ms between two samples

    private static final int AUTO_TIMEOUT_MULTIPLIER = 1_000 * 60; // minutes to ms


    protected final Context context;
    protected boolean useAutoPause;
    protected long autoTimeoutMs;

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

    private static final double SIGNAL_BAD_THRESHOLD = 30; // In meters
    private static final int SIGNAL_LOST_THRESHOLD = 10_000; // 10 Seconds In milliseconds
    private Location lastFix = null;
    private GpsWorkoutRecorder.GpsState gpsState = GpsWorkoutRecorder.GpsState.SIGNAL_LOST;

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

    public WorkoutRecorder(Context context, GpsWorkout workout, List<RouteSample> samples) {
        this(context);
        this.state = RecordingState.PAUSED;

        this.workout = workout;
        this.samples.addAll(samples);

        // time = 0; x
        // pauseTime = 0; x
        // lastResume; x
        // lastPause = 0; x
        // lastSampleTime = 0; x
        // distance = 0; x
        reconstructBySamples();

        workoutSaver = new WorkoutSaver(this.context, getWorkoutData());
    }

    private void reconstructBySamples() {
        WorkoutLogger.log("WorkoutRecorder", "Trying to reconstruct previously recorded workout");
        lastResume = workout.start;
        lastSampleTime = workout.start;
        LatLong prefLocation = null;
        for (RouteSample sample : samples) {
            long timeDiff = sample.absoluteTime - lastSampleTime;
            if (timeDiff > PAUSE_TIME) { // Handle Pause
                lastPause = lastSampleTime + PAUSE_TIME; // Also add the Minimal Pause Time ;D
                lastResume = sample.absoluteTime; // Workout resumed at new sample
                pauseTime += timeDiff - PAUSE_TIME; // Add Time Diff without Pause Time
            }
            if (prefLocation != null) { //Update Distance
                double sampleDistance = prefLocation.sphericalDistance(sample.toLatLong());
                distance += sampleDistance;
            }
            prefLocation = sample.toLatLong();
            lastSampleTime = sample.absoluteTime;
            time = sample.relativeTime; // Update Times Always To Sample RelTime
        }
        if (System.currentTimeMillis() - lastSampleTime > PAUSE_TIME) {
            state = RecordingState.PAUSED;
            time += PAUSE_TIME;
            lastPause = lastSampleTime + PAUSE_TIME;
        } else {
            state = RecordingState.RUNNING;
        }
        lastSampleTime = System.currentTimeMillis(); // prevent automatic stop
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

    /**
     * Handles the Record Watchdog, for GPS Check, Pause Detection and Auto Timeout
     *
     * @return is still active workout
     */
    public boolean handleWatchdog() {
        if (isActive()) {
            WorkoutLogger.log("WorkoutRecorder", "handleWatchdog " + this.getState().toString() + " samples: " + getSampleSize() + " instance: " + this);
            onWatchdog();
            if (hasRecordedSomething()) {
                long timeDiff = System.currentTimeMillis() - lastSampleTime;
                if (autoTimeoutMs > 0 && timeDiff > autoTimeoutMs) {
                    if (isActive()) {
                        WorkoutLogger.log("WorkoutRecorder", "Auto timeout was set to: " + autoTimeoutMs);
                        stop("Auto timeout, timediff: " + timeDiff);
                        save();
                        EventBus.getDefault().post(new WorkoutAutoStopEvent());
                    }
                } else if (useAutoPause) {
                    if (timeDiff > PAUSE_TIME) {
                        if (state == RecordingState.RUNNING && autoPausePossible()) {
                            pause();
                        }
                    } else {
                        if (state == RecordingState.PAUSED) {
                            resume();
                        }
                    }
                }
            }
            return true;
        } else {
            return false;
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
    public RecordingType getRecordingType() {
        return RecordingType.GPS;
    }

    @Override
    protected void onWatchdog() {
        checkSignalState();
    }

    @Override
    protected boolean autoPausePossible() {
        return gpsState != GpsWorkoutRecorder.GpsState.SIGNAL_LOST;
    }

    private void checkSignalState() {
        if (lastFix == null) {
            return;
        }
        GpsState state;
        if ((SystemClock.elapsedRealtimeNanos() - lastFix.getElapsedRealtimeNanos()) / 1000_000L > SIGNAL_LOST_THRESHOLD) {
            state = GpsState.SIGNAL_LOST;
        } else if (lastFix.getAccuracy() > SIGNAL_BAD_THRESHOLD) {
            state = GpsState.SIGNAL_BAD;
        } else {
            state = GpsState.SIGNAL_GOOD;
        }

        if (state != gpsState) {
            WorkoutLogger.log("Recorder", "GPS State: " + this.gpsState.name() + " -> " + state.name());
            EventBus.getDefault().post(new WorkoutGPSStateChanged(this.gpsState, state));
            gpsState = state;
        }
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
        Location location = e.location;
        lastFix = location;
        if (isActive()) {
            double distance = 0;
            if (getSampleCount() > 0) {
                // Checks whether the minimum distance to last sample was reached
                // and if the time difference to the last sample is too small
                synchronized (samples) {
                    RouteSample lastSample = samples.get(samples.size() - 1);
                    distance = Math.abs(GpsComponent.locationToLatLong(location).sphericalDistance(lastSample.toLatLong()));
                    long timediff = Math.abs(lastSample.absoluteTime - LocationUtils.getTimeFor(location));
                    if (distance < workout.getWorkoutType(context).minDistance || timediff < MIN_DURATION_DIFF) {
                        return;
                    }
                }
            }
            lastSampleTime = System.currentTimeMillis();
            if (state == RecordingState.RUNNING && LocationUtils.getTimeFor(location) > workout.start) {
                this.distance += distance;
                addToSamples(location);
            }
        }
    }

    private void addToSamples(Location location) {
        RouteSample sample = new RouteSample();
        sample.latitude = location.getLatitude();
        sample.longitude = location.getLongitude();
        sample.elevation = location.getAltitude();
        sample.speed = location.getSpeed();
        sample.relativeTime = LocationUtils.getTimeFor(location) - workout.start - getPauseDuration();
        sample.absoluteTime = LocationUtils.getTimeFor(location);
        sample.pressure = lastPressure;
        sample.heartRate = lastHeartRate;
        sample.intervalTriggered = lastTriggeredInterval;
        lastTriggeredInterval = -1;
        synchronized (samples) {
            if (workoutSaver == null) {
                throw new RuntimeException("Missing WorkoutSaver for Recorder");
            }
            workoutSaver.addSample(sample); // already persist to db
            samples.add(sample); // add to recorder list
        }
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

    public int getAscent() {
        double ascent = 0;
        synchronized (samples) {
            if (samples.size() == 0) {
                return 0;
            }
            double lastElevation = -1;
            for (RouteSample sample : samples) {
                double elevation = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, sample.pressure);
                if (lastElevation == -1) lastElevation = elevation;
                elevation = (elevation + lastElevation * 9) / 10; // Slow floating average
                if (elevation > lastElevation) {
                    ascent += elevation - lastElevation;
                }
                lastElevation = elevation;
            }
        }
        System.out.println(ascent);
        return (int) ascent;
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

    public enum GpsState {
        SIGNAL_LOST(Color.RED),
        SIGNAL_GOOD(Color.GREEN),
        SIGNAL_BAD(Color.YELLOW);

        public final int color;

        GpsState(int color) {
            this.color = color;
        }
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
        return state == GpsWorkoutRecorder.RecordingState.IDLE || state == GpsWorkoutRecorder.RecordingState.RUNNING || state == GpsWorkoutRecorder.RecordingState.PAUSED;
    }

    public boolean isPausedOrResumed() {
        return isPaused() || isResumed();
    }

    public boolean isResumed() {
        return state == GpsWorkoutRecorder.RecordingState.RUNNING;
    }

    public boolean isPaused() {
        return state == RecordingState.PAUSED;
    }

    public enum RecordingState {
        IDLE, RUNNING, PAUSED, STOPPED
    }

}
