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

package de.tadris.fitness.stats;

import java.util.Comparator;

import de.tadris.fitness.data.WorkoutType;

public class StatsDataTypes {
    public static class DataPoint {
        public WorkoutType workoutType;
        public long workoutID;
        public long time;
        public double value;

        public DataPoint(WorkoutType type, long id, long start, double value) {
            this.workoutType = type;
            this.workoutID=id;
            this.time = start;
            this.value = value;
        }

        public static Comparator<DataPoint> timeComparator = (first, second) -> {
            if (first.time > second.time) {
                return 1;
            } else if (first.time < second.time) {
                return -1;
            } else {
                return 0;
            }
        };

        public static Comparator<DataPoint> valueComparator = (first, second) -> {
            if (first.value > second.value) {
                return 1;
            } else if (first.value < second.value) {
                return -1;
            } else {
                return 0;
            }
        };
    }

    public static class TimeSpan {
        public long startTime;
        public long endTime;

        public TimeSpan(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public boolean contains(long time) {
            return startTime <= time && time <= endTime;
        }

        public long length() {
            return this.endTime - this.startTime;
        }

        @Override
        public String toString() {
            return "TimeSpan{" +
                    "startTime=" + startTime +
                    ", endTime=" + endTime +
                    '}';
        }

        public enum Type {
            WEEK(0),
            MONTH(1),
            YEAR(2);

            private int id;

            Type(int i) {
                this.id = i;
            }

            public int getId() {
                return id;
            }
        }
    }
}
