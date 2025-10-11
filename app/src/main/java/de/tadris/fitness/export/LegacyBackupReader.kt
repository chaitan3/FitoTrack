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
package de.tadris.fitness.export

import android.content.Context
import android.net.Uri
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.AppDatabase
import de.tadris.fitness.export.model.IntervalSetContainer
import de.tadris.fitness.export.model.LegacyDataContainer
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

class LegacyBackupReader(
    private val context: Context,
    private val input: Uri,
    private val replace: Boolean,
    private val listener: ImportStatusListener
) {
    private var dataContainer: LegacyDataContainer? = null
    private val database: AppDatabase = Instance.getInstance(context).db

    companion object {
        const val VERSION: Int = 3
    }

    @Throws(IOException::class, UnsupportedVersionException::class)
    fun restoreData() {
        listener.onStatusChanged(0, context.getString(R.string.loadingFile))
        loadDataFromFile()
        checkVersion()
        restoreDatabase()
        listener.onStatusChanged(100, context.getString(R.string.finished))
    }

    @Throws(IOException::class)
    private fun loadDataFromFile() {
        var stream: InputStream = context.contentResolver.openInputStream(input)!!
        val isZIP = stream.read() == 0x50 // Zip Magic number
        stream.close()
        stream = context.contentResolver.openInputStream(input)!!
        val decompressedInput: InputStream
        if (isZIP) {
            val zipIn = ZipInputStream(stream)
            zipIn.getNextEntry()
            decompressedInput = zipIn
        } else {
            decompressedInput = stream
        }
        val xmlMapper = XmlMapper()
        xmlMapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
        dataContainer = xmlMapper.readValue(
            decompressedInput,
            LegacyDataContainer::class.java
        )
        decompressedInput.close()
        stream.close()
    }

    @Throws(UnsupportedVersionException::class)
    private fun checkVersion() {
        if (dataContainer!!.version > VERSION) {
            throw UnsupportedVersionException("Version Code" + dataContainer!!.version + " is unsupported!")
        }
    }

    private fun restoreDatabase() {
        database.runInTransaction {
            if (replace) {
                resetDatabase()
            }
            listener.onStatusChanged(40, context.getString(R.string.workoutRecordingTypeGps))
            restoreGpsWorkouts()
            listener.onStatusChanged(50, context.getString(R.string.locationData))
            restoreGpsSamples()
            listener.onStatusChanged(80, context.getString(R.string.intervalSets))
            restoreIntervalSets()
            listener.onStatusChanged(85, context.getString(R.string.customWorkoutTypesTitle))
            restoreWorkoutTypes()
        }
    }

    private fun resetDatabase() {
        database.clearAllTables()
    }

    private fun restoreGpsWorkouts() {
        if (dataContainer!!.getWorkouts() != null) {
            for (workout in dataContainer!!.getWorkouts()) {
                // Only Import Unknown Workouts on merge
                if (replace || database.workoutDao().findById(workout.id) == null) {
                    database.workoutDao().insertWorkout(workout)
                }
            }
        }
    }

    private fun restoreGpsSamples() {
        if (dataContainer!!.getRouteSamples() != null) {
            for (sample in dataContainer!!.getRouteSamples()) {
                // Only import unknown samples with known workout on merge
                // Query not necessary on replace because data was cleared
                if (replace || (database.workoutDao().findById(sample.workoutId) != null &&
                            database.workoutDao().findSampleById(sample.id) == null)
                ) {
                    database.workoutDao().insertSample(sample)
                }
            }
        }
    }

    private fun restoreIntervalSets() {
        if (dataContainer!!.getIntervalSets() != null) {
            for (container in dataContainer!!.getIntervalSets()) {
                restoreIntervalSet(container)
            }
        }
    }

    private fun restoreIntervalSet(container: IntervalSetContainer) {
        val set = container.set
        // Only Import unknownInterval Sets
        if (database.intervalDao().getSet(set.id) == null) {
            database.intervalDao().insertIntervalSet(set)
        }
        if (container.intervals != null) {
            for (interval in container.intervals) {
                // Only Import Unknown Intervals
                if (database.intervalDao().findById(interval.id) == null) {
                    database.intervalDao().insertInterval(interval)
                }
            }
        }
    }

    private fun restoreWorkoutTypes() {
        if (dataContainer!!.workoutTypes != null) {
            for (type in dataContainer!!.workoutTypes) {
                // Only import unknown workout types
                if (database.workoutTypeDao().findById(type.id) == null) {
                    database.workoutTypeDao().insert(type)
                }
            }
        }
    }

    interface ImportStatusListener {
        fun onStatusChanged(progress: Int, action: String?)
    }

    internal class UnsupportedVersionException(message: String?) : Exception(message)
}
