package de.tadris.fitness.export.model.legacy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LegacyIndoorSample{

    private long workoutId;
    private long id;
    private long absoluteTime;
    private long relativeTime;
    private int heartRate;
    private long intervalTriggered;
    private int repetitions;
    private long absoluteEndTime;
    private double intensity;
    private double frequency;

    public LegacyIndoorSample(){ }

    public long getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(long workoutId) {
        this.workoutId = workoutId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAbsoluteTime() {
        return absoluteTime;
    }

    public void setAbsoluteTime(long absoluteTime) {
        this.absoluteTime = absoluteTime;
    }

    public long getRelativeTime() {
        return relativeTime;
    }

    public void setRelativeTime(long relativeTime) {
        this.relativeTime = relativeTime;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    public long getIntervalTriggered() {
        return intervalTriggered;
    }

    public void setIntervalTriggered(long intervalTriggered) {
        this.intervalTriggered = intervalTriggered;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public void setRepetitions(int repetitions) {
        this.repetitions = repetitions;
    }

    public long getAbsoluteEndTime() {
        return absoluteEndTime;
    }

    public void setAbsoluteEndTime(long absoluteEndTime) {
        this.absoluteEndTime = absoluteEndTime;
    }

    public double getIntensity() {
        return intensity;
    }

    public void setIntensity(double intensity) {
        this.intensity = intensity;
    }

    public double getFrequency() {
        return frequency;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }
}