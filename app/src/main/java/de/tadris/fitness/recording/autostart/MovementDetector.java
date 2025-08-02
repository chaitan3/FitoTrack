/*
 * Copyright (c) 2025 Jannis Scheibe <jannis@tadris.de>
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

package de.tadris.fitness.recording.autostart;

import org.greenrobot.eventbus.EventBus;

import de.tadris.fitness.util.event.EventBusMember;

/**
 * This class implements a basic interface for movement detectors.<p>
 *     Movement detectors are used by {@link de.tadris.fitness.model.AutoStartWorkout} in {@link de.tadris.fitness.model.AutoStartWorkout.Mode#ON_MOVE}
 * Why an extra class: Movement detection may need to be tailored to a specific workout type to
 * reach an acceptable depending on the algorithm used to
 * detect user movement. E.g. differences in the x-,y- and z-acceleration profile (for example
 * cycling compared to horseback riding) may be leveraged to tailor movement detection to a specific
 * workout type.
 */
public abstract class MovementDetector implements EventBusMember {
    /**
     * Different stages a {@link MovementDetector} runs through in its lifecycle.
     */
    public enum State {
        /**
         * This is the initial state from which it can be started.
         */
        IDLE,
        /**
         * A temporary state only, used when transitioning from {@link #IDLE} or {@link #STOPPED} to
         * {@link #RUNNING}*, if the detector needs a certain time for that.
         */
        STARTING,
        /**
         * Movement detection is activated. Its #DetectionState can be used now to check whether a
         * user is moving.
         */
        RUNNING,
        /**
         * A temporary state only, used when transitioning from {@link #RUNNING} to {@link #STOPPED},
         * if the detector needs a certain time for that. You can't rely on the #DetectionState
         * anymore from now on.
         * */
        STOPPING,
        /**
         * The detector has been stopped. You may restart it by calling its {@link #start() start}
         * method.
         */
        STOPPED
    }

    /**
     * Indicates if the user moves.
     */
    public enum DetectionState {
        /**
         * The detector is either still initializing or has a hard time determining,
         * whether the user is actually moving.
         */
        NOT_SURE,
        /**
         * User is moving.
         */
        MOVING,
        /**
         * User is **NOT** moving.
         */
        NOT_MOVING
    }

    protected EventBus eventBus;
    protected State state;
    protected DetectionState detectionState;

    /**
     * Initialize the instance.
     * @apiNote Don't forget to call this from your subclass' constructor.
     */
    public MovementDetector() {
        state = State.IDLE;
        detectionState = DetectionState.NOT_SURE;
    }

    /**
     * Starts movement detection.
     * @return Whether starting was successful
     * @implNote Make sure
     */
    public abstract boolean start();

    /**
     * Stops movement detection.
     * @return Whether stopping was successful
     * @implNote Make sure detection stops as soon as possible on the first call. Further calls must
     * be allowed and not change anything.
     */
    public abstract boolean stop();

    /**
     * Check whether detection has been started and is currently enabled.
     */
    public boolean isStarted() {
        return state == State.RUNNING;
    }

    /**
     * Check whether detection is disabled.
     */
    public boolean isStopped() {
        return state == State.STOPPED || state == State.IDLE;
    }

    /**
     * Get the current detection state (whether user moves or not).
     * @apiNote You can only rely on this when detection {@link #isStarted() is started}.
     */
    public DetectionState getDetectionState() {
        return detectionState;
    }

    @Override
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
