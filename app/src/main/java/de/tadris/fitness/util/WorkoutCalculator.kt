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

package de.tadris.fitness.util;

import org.mapsforge.core.model.LatLong;

import java.util.ArrayList;
import java.util.List;

import de.tadris.fitness.data.BaseSample;
import de.tadris.fitness.data.RouteSample;

public class WorkoutCalculator {

    public static List<Set> getSetsFromWorkout(IndoorWorkoutData data) {
        IndoorWorkout workout = data.getWorkout();
        List<IndoorSample> samples = new ArrayList<>(data.getSamples());
        List<Set> result = new ArrayList<>();

        long lastAbsoluteTimeEnd = workout.start;
        long lastRelativeTimeEnd = 0;
        List<Pause> pauses = getPausesFromWorkout(data.castToBaseWorkoutData());
        pauses.add(new Pause(workout.end, workout.duration, 0, null)); // Append a last pause so every set ends with a pause

        for (Pause pause : pauses) {
            Set set = new Set(lastAbsoluteTimeEnd, lastRelativeTimeEnd, pause.relativeTimeStart - lastRelativeTimeEnd, 0);

            while (samples.size() > 0 && samples.get(0).relativeTime < set.relativeTimeStart + set.duration) {
                set.repetitions += samples.remove(0).repetitions;
            }

            if (set.repetitions > 0) {
                result.add(set);
            }
            lastAbsoluteTimeEnd = pause.absoluteTimeStart + pause.duration;
            lastRelativeTimeEnd = pause.relativeTimeStart + pause.duration;
        }

        return result;
    }

    public static long calculatePauseDuration(BaseWorkoutData data) {
        long pauseDuration = 0;
        for (WorkoutCalculator.Pause pause : getPausesFromWorkout(data)) {
            pauseDuration += pause.duration;
        }

        return pauseDuration;
    }

    public static List<Pause> getPausesFromWorkout(BaseWorkoutData data) {
        List<Pause> result = new ArrayList<>();
        List<BaseSample> samples = data.getSamples();

        long absoluteTime = data.getWorkout().start;
        long relativeTime = 0;
        boolean lastWasPause = false;

        for (BaseSample sample : samples) {
            long absoluteDiff = sample.absoluteTime - absoluteTime;
            long relativeDiff = sample.relativeTime - relativeTime;
            long diff = absoluteDiff - relativeDiff;

            if (diff > 1000) {
                if (lastWasPause) {
                    // Add duration to last pause if there is no sample between detected pauses
                    result.get(result.size() - 1).addDuration(diff);
                } else {
                    LatLong location = null;
                    if (sample instanceof RouteSample) {
                        location = ((RouteSample) sample).toLatLong();
                    }
                    result.add(new Pause(absoluteTime, relativeTime, diff, location));
                }
                lastWasPause = true;
            } else {
                lastWasPause = false;
            }
            absoluteTime = sample.absoluteTime;
            relativeTime = sample.relativeTime;
        }
        return result;
    }

    public static class Set {

        public final long absoluteTimeStart;
        public final long relativeTimeStart;
        public long duration;
        public int repetitions;

        public Set(long absoluteTimeStart, long relativeTimeStart, long duration, int repetitions) {
            this.absoluteTimeStart = absoluteTimeStart;
            this.relativeTimeStart = relativeTimeStart;
            this.duration = duration;
            this.repetitions = repetitions;
        }
    }

    public static class Pause {
        public final long absoluteTimeStart;
        public final long relativeTimeStart;
        public long duration;
        public final LatLong location;

        public Pause(long absoluteTimeStart, long relativeTimeStart, long duration, LatLong location) {
            this.absoluteTimeStart = absoluteTimeStart;
            this.relativeTimeStart = relativeTimeStart;
            this.duration = duration;
            this.location = location;
        }

        private void addDuration(long duration) {
            this.duration += duration;
        }

    }

    /**
     * Returns a list of relative times when intervals were triggered
     */
    public static List<Long> getIntervalSetTimesFromWorkout(BaseWorkoutData data) {
        List<Long> result = new ArrayList<>();
        List<BaseSample> samples = data.getSamples();

        for (BaseSample sample : samples) {
            if (sample.intervalTriggered > 0) {
                result.add(sample.relativeTime);
            }
        }
        return result;
    }

}