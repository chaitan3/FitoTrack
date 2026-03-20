package de.tadris.fitness.export.model.legacy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LegacyGpsSample {

    private long workoutId;
    private long id;
    private long absoluteTime;
    private long relativeTime;
    private int heartRate;
    private long intervalTriggered;
    private double lat;
    private double lon;
    private double elevation;
    private double elevationMSL;
    private double speed;
    private float pressure;

    public LegacyGpsSample(){}

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

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public double getElevationMSL() {
        return elevationMSL;
    }

    public void setElevationMSL(double elevationMSL) {
        this.elevationMSL = elevationMSL;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }
}