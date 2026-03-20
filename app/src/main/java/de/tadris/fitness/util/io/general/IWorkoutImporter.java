/*
 * Copyright (c) 2021 Jannis Scheibe <jannis@tadris.de>
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

package de.tadris.fitness.util.io.general;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

import de.tadris.fitness.Instance;
import de.tadris.fitness.data.AppDatabase;
import de.tadris.fitness.data.WorkoutData;
import de.tadris.fitness.util.io.ImportWorkoutSaver;

public interface IWorkoutImporter {
    class WorkoutImportResult {
        public List<WorkoutData> workouts;

        public WorkoutImportResult(List<WorkoutData> workouts) {
            this.workouts = workouts;
        }
    }

    WorkoutImportResult readWorkouts(InputStream input) throws IOException;

    /**
     * @return number of imported workouts
     */
    default int importWorkout(Context context, InputStream input) throws IOException {
        WorkoutImportResult importResult = readWorkouts(input);
        for (WorkoutData data : importResult.workouts) {
            new ImportWorkoutSaver(context, data).save();
        }
        return importResult.workouts.size();
    }

    default int importWorkout(Context context, File file) throws IOException {
        return importWorkout(context, Files.newInputStream(file.toPath()));
    }
}
