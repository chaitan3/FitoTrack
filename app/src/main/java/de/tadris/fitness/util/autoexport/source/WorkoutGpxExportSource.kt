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
package de.tadris.fitness.util.autoexport.source

import android.content.Context
import de.tadris.fitness.Instance
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.util.DataManager
import de.tadris.fitness.util.io.general.IOHelper

class WorkoutGpxExportSource(private val workoutId: Long) : ExportSource {

    override fun provideFile(context: Context): ExportSource.ExportedFile {
        val workout = Instance.getInstance(context).db.workoutDao().getWorkoutById(workoutId)!!
        val data = WorkoutData.fromWorkout(context, workout)
        val file =
            DataManager.createSharableFile(context, workout.getExportFileName(context) + ".gpx")
        IOHelper.GpxExporter.exportWorkout(data, file)

        return ExportSource.ExportedFile(
            file, mapOf(
                "FitoTrack-Type" to ExportSource.EXPORT_SOURCE_WORKOUT_GPX,
                "FitoTrack-Timestamp" to workout.start.toString(),
                "FitoTrack-Workout-Type" to workout.workoutTypeId,
                "FitoTrack-Comment" to workout.safeComment,
            )
        )
    }
}