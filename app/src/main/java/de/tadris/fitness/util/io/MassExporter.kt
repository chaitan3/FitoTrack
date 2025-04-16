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

package de.tadris.fitness.util.io

import android.content.Context
import de.tadris.fitness.data.GpsWorkout
import de.tadris.fitness.data.GpsWorkoutDao
import de.tadris.fitness.data.GpsWorkoutData
import de.tadris.fitness.util.io.general.IWorkoutExporter
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MassExporter(
    private val context: Context,
    private val gpsWorkoutDao: GpsWorkoutDao,
    private val export: IWorkoutExporter,
    file: File,
    private val listener: ProgressListener? = null,
) {

    private val output = file.outputStream()
    private val zipOut = ZipOutputStream(output)

    fun export(){
        val workouts = gpsWorkoutDao.workouts
        workouts.forEachIndexed { index, workout ->
            listener?.onProgressUpdate(index * 100 / workouts.size)
            exportWorkout(index, workout)
        }
        zipOut.close()
        output.close()
        listener?.onProgressUpdate(100)
    }

    private fun exportWorkout(index: Int, workout: GpsWorkout){
        val data = GpsWorkoutData.fromWorkout(gpsWorkoutDao, workout)
        val zipEntryName =
            String.format("%05d", index) + "_" + workout.getWorkoutType(context) + ".gpx";
        val entry = ZipEntry(zipEntryName)

        zipOut.putNextEntry(entry)
        export.exportWorkout(data, zipOut)
        zipOut.closeEntry()
    }

    fun interface ProgressListener {

        fun onProgressUpdate(progress: Int)

    }

}