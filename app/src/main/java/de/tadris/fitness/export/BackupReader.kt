package de.tadris.fitness.export

import android.content.Context
import android.net.Uri
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.json.JsonMapper
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.AppDatabase
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.export.BackupWriter.ExportStatusListener
import de.tadris.fitness.export.model.DataContainer
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class BackupReader(
    private val context: Context,
    private val input: Uri,
    private val replace: Boolean,
    private val listener: ExportStatusListener
) {

    private val database: AppDatabase = Instance.getInstance(context).db
    private val mapper = JsonMapper.builder()
        .configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
        .build()

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

        val workout = workoutData.workout
        val workoutId = workout.id

        // Only import unknown workouts on merge
        if (replace || database.workoutDao().findById(workoutId) == null) {
            database.workoutDao().insertWorkout(workout)

            // Insert all related data
            for (sample in workoutData.samples) {
                database.workoutDao().insertSample(sample)
            }

            for (pause in workoutData.pauses) {
                database.workoutDao().insertPause(pause)
            }

            for (event in workoutData.events) {
                database.workoutDao().insertWorkoutEvent(event)
            }

            for (repetitionSample in workoutData.repetitionSamples) {
                database.workoutDao().insertSample(repetitionSample)
            }

            for (heartRateSample in workoutData.heartRateSamples) {
                database.workoutDao().insertSample(heartRateSample)
            }
        }
    }

    internal class UnsupportedVersionException(message: String?) : Exception(message)

}