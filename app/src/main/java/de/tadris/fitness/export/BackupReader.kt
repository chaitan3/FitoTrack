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

package de.tadris.fitness.export

import android.content.Context
import android.net.Uri
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.AppDatabase
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.export.BackupWriter.ExportStatusListener
import de.tadris.fitness.export.model.DataContainer
import tools.jackson.core.StreamReadFeature
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Reads a backup from [input] and writes the result into the local DB.
 */
class BackupReader(
    private val context: Context,
    private val input: Uri,
    private val replace: Boolean,
    private val listener: ExportStatusListener
) {

    private val database: AppDatabase = Instance.getInstance(context).db
    private val mapper = jsonMapper {
        addModule(kotlinModule())
        configure(StreamReadFeature.IGNORE_UNDEFINED, true)
        configure(StreamReadFeature.AUTO_CLOSE_SOURCE, false)
    }

    @Throws(IOException::class, UnsupportedVersionException::class)
    fun read() {
        listener.onStatusChanged(0, context.getString(R.string.loadingFile))

        if (checkIfLegacyBackup()) {
            delegateToLegacyBackup()
            return
        }

        val version = readVersion()
        if (version > BackupWriter.VERSION) {
            throw UnsupportedVersionException("Version $version is unsupported!")
        }

        restoreDatabase()
        listener.onStatusChanged(100, context.getString(R.string.finished))
    }

    private fun delegateToLegacyBackup(){
        listener.onStatusChanged(0, context.getString(R.string.loadingFile))
        val legacyReader = LegacyBackupReader(
            context,
            input,
            replace,
            object : LegacyBackupReader.ImportStatusListener {
                override fun onStatusChanged(progress: Int, action: String?) {
                    listener.onStatusChanged(progress, action)
                }
            }
        )
        legacyReader.restoreData()
    }

    @Throws(IOException::class)
    private fun checkIfLegacyBackup(): Boolean {
        useZipInputStream { zipStream ->
            val firstEntry = zipStream.nextEntry
            if (firstEntry != null) {
                // Old backups have "data.xml" as the first (and only) entry
                return firstEntry.name == "data.xml"
            }
        }
        return false
    }

    @Throws(IOException::class)
    private fun readVersion(): Int {
        forEachZipEntry { entry, stream ->
            if (entry.name == BackupWriter.FILE_VERSION) {
                val versionString = stream.readBytes().toString(Charsets.UTF_8)
                stream.closeEntry()
                return versionString.toInt()
            }
        }
        throw IOException("Version file not found in backup")
    }

    @Throws(IOException::class)
    private fun openInputStream(): InputStream {
        return context.contentResolver.openInputStream(input)
            ?: throw IOException("Failed to open input stream for Uri: $input")
    }

    @Throws(IOException::class)
    private fun restoreDatabase() {
        database.runInTransaction {
            if (replace) {
                listener.onStatusChanged(10, context.getString(R.string.restorePreparing))
                database.clearAllTables() // TODO dont clear local settings like auto export
            }

            var workoutCount = 0
            var processedWorkouts = 0

            forEachZipEntry { entry, _ ->
                if (isWorkoutFile(entry.name)) {
                    workoutCount++
                }
            }

            // Second pass: restore data
            forEachZipEntry { entry, stream ->
                when {
                    entry.name == BackupWriter.FILE_DATA -> {
                        listener.onStatusChanged(20, context.getString(R.string.customWorkoutTypesTitle))
                        restoreExtraData(stream)
                    }
                    isWorkoutFile(entry.name) -> {
                        val progress = 20 + (80 * processedWorkouts / workoutCount.coerceAtLeast(1))
                        listener.onStatusChanged(progress, context.getString(R.string.workouts))
                        restoreWorkout(stream)
                        processedWorkouts++
                    }
                }
            }
        }
    }

    private inline fun forEachZipEntry(action: (ZipEntry, ZipInputStream) -> Unit){
        useZipInputStream { zipStream ->
            var entry = zipStream.nextEntry

            // First pass: count workout files
            while (entry != null) {
                action(entry, zipStream)
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        }
    }

    private inline fun useZipInputStream(action: (ZipInputStream) -> Unit){
        openInputStream().use { fileStream ->
            ZipInputStream(fileStream).use { zipStream ->
                action(zipStream)
            }
        }
    }

    private fun isWorkoutFile(name: String) = name.startsWith("workout_") && name.endsWith(".json")

    @Throws(IOException::class)
    private fun restoreExtraData(zipStream: ZipInputStream) {
        val dataContainer = mapper.readValue(zipStream, DataContainer::class.java)

        // Restore interval sets
        for (container in dataContainer.intervalSets) {
            val set = container.set
            if (replace || database.intervalDao().getSet(set.id) == null) {
                database.intervalDao().insertIntervalSet(set)
            }

            for (interval in container.intervals) {
                if (replace || database.intervalDao().findById(interval.id) == null) {
                    database.intervalDao().insertInterval(interval)
                }
            }
        }

        // Restore workout types
        for (type in dataContainer.workoutTypes) {
            if (replace || database.workoutTypeDao().findById(type.id) == null) {
                database.workoutTypeDao().insert(type)
            }
        }
    }

    @Throws(IOException::class)
    private fun restoreWorkout(zipStream: ZipInputStream) {
        val workoutData = mapper.readValue(zipStream, WorkoutData::class.java)

        // Only import unknown workouts on merge
        if (replace || database.workoutDao().findById(workoutData.workout.id) == null) {
            database.workoutDao().insertWorkoutData(workoutData)
        }
    }

    internal class UnsupportedVersionException(message: String?) : Exception(message)

}