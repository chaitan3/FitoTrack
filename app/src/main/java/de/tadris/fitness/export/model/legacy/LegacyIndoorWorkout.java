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

package de.tadris.fitness.export.model.legacy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LegacyIndoorWorkout {
    private long id;
    private long start;
    private long end;
    private long duration;
    private long pauseDuration;
    private String comment;

    @JsonProperty(value = "workoutType")
    private String workoutTypeId;
    private int avgHeartRate;
    private int maxHeartRate;
    private int calorie;
    private boolean edited;
    private long intervalSetUsedId;
    private int repetitions;
    private double avgFrequency;
    private double maxFrequency;
    private double maxIntensity;
    private double avgIntensity;

    public LegacyIndoorWorkout(){ }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getPauseDuration() {
        return pauseDuration;
    }

    public void setPauseDuration(long pauseDuration) {
        this.pauseDuration = pauseDuration;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getWorkoutTypeId() {
        return workoutTypeId;
    }

    public void setWorkoutTypeId(String workoutTypeId) {
        this.workoutTypeId = workoutTypeId;
    }

    public int getAvgHeartRate() {
        return avgHeartRate;
    }

    public void setAvgHeartRate(int avgHeartRate) {
        this.avgHeartRate = avgHeartRate;
    }

    public int getMaxHeartRate() {
        return maxHeartRate;
    }

    public void setMaxHeartRate(int maxHeartRate) {
        this.maxHeartRate = maxHeartRate;
    }

    public int getCalorie() {
        return calorie;
    }

    public void setCalorie(int calorie) {
        this.calorie = calorie;
    }

    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public long getIntervalSetUsedId() {
        return intervalSetUsedId;
    }

    public void setIntervalSetUsedId(long intervalSetUsedId) {
        this.intervalSetUsedId = intervalSetUsedId;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public void setRepetitions(int repetitions) {
        this.repetitions = repetitions;
    }

    public double getAvgFrequency() {
        return avgFrequency;
    }

    public void setAvgFrequency(double avgFrequency) {
        this.avgFrequency = avgFrequency;
    }

    public double getMaxFrequency() {
        return maxFrequency;
    }

    public void setMaxFrequency(double maxFrequency) {
        this.maxFrequency = maxFrequency;
    }

    public double getMaxIntensity() {
        return maxIntensity;
    }

    public void setMaxIntensity(double maxIntensity) {
        this.maxIntensity = maxIntensity;
    }

    public double getAvgIntensity() {
        return avgIntensity;
    }

    public void setAvgIntensity(double avgIntensity) {
        this.avgIntensity = avgIntensity;
    }
}