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

package de.tadris.fitness.recording.autostart;

import android.os.CountDownTimer;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Timer;
import java.util.TimerTask;

import de.tadris.fitness.recording.GpsState;
import de.tadris.fitness.recording.event.WorkoutGPSStateChanged;
import de.tadris.fitness.util.event.EventBusMember;

public class AutoStartWorkout implements EventBusMember {
    public static final int DEFAULT_DELAY_S = 20;

    public enum State {
            IDLE,
            COUNTDOWN,
            WAITING_FOR_GPS,
            WAITING_FOR_MOVE,
            AUTO_START_REQUESTED,
            ABORTED_BY_USER,
            ABORTED_ALREADY_STARTED,
    }

    /**
     * The different auto start modes. They can all be used together with a countdown timer.
     *
     * @implNote {@link #ON_MOVE} is not implemented yet and will fall back to {@link #WAIT_FOR_GPS}
     */
    public enum Mode {
        /** start recording immediately **/
        INSTANT,
        /** start recording once the user starts moving around **/
        ON_MOVE,
        /** wait for the GPS position to be accurate enough before starting **/
        WAIT_FOR_GPS;

        /**
         * Get the default auto start mode.
         */
        public static Mode getDefault() {
            return INSTANT;
        }
    }

    /**
     * A configuration structure that specifies how auto start should behave.
     */
    public static class Config {
        /**
         * Countdown length.
         * @implNote This might actually be ignored depending on {@link Config#mode}.
         * @apiNote If you don't set this field, the {@link #DEFAULT_DELAY_S default} will be used.
         */
        public final long countdownMs;

        /**
         * Auto start mode.
         * @apiNote If you don't set this field, the {@link Mode#getDefault()} default} will be used.
         */
        public final Mode mode;

        public Config(long countdownMs) {
            this.countdownMs = countdownMs;
            this.mode = Mode.getDefault();
        }

        public Config(Mode mode) {
            this.countdownMs = DEFAULT_DELAY_S * 1_000;
            this.mode = mode;
        }

        public Config(long countdownMs, Mode mode) {
            this.countdownMs = countdownMs;
            this.mode = mode;
        }
    }

    private static final String TAG = "AutoStartWorkoutModel";

    private State state = State.IDLE;
    private long countdownMs;
    private Config lastStartConfig;
    private Config defaultStartConfig;
    private MovementDetector movementDetector;
    private CountDownTimer autoStartCountdownTimer;
    private final Timer autoStartWaitTimer = new Timer("AutoStartOnGpsOkay");
    private TimerTask autoStartWaitTask;
    private EventBus eventBus;
    private boolean gpsOkay = false;

    /**
     * Creates an {@link AutoStartWorkout} instance.
     * @param defaultConfig the default config for this instance (e.g. taken from preferences)
     */
    public AutoStartWorkout(Config defaultConfig, MovementDetector movementDetector) {
        this.lastStartConfig = defaultConfig;
        this.defaultStartConfig = defaultConfig;
        this.movementDetector = movementDetector;
    }

    @Override
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * This event will be posted to EventBus when the internal state changes.
     */
    public static class StateChangeEvent {
        public final State oldState;
        public final State newState;
        public StateChangeEvent(State newState, State oldState) {
            this.newState = newState;
            this.oldState = oldState;
        }
    }

    /**
     * This event will be posted to EventBus when the internal countdown value changes.
     */
    public static class CountdownChangeEvent {
        public final long countdownMs;
        public final int countdownS;
        public CountdownChangeEvent(long countdownMs) {
            this.countdownMs = countdownMs;
            this.countdownS = (int) (countdownMs + 500) / 1000;
        }
    }

    /**
     * This event must be posted to EventBus to start the auto start sequence
     */
    public static class BeginEvent {
        public Config config;
        public BeginEvent(Config config) {
            this.config = config;
        }
    }

    /**
     * This event must be posted to EventBus to stop/abort the auto start sequence
     */
    public static class AbortEvent {
        public Reason reason;

        /**
         * Some reasons why auto start is aborted.
         */
        public enum Reason {
            /**
             * When auto start is aborted, because something else triggered recording
             */
            STARTED,
            /**
             * When the user requested to abort auto start
             */
            USER_REQ,
        }

        public AbortEvent() {
            this.reason = Reason.USER_REQ;
        }

        public AbortEvent(Reason reason) {
            this.reason = reason;
        }

    }

    /**
     * Set the internal state
     *
     * @implNote This posts a StateChangeEvent to {@link #eventBus}
     * @param newState the new state
     */
    public void setState(State newState) {
        State oldState = state;
        state = newState;
        eventBus.post(new StateChangeEvent(state, oldState));
    }

    /**
     * Get the current state
     * @return current state
     */
    public State getState() {
        return state;
    }

    /**
     * Set the current countdown value
     *
     * @implNote This posts a CountdownChangeEvent to {@link #eventBus}
     * @param newCountdownMs the new countdown value in milliseconds
     */
    public void setCountdownMs(long newCountdownMs) {
        countdownMs = newCountdownMs;
        eventBus.post(new CountdownChangeEvent(countdownMs));
    }

    /**
     * Get the current countdown value
     * @return current countdown value in milliseconds
     */
    public long getCountdownMs() {
        return countdownMs;
    }

    /**
     * Get the last initial configuration
     */
    public Config getLastStartConfig() {
        return lastStartConfig;
    }

    /**
     * Get the default auto start config
     */
    public Config getDefaultStartConfig() {
        return defaultStartConfig;
    }

    /**
     * Start the auto start countdown
     */
    private void startCountdown() {
        if (state != State.COUNTDOWN) {
            setState(State.COUNTDOWN);
        }

        // start countdown timer
        // do this for 0s delay as well to prevent duplicate code
        autoStartCountdownTimer = new CountDownTimer(countdownMs, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "Remaining: " + millisUntilFinished);
                // (x + 500) / 1000 for rounding (otherwise the countdown would start at one
                // less than the expected value
                setCountdownMs(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                // show 0s left...
                onTick(0);
                startWithMode();
            }
        }.start();
    }

    /**
     * When the countdown expired, this function initiates the next steps to auto start depending on
     * the configured mode.
     */
    private void startWithMode() {
        switch (lastStartConfig.mode) {
            case ON_MOVE:
                waitForMove();
                break;
            case WAIT_FOR_GPS:
                waitForGps();
                break;
            case INSTANT:
                setState(State.AUTO_START_REQUESTED);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + lastStartConfig.mode);
        }
    }

    /**
     * Wait for GPS before starting the workout automatically
     */
    private void waitForGps() {
        // early exit when GPS position is good enough
        if (gpsOkay) {
            setState(State.AUTO_START_REQUESTED);
            return;
        }

        // otherwise, wait for that to happen
        if (state != State.WAITING_FOR_GPS) {
            setState(State.WAITING_FOR_GPS);
        }

        // start as soon as GPS position is accurate enough
        autoStartWaitTask = new TimerTask() {
            @Override
            public void run() {
                if (gpsOkay) {  // request auto start
                    this.cancel();  // no need to run again
                    Log.d(TAG, "GPS fix -> finally able to start workout");
                    setState(State.AUTO_START_REQUESTED);
                } else {    // continue waiting
                    Log.d(TAG, "Still no GPS fix...");
                }
            }
        };
        autoStartWaitTimer.scheduleAtFixedRate(autoStartWaitTask, 500, 500);
    }
    /**
     * Wait for the user starting move before starting the workout automatically
     */
    private void waitForMove() {
        // otherwise, wait for that to happen
        if (state != State.WAITING_FOR_MOVE) {
            setState(State.WAITING_FOR_MOVE);
        }

        // start as soon as user moves
        autoStartWaitTask = new TimerTask() {
            @Override
            public void run() {
                if (!movementDetector.isStarted()) {
                    Log.w(TAG, "Waiting for user to start moving, but detector is not " +
                            "started. Will do this now.");
                    if (!movementDetector.start()) {
                        Log.e(TAG, "Failed to start movement detector. Cannot determine " +
                                "whether user is moving.");
                        return;
                    }
                }
                if (movementDetector.isStarted() && // might not be ready yet, if just started
                        movementDetector.getDetectionState() ==
                                MovementDetector.DetectionState.MOVING) {  // request auto start
                    this.cancel();  // no need to run again
                    Log.d(TAG, "user is moving -> finally able to start workout");
                    setState(State.AUTO_START_REQUESTED);
                } else {    // continue waiting
                    Log.d(TAG, "Still no movement...");
                }
            }
        };
        autoStartWaitTimer.scheduleAtFixedRate(autoStartWaitTask, 500, 500);
    }

    /**
     * Listen for requests to begin auto start
     */
    @Subscribe
    public void onBeginEvent(BeginEvent beginEvent) {
        // if we're already running, that should be aborted
        if (autoStartCountdownTimer != null) {
            autoStartCountdownTimer.cancel();
        }
        if (autoStartWaitTask != null) {
            autoStartWaitTask.cancel();
        }

        setCountdownMs(beginEvent.config.countdownMs);
        lastStartConfig = beginEvent.config;

        if (countdownMs > 0) {
            startCountdown();
        } else {
            startWithMode();
        }
    }


    /**
     * Listen for requests to abort auto start
     */
    @Subscribe
    public void onAbortEvent(AbortEvent abortEvent) {
        if (autoStartCountdownTimer != null) {
            autoStartCountdownTimer.cancel();
        }
        if (autoStartWaitTask != null) {
            autoStartWaitTask.cancel();
        }

        if (abortEvent.reason == AbortEvent.Reason.USER_REQ) {
            setState(State.ABORTED_BY_USER);
        } else {
            setState(State.ABORTED_ALREADY_STARTED);
        }
    }

    /**
     * Listen for GPS state changes
     */
    @Subscribe
    public void onGpsStateChanged(WorkoutGPSStateChanged gpsStateChanged) {
        gpsOkay = gpsStateChanged.newState == GpsState.SIGNAL_GOOD;
    }
}
