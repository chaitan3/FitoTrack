/*
 * Copyright (c) 2026 Bart Vullings <dev@windkracht8.com>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU General Public License as published by
 * 	the Free Software Foundation, either version 3 of the License, or
 * 	(at your option) any later version.
 *
 * 	FitoTrack is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 *
 * 	You should have received a copy of the GNU General Public License
 * 	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.tadris.fitness.data

import android.content.Context
import de.tadris.fitness.util.logE
import de.tadris.fitness.util.logW
import de.tadris.fitness.util.runInBackground
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.io.RandomAccessFile
import java.util.Arrays

object WorkoutDataStore {
	val workouts = mutableListOf<WorkoutData>()

	fun deleteWorkout(context: Context, id: Long, onError: () -> Unit) { //Thread: BG
		workouts.removeIf{ it.id == id }
		if(!context.deleteFile(idToLocationFilename(id))) {
			logW("WorkoutDataStore.deleteWorkout failed, filename: " + idToLocationFilename(id))
		}
		if(!context.deleteFile(idToHeartRateFilename(id))) {
			logW("WorkoutDataStore.deleteWorkout failed, filename: " + idToHeartRateFilename(id))
		}
		if(!context.deleteFile(idToWorkoutFilename(id))) {
			logE("WorkoutDataStore.deleteWorkout failed, filename: " + idToWorkoutFilename(id))
			onError()
		}
	}

	fun readWorkouts(context: Context) { //Thread: BG
		val files = context.filesDir.listFiles() ?: return
		Arrays.sort(files)
		workouts.clear()
		for(i in files.indices.reversed()) {
			val filename = files[i].getName()
			if(filename.startsWith("workout_") && filename.endsWith(".json")) {
				readWorkout(context, filename)?.let{ workouts.add(it) }
			}
		}
		workouts.forEach { workout ->
			if(workout.rootWorkoutId != null) {
				workout.rootWorkout = workouts.find { it.id == workout.rootWorkoutId }
				workout.rootWorkout?.legs?.add(workout)
			}
		}
	}

	fun getFullWorkout(context: Context, id: Long): WorkoutData? { //Thread: BG
		try {
			val workoutData = readWorkout(context, idToWorkoutFilename(id)) ?: return null
			if(workoutData.isWorkoutTypeGps) {
				val locations = readLocations(context, id)
				if(locations != null) workoutData.locationsFromJson(locations)
			}
			val heartRates = readHeartRates(context, id)
			if(heartRates != null) workoutData.heartRatesFromJson(heartRates)
			return workoutData
		} catch(e: Exception) {
			logE("WorkoutDataStore.getFullWorkout: ${e.message}")
		}
		return null
	}

	fun readWorkout(context: Context, filename: String): WorkoutData? { //Thread: BG
		try {
			val text = context.openFileInput(filename).bufferedReader().use { it.readText() }
			return WorkoutData(JSONObject(text))
		} catch(e: Exception) {
			logE("WorkoutDataStore.readWorkout: ${e.message}")
		}
		return null
	}

	fun readLocations(//Thread: BG
		context: Context,
		workoutData: WorkoutData,
		onDone: () -> Unit = {},
		filter: Boolean = false
	) {
		try {
			val locations = readLocations(context, workoutData.id) ?: return
			workoutData.locationsFromJson(locations, filter)
			workoutData.legs.forEach { readLocations(context, workoutData = it, onDone, filter) }
			onDone()
		} catch(e: Exception) { logW("WorkoutDataStore.readLocations(workoutData): ${e.message}") }
	}

	fun readLocations(context: Context, workoutDataId: Long): JSONArray? { //Thread: BG
		try {
			val filename = idToLocationFilename(workoutDataId)
			val text = context.openFileInput(filename).bufferedReader().use { it.readText() }
			return JSONArray(text)
		} catch(e: Exception) {
			logW("WorkoutDataStore.readLocations(workoutDataId): ${e.message}")
		}
		return null
	}

	fun readHeartRates(//Thread: BG
		context: Context,
		workoutData: WorkoutData,
		onDone: () -> Unit = {},
		filter: Boolean = false
	) {
		try {
			val heartRates = readHeartRates(context, workoutData.id) ?: return
			workoutData.heartRatesFromJson(heartRates, filter)
			workoutData.legs.forEach { readHeartRates(context, workoutData = it, onDone, filter) }
			onDone()
		} catch(e: Exception) { logW("WorkoutDataStore.readHeartRates(workoutData): ${e.message}") }
	}

	fun readHeartRates(context: Context, workoutDataId: Long): JSONArray? { //Thread: BG
		try {
			val filename = idToHeartRateFilename(workoutDataId)
			val text = context.openFileInput(filename).bufferedReader().use { it.readText() }
			return JSONArray(text)
		} catch(e: Exception) {
			logW("WorkoutDataStore.readHeartRates(workoutDataId): ${e.message}")
		}
		return null
	}

	fun storeWorkout(context: Context, workoutData: WorkoutData, onError: () -> Unit) { //Thread: UI
		workoutData.setDescName()
		workouts.add(0, workoutData)
		runInBackground { writeWorkout(context, workoutData, onError) }
	}

	fun write(context: Context, name: String, str: String, onError: () -> Unit = {}) { //Thread: BG
		if(str.length < 10) return
		try {
			context.openFileOutput(name, Context.MODE_PRIVATE).use { out ->
				OutputStreamWriter(out).use { osr -> osr.write(str) }
			}
		} catch (e: Exception) {
			logE("WorkoutDataStore.write: ${e.message}")
			onError()
		}
	}

	fun writeWorkout(context: Context, workoutData: WorkoutData, onError: () -> Unit) { //Thread: BG
		write(
			context,
			name = idToWorkoutFilename(workoutData.id),
			str = workoutData.toJsonString(context),
			onError
		)
		flushLocations(context, workoutData, onError)
		flushHeartRates(context, workoutData, onError)
	}

	fun flushLocations(context: Context, workoutData: WorkoutData, onError: () -> Unit) {
		if(workoutData.locationSamples.isEmpty()) return
		val locationSamples = synchronized(workoutData.locationSamples) {
			val samples = workoutData.locationSamples.toList()
			workoutData.locationSamples.clear()
			samples
		}
		val newSamplesStr = locationSamples.toList().joinToString(separator = ",") {
			it.toJson().toString()
		}
		val name = idToLocationFilename(workoutData.id)

		try {
			val file = context.getFileStreamPath(name)
			if(file.exists() && file.length() > 2) {
				RandomAccessFile(file, "rw").use { raf ->
					raf.seek(raf.length() - 1) // Overwrite ]
					raf.writeBytes(",$newSamplesStr]")
				}
			} else write(context, name, "[$newSamplesStr]", onError)
		} catch (e: Exception) {
			logE("WorkoutDataStore.flushLocations: ${e.message}")
			onError()
		}
	}

	fun flushHeartRates(context: Context, workoutData: WorkoutData, onError: () -> Unit) {
		if(workoutData.heartRateSamples.isEmpty()) return
		val heartRateSamples = synchronized(workoutData.heartRateSamples) {
			val samples = workoutData.heartRateSamples.toList()
			workoutData.heartRateSamples.clear()
			samples
		}
		val newSamplesStr = heartRateSamples.toList().joinToString(separator = ",") {
			it.toJson().toString()
		}
		val name = idToHeartRateFilename(workoutData.id)

		try {
			val file = context.getFileStreamPath(name)
			if(file.exists() && file.length() > 2) {
				RandomAccessFile(file, "rw").use { raf ->
					raf.seek(raf.length() - 1) // Overwrite ]
					raf.writeBytes(",$newSamplesStr]")
				}
			} else write(context, name, "[$newSamplesStr]", onError)
		} catch (e: Exception) {
			logE("WorkoutDataStore.flushHeartRates: ${e.message}")
			onError()
		}
	}

	fun idToWorkoutFilename(id: Long): String = "workout_$id.json"
	fun idToLocationFilename(id: Long): String = "location_$id.json"
	fun idToHeartRateFilename(id: Long): String = "heart_$id.json"
}
