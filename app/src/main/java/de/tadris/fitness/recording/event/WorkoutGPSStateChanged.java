package de.tadris.fitness.recording.event;

import de.tadris.fitness.recording.GpsState;

public class WorkoutGPSStateChanged {

    public final GpsState oldState;
    public final GpsState newState;

    public WorkoutGPSStateChanged(GpsState oldState, GpsState newState) {
        this.oldState = oldState;
        this.newState = newState;
    }
}
