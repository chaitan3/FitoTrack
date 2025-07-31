package de.tadris.fitness.recording.event;

public class WorkoutGPSStateChanged {

    public final GpsWorkoutRecorder.GpsState oldState;
    public final GpsWorkoutRecorder.GpsState newState;

    public WorkoutGPSStateChanged(GpsWorkoutRecorder.GpsState oldState, GpsWorkoutRecorder.GpsState newState) {
        this.oldState = oldState;
        this.newState = newState;
    }
}
