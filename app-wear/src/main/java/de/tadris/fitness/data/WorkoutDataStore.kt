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
		flushHeartRate(context, workoutData, onError)
	}

	fun flushLocations(
		context: Context,
		workoutData: WorkoutData,
		onError: () -> Unit
	) {
		if(workoutData.locationSamples.isEmpty()) return
		val locationSamples = ArrayList(workoutData.locationSamples)
		workoutData.locationSamples.clear()
		val name = idToLocationFilename(workoutData.id)
		var str = locationSamples.toList().joinToString(prefix = "[", postfix = "]", separator = ",") {
			it.toJson().toString()
		}
		try{
			val file = context.getFileStreamPath(name)
			if(file.exists() && file.length() > 2) {
				var existingSamples = context.openFileInput(name).bufferedReader().use { it.readText() }
				existingSamples = existingSamples.trim('[', ']')
				if(existingSamples.length > 2) str = "[$existingSamples,${str.substring(1)}"
			}
			write(context, name, str, onError)
		}catch(e: Exception){
			logE("WorkoutDataStore.flushLocations: ${e.message}")
			onError()
		}
	}

	fun flushHeartRate(
		context: Context,
		workoutData: WorkoutData,
		onError: () -> Unit
	) {
		if(workoutData.heartRateSamples.isEmpty()) return
		val heartRateSamples = ArrayList(workoutData.heartRateSamples)
		workoutData.heartRateSamples.clear()
		val name = idToHeartRateFilename(workoutData.id)
		var str = heartRateSamples.toList().joinToString(prefix = "[", postfix = "]", separator = ",") {
			it.toJson().toString()
		}
		try{
			val file = context.getFileStreamPath(name)
			if(file.exists() && file.length() > 2) {
				var existingSamples = context.openFileInput(name).bufferedReader().use { it.readText() }
				existingSamples = existingSamples.trim('[', ']')
				if(existingSamples.length > 2) str = "[$existingSamples,${str.substring(1)}"
			}
			write(context, name, str, onError)
		}catch(e: Exception){
			logE("WorkoutDataStore.flushHeartRate: ${e.message}")
			onError()
		}
	}

	fun idToWorkoutFilename(id: Long): String = "workout_$id.json"
	fun idToLocationFilename(id: Long): String = "location_$id.json"
	fun idToHeartRateFilename(id: Long): String = "heart_$id.json"
}
