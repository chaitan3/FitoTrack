/*
 * Copyright (c) 2022 Jannis Scheibe <jannis@tadris.de>
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

package de.tadris.fitness;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

import de.tadris.fitness.data.preferences.UserMeasurements;
import de.tadris.fitness.util.calorie.CalorieCalculator;
import de.tadris.fitness.util.calorie.FallbackMETProvider;

public class CalorieCalculatorTest {

    @Test
    public void testFallbackCalculation() {
        UserMeasurements measurements = new UserMeasurements(80, 0.7f);

        GpsWorkout workout = new GpsWorkout();
        workout.avgSpeed = 2.7d;
        workout.workoutTypeId = "running";
        workout.duration = 1000L * 60 * 10;

        int calorie = new CalorieCalculator(Collections.singletonList(FallbackMETProvider.INSTANCE)).calculateCalories(measurements, workout);
        Assert.assertEquals(130, calorie, 50);
    }

}
