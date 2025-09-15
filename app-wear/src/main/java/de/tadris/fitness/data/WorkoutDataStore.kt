/*
 * Copyright (c) 2025 Bart Vullings <dev@windkracht8.com>
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
		val files = context.filesDir.listFiles()
		if(files == null) return
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
			val workoutData = readWorkout(context, idToWorkoutFilename(id))
			if(workoutData == null) return null
			if(workoutData.isWorkoutTypeGps) {
				val locations = readLocations(context, id)
				if(locations != null) workoutData.locationsFromJson(locations)
			}
			val heartRates = readHeartRates(context, id)
			if(heartRates != null) workoutData.heartRatesFromJson(heartRates)
			return workoutData
		} catch(e: Exception) {
			logE("WorkoutDataStore.getFullWorkout: " + e.message)
		}
		return null
	}

	fun readWorkout(context: Context, filename: String): WorkoutData? { //Thread: BG
		try {
			val text = context.openFileInput(filename).bufferedReader().use { it.readText() }
			return WorkoutData(JSONObject(text))
		} catch(e: Exception) {
			logE("WorkoutDataStore.readWorkout: " + e.message)
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
			val locations = readLocations(context, workoutData.id)
			if(locations == null) return
			workoutData.locationsFromJson(locations, filter)
			workoutData.legs.forEach { readLocations(context, workoutData = it, onDone, filter) }
			onDone()
		} catch(e: Exception) { logW("WorkoutDataStore.readLocations: " + e.message) }
	}

	fun readLocations(context: Context, workoutDataId: Long): JSONArray? { //Thread: BG
		try {
			val filename = idToLocationFilename(workoutDataId)
			val text = context.openFileInput(filename).bufferedReader().use { it.readText() }
			return JSONArray(text)
		} catch(e: Exception) {
			logW("WorkoutDataStore.readLocations: " + e.message)
		}
		return null
	}

	fun readHeartRates(context: Context, workoutData: WorkoutData, onDone: () -> Unit) { //Thread: BG
		try {
			val heartRates = readHeartRates(context, workoutData.id)
			if(heartRates == null) return
			workoutData.heartRatesFromJson(heartRates)
			onDone()
		} catch(e: Exception) { logW("WorkoutDataStore.readHeartRates: " + e.message) }
	}

	fun readHeartRates(context: Context, workoutDataId: Long): JSONArray? { //Thread: BG
		try {
			val filename = idToHeartRateFilename(workoutDataId)
			val text = context.openFileInput(filename).bufferedReader().use { it.readText() }
			return JSONArray(text)
		} catch(e: Exception) {
			logW("WorkoutDataStore.readHeartRates: " + e.message)
		}
		return null
	}

	fun storeWorkout(context: Context, workoutData: WorkoutData, onError: () -> Unit) { //Thread: UI
		workoutData.setDescName()
		workouts.add(0, workoutData)
		runInBackground {
			writeWorkout(context, workoutData, onError)
			workoutData.locationSamples.clear()
			workoutData.heartRateSamples.clear()
		}
	}

	fun write(context: Context, name: String, str: String, onError: () -> Unit = {}) { //Thread: BG
		if(str.length < 10) return
		try {
			context.openFileOutput(name, Context.MODE_PRIVATE).use { out ->
				OutputStreamWriter(out).use { osr -> osr.write(str) }
			}
		} catch (e: Exception) {
			logE("WorkoutDataStore.write: " + e.message)
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
		write(
			context,
			name = idToLocationFilename(workoutData.id),
			str = workoutData.locationsToJsonString()
		)
		write(
			context,
			name = idToHeartRateFilename(workoutData.id),
			str = workoutData.heartRatesToJsonString()
		)
	}

	private fun idToWorkoutFilename(id: Long): String = "workout_$id.json"
	private fun idToLocationFilename(id: Long): String = "location_$id.json"
	private fun idToHeartRateFilename(id: Long): String = "heart_$id.json"
}