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
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import de.tadris.fitness.Instance
import de.tadris.fitness.R
import de.tadris.fitness.data.AppDatabase
import de.tadris.fitness.data.BaseSample
import de.tadris.fitness.data.HeartRateSample
import de.tadris.fitness.data.RepetitionSample
import de.tadris.fitness.data.RouteSample
import de.tadris.fitness.data.Workout
import de.tadris.fitness.data.WorkoutData
import de.tadris.fitness.data.WorkoutEvent
import de.tadris.fitness.data.WorkoutPause
import de.tadris.fitness.data.WorkoutTypeManager
import de.tadris.fitness.export.model.IntervalSetContainer
import de.tadris.fitness.export.model.legacy.LegacyDataContainer
import java.io.IOException
import java.io.InputStream
import java.util.UUID
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

        private const val PAUSES_MIN_DIFF = 2000
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
            val restoredWorkouts = mutableListOf<Long>()
            listener.onStatusChanged(40, context.getString(R.string.workoutRecordingTypeGps))
            restoreGpsWorkouts().also { restoredWorkouts.addAll(it) }
            listener.onStatusChanged(50, context.getString(R.string.locationData))
            restoreGpsSamples()
            listener.onStatusChanged(80, context.getString(R.string.workoutRecordingTypeIndoor))
            restoreIndoorWorkouts().also { restoredWorkouts.addAll(it) }
            listener.onStatusChanged(85, context.getString(R.string.workoutRecordingTypeIndoor))
            restoreIndoorSamples()
            listener.onStatusChanged(90, context.getString(R.string.intervalSets))
            restoreIntervalSets()
            listener.onStatusChanged(95, context.getString(R.string.customWorkoutTypesTitle))
            restoreWorkoutTypes()
            listener.onStatusChanged(98, context.getString(R.string.runningMigrations))
            migratePauses(restoredWorkouts)
        }
    }

    private fun resetDatabase() {
        database.clearAllTables()
    }

    private fun restoreGpsWorkouts(): List<Long> {
        val importedWorkouts = mutableListOf<Long>()
        if (dataContainer!!.workouts != null) {
            for (workout in dataContainer!!.workouts) {
                // Only Import Unknown Workouts on merge
                if (replace || database.workoutDao().findById(workout.id) == null) {
                    database.workoutDao().insertWorkout(Workout(
                        id = workout.id,
                        uuid = UUID.randomUUID().toString(),
                        start = workout.start,
                        end = workout.end,
                        activeDuration = workout.duration,
                        comment = workout.comment,
                        distance = workout.length.toDouble(),
                        repetitions = 0,
                        energy = workout.calorie * 4184.0, // kcal -> joule
                        workoutTypeId = workout.workoutTypeId,
                        intervalSetUsedId = workout.intervalSetUsedId,
                    ))
                    importedWorkouts.add(workout.id)
                }
            }
        }
        return importedWorkouts
    }

    private fun restoreGpsSamples() {
        if (dataContainer!!.samples != null) {
            for (sample in dataContainer!!.samples) {
                // Only import unknown samples with known workout on merge
                // Query not necessary on replace because data was cleared
                if (replace || (database.workoutDao().findById(sample.workoutId) != null &&
                            database.workoutDao().findSampleById(sample.id) == null)
                ) {
                    database.workoutDao().insertSample(RouteSample(
                        workoutId = sample.workoutId,
                        id = sample.id,
                        timestamp = sample.absoluteTime,
                        relativeTime = sample.relativeTime,
                        latitude = sample.lat,
                        longitude = sample.lon,
                        elevation = sample.elevation,
                        elevationMSL = sample.elevationMSL,
                        speed = sample.speed,
                        pressure = sample.pressure,
                    ))

                    if(sample.intervalTriggered > 0){
                        database.workoutDao().insertWorkoutEvent(WorkoutEvent(
                            workoutId = sample.workoutId,
                            id = sample.id,
                            timestamp = sample.absoluteTime,
                            relativeTime = sample.relativeTime,
                            typeId = WorkoutEvent.TYPE_ID_INTERVAL
                        ))
                    }
                    if(sample.heartRate > 0){
                        database.workoutDao().insertSample(HeartRateSample(
                            workoutId = sample.workoutId,
                            id = sample.id,
                            timestamp = sample.absoluteTime,
                            relativeTime = sample.relativeTime,
                            heartRate = sample.heartRate
                        ))
                    }
                }
            }
        }
    }

    private fun restoreIndoorWorkouts(): List<Long> {
        val importedWorkouts = mutableListOf<Long>()
        if (dataContainer!!.indoorWorkouts != null) {
            for (workout in dataContainer!!.indoorWorkouts) {
                // Only Import Unknown Workouts on merge
                if (replace || database.workoutDao().findById(workout.id) == null) {
                    database.workoutDao().insertWorkout(Workout(
                        id = workout.id,
                        uuid = UUID.randomUUID().toString(),
                        start = workout.start,
                        end = workout.end,
                        activeDuration = workout.duration,
                        comment = workout.comment,
                        distance = 0.0,
                        repetitions = workout.repetitions,
                        energy = workout.calorie * 4184.0, // kcal -> joule
                        workoutTypeId = workout.workoutTypeId,
                        intervalSetUsedId = workout.intervalSetUsedId,
                    ))
                    importedWorkouts.add(workout.id)
                }
            }
        }
        return importedWorkouts
    }

    private fun restoreIndoorSamples() {
        if (dataContainer!!.indoorSamples != null) {
            for (sample in dataContainer!!.indoorSamples) {
                // Only import unknown samples with known workout on merge
                // Query not necessary on replace because data was cleared
                if (replace || (database.workoutDao().findById(sample.workoutId) != null &&
                            database.workoutDao().findSampleById(sample.id) == null)
                ) {
                    val workout = database.workoutDao().findById(sample.workoutId)
                        ?: throw IllegalStateException("Cannot import indoor sample ${sample.id} as workout ${sample.workoutId} was not found")

                    val repetitionTypeId = when(workout.workoutTypeId){
                        WorkoutTypeManager.WORKOUT_TYPE_ID_TREADMILL -> RepetitionSample.REPETITION_TYPE_STEP
                        WorkoutTypeManager.WORKOUT_TYPE_ID_ROPE_SKIPPING,
                        WorkoutTypeManager.WORKOUT_TYPE_ID_TRAMPOLINE_JUMPING -> RepetitionSample.REPETITION_TYPE_JUMP
                        WorkoutTypeManager.WORKOUT_TYPE_ID_PUSH_UPS -> RepetitionSample.REPETITION_TYPE_PUSH_UP
                        WorkoutTypeManager.WORKOUT_TYPE_ID_PULL_UPS -> RepetitionSample.REPETITION_TYPE_PULL_UP
                        else -> RepetitionSample.REPETITION_TYPE_UNKNOWN
                    }

                    database.workoutDao().insertSample(RepetitionSample(
                        workoutId = sample.workoutId,
                        id = sample.id,
                        timestamp = sample.absoluteTime,
                        relativeTime = sample.relativeTime,
                        repetitionTypeId = repetitionTypeId,
                        timestampEnd = sample.absoluteEndTime,
                        repetitions = sample.repetitions,
                        intensity = sample.intensity,
                        frequency = sample.frequency,
                    ))

                    if(sample.intervalTriggered > 0){
                        database.workoutDao().insertWorkoutEvent(WorkoutEvent(
                            workoutId = sample.workoutId,
                            id = sample.id,
                            timestamp = sample.absoluteTime,
                            relativeTime = sample.relativeTime,
                            typeId = WorkoutEvent.TYPE_ID_INTERVAL
                        ))
                    }
                    if(sample.heartRate > 0){
                        database.workoutDao().insertSample(HeartRateSample(
                            workoutId = sample.workoutId,
                            id = sample.id,
                            timestamp = sample.absoluteTime,
                            relativeTime = sample.relativeTime,
                            heartRate = sample.heartRate
                        ))
                    }
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

    private fun migratePauses(workoutIds: List<Long>){
        for(id in workoutIds){
            val workout = database.workoutDao().getWorkoutById(id) ?: continue
            val data = WorkoutData.fromWorkout(database.workoutDao(), workout)
            if(data.hasRouteData){
                checkSamples(workout, data.samples)
            }else if(data.hasRepetitions){
                checkSamples(workout, data.repetitionSamples)
            }
        }
    }

    private fun checkSamples(workout: Workout, samples: List<BaseSample>){
        var currentDiff = samples.first().let { it.timestamp - it.relativeTime }
        for(sample in samples){
            val sampleDiff = sample.timestamp - sample.relativeTime
            if(sampleDiff - currentDiff > PAUSES_MIN_DIFF){
                val endDate = sample.timestamp
                val startDate = endDate - (sampleDiff - currentDiff)
                database.workoutDao().insertPause(WorkoutPause(
                    workout.id, startDate, startDate,
                    endDate, false
                ))
                currentDiff = sampleDiff
            }
        }
    }

    interface ImportStatusListener {
        fun onStatusChanged(progress: Int, action: String?)
    }

    internal class UnsupportedVersionException(message: String?) : Exception(message)
}
